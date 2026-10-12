package com.adskip.assist.automation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.adskip.assist.data.SettingsRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/** One transient screenshot for the explicit, user-triggered Teach workflow. Never persists full screens. */
object TeachCaptureStore {
    @Volatile var requestedPackage: String? = null
    @Volatile var captured: Bitmap? = null
        private set
    @Volatile var capturedPackage: String? = null
        private set
    fun receive(game: String, screenshot: Bitmap) {
        if (requestedPackage != game) return
        val old = captured
        captured = screenshot.copy(Bitmap.Config.ARGB_8888, false)
        capturedPackage = game
        requestedPackage = null
        old?.recycle()
    }
    fun dismiss() {
        captured?.recycle()
        captured = null
        capturedPackage = null
        requestedPackage = null
    }
}

/** Private, per-game close-button thumbnails. No full screenshots leave memory. */
class LearnedTargetStore(private val context: Context) {
    private fun file(game: String): File {
        val safeName = game.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)
        return File(context.filesDir, "learned_targets/$safeName.png")
    }
    fun exists(game: String) = file(game).isFile
    fun delete(game: String) { file(game).delete() }
    fun save(game: String, full: Bitmap, x: Int, y: Int): Boolean {
        val margin = 48
        if (!inCorner(x, y, full.width, full.height)) return false
        if (x < margin || y < margin || x + margin >= full.width || y + margin >= full.height) return false
        val crop = Bitmap.createBitmap(full, x-margin, y-margin, margin*2, margin*2)
        val destination = file(game)
        destination.parentFile?.mkdirs()
        val saved = destination.outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
        crop.recycle()
        return saved
    }
    fun load(game: String): Bitmap? {
        val file = file(game)
        if (!file.exists()) return null
        return android.graphics.BitmapFactory.decodeFile(file.absolutePath)
    }
    companion object {
        /** Intentionally restrict learned clicks to edge zones, never central CTAs. */
        fun inCorner(x: Int, y: Int, width: Int, height: Int): Boolean =
            width > 0 && height > 0 &&
                (x < width * .30 || x > width * .70) &&
                (y < height * .38 || y > height * .82)
    }
}

/** Scores local grayscale patches using correlation and mean absolute error; no ML cloud service. */
object ImageTemplateMatcher {
    data class Hit(val x: Int, val y: Int, val confidence: Double)
    fun find(screen: Bitmap, template: Bitmap): Hit? {
        if (template.width < 20 || template.height < 20 || screen.width < 100) return null
        val factor = min(1.0, 360.0 / screen.width)
        val w = (screen.width * factor).toInt().coerceAtLeast(1)
        val h = (screen.height * factor).toInt().coerceAtLeast(1)
        val tw = (template.width * factor).toInt().coerceAtLeast(9)
        val th = (template.height * factor).toInt().coerceAtLeast(9)
        if (tw >= w || th >= h) return null
        val small = Bitmap.createScaledBitmap(screen, w, h, true)
        val target = Bitmap.createScaledBitmap(template, tw, th, true)
        val pixels = IntArray(w*h); small.getPixels(pixels, 0, w, 0, 0, w, h)
        val reference = IntArray(tw*th); target.getPixels(reference, 0, tw, 0, 0, tw, th)
        val stride = 3
        val xs = (0 until tw step stride).toList()
        val ys = (0 until th step stride).toList()
        val samples = xs.flatMap { x -> ys.map { y -> luminance(reference[y*tw+x]) } }
        val mean = samples.average()
        val variance = samples.sumOf { (it-mean)*(it-mean) }
        // Blank/flat thumbnails are not useful and risk false positives.
        if (variance < samples.size * 90.0) { if (small !== screen) small.recycle(); if (target !== template) target.recycle(); return null }
        var best: Hit? = null
        val step = 3
        for (yy in 0..(h-th) step step) for (xx in 0..(w-tw) step step) {
            val cx = ((xx+tw/2)/factor).toInt()
            val cy = ((yy+th/2)/factor).toInt()
            if (!LearnedTargetStore.inCorner(cx, cy, screen.width, screen.height)) continue
            var absError = 0.0
            var dot = 0.0; var sum = 0.0; var varianceAt = 0.0
            var k = 0
            for (x in xs) for (y in ys) {
                val value = luminance(pixels[(yy+y)*w+xx+x])
                val centered = samples[k++]-mean
                absError += abs(value-(centered+mean))
                sum += value
                dot += centered*value
            }
            val patchMean = sum/samples.size
            for (x in xs) for (y in ys) {
                val value = luminance(pixels[(yy+y)*w+xx+x]); varianceAt += (value-patchMean)*(value-patchMean)
            }
            val correlation = if (varianceAt > 0) dot/sqrt(variance*varianceAt) else 0.0
            val averageError = absError/samples.size
            val confidence = min(correlation, 1.0 - averageError/180.0)
            if (correlation >= .965 && averageError <= 14 && (best == null || confidence > best.confidence))
                best = Hit(cx, cy, confidence)
        }
        if (small !== screen) small.recycle()
        if (target !== template) target.recycle()
        return best
    }
    private fun luminance(c: Int): Double = ((c shr 16 and 255)*.299 + (c shr 8 and 255)*.587 + (c and 255)*.114)
}

/** Screenshot analysis is one-shot and asynchronous. OCR is entirely on-device. */
class VisualRecognition(private val context: Context) {
    data class Target(val action: AdStateMachine.Action, val bounds: Rect, val source: String)
    data class Observation(val adEvidence: Boolean, val completionHint: Boolean, val targets: List<Target>)
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val templates = LearnedTargetStore(context)
    fun analyze(game: String, image: Bitmap, config: SettingsRepository.Settings, done: (Observation) -> Unit) {
        if (!config.visualDetection && !config.learnedTargetsEnabled) { done(Observation(false, false, emptyList())); return }
        val learned = if (config.learnedTargetsEnabled) templates.load(game)?.let { template ->
            try { ImageTemplateMatcher.find(image, template) } finally { template.recycle() }
        } else null
        val preTargets = learned?.let {
            listOf(Target(AdStateMachine.Action.CLOSE, Rect(it.x-36, it.y-36, it.x+36, it.y+36), "learned"))
        }.orEmpty()
        if (!config.visualDetection) { done(Observation(preTargets.isNotEmpty(), false, preTargets)); return }
        recognizer.process(InputImage.fromBitmap(image, 0))
            .addOnSuccessListener { page ->
                val lines = page.textBlocks.flatMap { it.lines }
                val labels = lines.map { it.text }
                val evidence = AdUiRules.analyze(labels, emptyList(), config.extendedDetection)
                val targets = lines.mapNotNull { line ->
                    val rect = line.boundingBox ?: return@mapNotNull null
                    // Never click OCR results unless they occur in a credible ad context.
                    val action = AdUiRules.classify(line.text, "", AdUiRules.Bounds(rect.left, rect.top, rect.width(), rect.height()), image.width, image.height)
                        ?: return@mapNotNull null
                    if (!LearnedTargetStore.inCorner(rect.centerX(), rect.centerY(), image.width, image.height)) return@mapNotNull null
                    Target(action, rect, "ocr")
                }
                done(Observation(evidence.isAd || preTargets.isNotEmpty(), evidence.completionHint, if (evidence.isAd) targets + preTargets else preTargets))
            }.addOnFailureListener { done(Observation(preTargets.isNotEmpty(), false, preTargets)) }
    }
    fun close() { recognizer.close() }
}
