package com.adskip.assist.automation

import java.util.Locale

/** Pure Kotlin matching rules; intentionally favors false negatives over unsafe taps. */
object AdUiRules {
    data class Context(val isAd: Boolean, val completionHint: Boolean)
    data class Bounds(val x: Int, val y: Int, val width: Int, val height: Int) {
        val centerX: Double get() = x + width / 2.0
        val centerY: Double get() = y + height / 2.0
    }
    private val risky = listOf("install", "download", "buy", "purchase", "learn more", "shop",
        "open app", "play now", "visit site", "subscribe")
    private val strong = listOf("skip ad", "skip video", "close ad", "adchoices", "ad choices",
        "advertisement", "video ad", "ad will end", "ad ends in", "ad ends",
        "ads by", "sponsored ad")
    private val completed = listOf("reward granted", "reward earned", "reward received", "ad complete",
        "video completed", "watch complete", "thanks for watching", "you earned the reward", "reward unlocked")
    private val adSdkIds = listOf("adchoices", "ad_choices", "rewarded_ad", "ad_container",
        "applovin", "mbridge", "unityads", "ironsource", "vungle")

    fun analyze(labels: List<String>, resourceIds: List<String>, extended: Boolean = false): Context {
        val texts = labels.map { it.lowercase(Locale.ROOT) }
        val ids = resourceIds.map { it.lowercase(Locale.ROOT) }
        val strongEvidence = texts.any { text -> strong.any(text::contains) }
        val weak = texts.any { it == "ad" || it == "ads" || it == "sponsored" || it.startsWith("ad 1 of ") }
        val sdkId = ids.any { id -> adSdkIds.any(id::contains) }
        val skipControl = texts.any { it == "skip" || it.startsWith("skip in ") }
        return Context(strongEvidence || (weak && sdkId) || (extended && weak && skipControl),
            texts.any { text -> completed.any(text::contains) })
    }

    fun classify(labelRaw: String, idRaw: String, box: Bounds,
                 screenWidth: Int, screenHeight: Int): AdStateMachine.Action? {
        val label = labelRaw.trim().lowercase(Locale.ROOT)
        val id = idRaw.lowercase(Locale.ROOT)
        if (risky.any(label::contains) || box.width < 1 || box.height < 1 ||
            box.width > screenWidth * .45 || box.height > screenHeight * .35) return null
        return when {
            label == "return to game" || label == "back to game" || label == "continue to game" ->
                AdStateMachine.Action.RETURN_TO_GAME
            label == "skip ad" || label == "skip video" || label == "skip" ||
                id.endsWith("skip_button") || id.endsWith("skip_btn") -> AdStateMachine.Action.SKIP
            label == "close ad" || label == "close video" || label == "close" ||
                label == "dismiss" || label == "done" || label == "exit ad" ||
                id.endsWith("ad_close") || id.endsWith("close_button") ||
                id.endsWith("close_btn") -> AdStateMachine.Action.CLOSE
            label in setOf("x", "×", "✕") &&
                box.centerX >= screenWidth * .65 && box.centerY <= screenHeight * .32 ->
                AdStateMachine.Action.CLOSE
            else -> null
        }
    }
}
