package com.adskip.assist.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import com.adskip.assist.data.ActivityLogRepository
import com.adskip.assist.data.SettingsRepository
import com.adskip.assist.data.StatisticsRepository

/** Local, opt-in automation. Never manipulates ad verification or traffic. */
class AccessibilityAutomationService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val handler = Handler(Looper.getMainLooper())
    private val settings by lazy { SettingsRepository.get(this) }
    private val history by lazy { ActivityLogRepository.get(this) }
    private val stats by lazy { StatisticsRepository.get(this) }
    private val detector = AdDetectionEngine()
    private val vision by lazy { VisualRecognition(this) }
    private val fsm = AdStateMachine()
    private var scheduled: Runnable? = null
    private var nextScanAtMs = Long.MAX_VALUE
    private var screenshotInFlight = false
    private var lastScreenshotAt = 0L
    private var activeGame: String? = null
    private var lastSeenGameAt = 0L
    private var lastVisualKey: String? = null
    private var lastVisualSeenAt = 0L
    private var learnOverlay: TextView? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        settings.addListener(this)
        configureFilter()
    }
    override fun onSharedPreferenceChanged(p: SharedPreferences?, key: String?) {
        if (key in setOf("games", "store_recovery")) configureFilter()
        if (key in setOf("enabled", "consent") && (!settings.refresh().enabled || !settings.refresh().consent)) stopScanning()
    }
    private fun configureFilter() {
        val cfg = settings.refresh()
        val info = serviceInfo ?: return
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 180
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        val packages = cfg.allowlist + (if (cfg.storeRecovery) setOf("com.android.vending") else emptySet())
        info.packageNames = if (packages.isEmpty()) arrayOf(packageName) else packages.toTypedArray()
        serviceInfo = info
        stopScanning()
    }
    private fun stopScanning() {
        scheduled?.let(handler::removeCallbacks)
        scheduled = null
        nextScanAtMs = Long.MAX_VALUE
        fsm.reset()
        lastVisualKey = null
        removeLearnOverlay()
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val from = event?.packageName?.toString() ?: return
        val cfg = settings.refresh()
        if (!cfg.consent) return
        val learning = TeachCaptureStore.requestedPackage
        if (learning != null && from == learning && from in cfg.allowlist) {
            activeGame = from
            showLearnOverlay()
            return
        }
        if (learning == null) removeLearnOverlay()
        if (!cfg.enabled || cfg.allowlist.isEmpty()) return
        if (from == "com.android.vending") {
            val session = fsm.session
            if (cfg.storeRecovery && session != null && session.packageName == activeGame &&
                SystemClock.elapsedRealtime() - lastSeenGameAt < 12_000L) {
                // Opt-in recovery is restricted to a short-lived active session, at most once per redirect.
                if (SystemClock.elapsedRealtime() - lastRecoveryAt > 25_000L) {
                    lastRecoveryAt = SystemClock.elapsedRealtime()
                    handler.postDelayed({
                        val active = rootInActiveWindow?.packageName?.toString()
                        if (settings.refresh().storeRecovery && active == "com.android.vending" &&
                            fsm.session?.packageName == activeGame) {
                            performGlobalAction(GLOBAL_ACTION_BACK)
                            log(session.packageName, "Returned from Play Store during a detected ad; reward unknown")
                        }
                    }, 700)
                }
            }
            return
        }
        if (from !in cfg.allowlist) { stopScanning(); return }
        activeGame = from
        lastSeenGameAt = SystemClock.elapsedRealtime()
        scheduleScan(180)
    }
    private var lastRecoveryAt = 0L
    private fun scheduleScan(delayMs: Long) {
        val desired = SystemClock.elapsedRealtime() + delayMs
        if (scheduled != null && nextScanAtMs <= desired) return
        scheduled?.let(handler::removeCallbacks)
        val work = Runnable { scheduled = null; nextScanAtMs = Long.MAX_VALUE; evaluate() }
        scheduled = work
        nextScanAtMs = desired
        handler.postDelayed(work, delayMs)
    }
    private fun evaluate() {
        val cfg = settings.refresh()
        if (!cfg.consent || !cfg.enabled) { stopScanning(); return }
        val root = rootInActiveWindow ?: return
        val game = root.packageName?.toString() ?: return
        if (game !in cfg.allowlist || game != activeGame) { fsm.reset(); return }
        val metrics = resources.displayMetrics
        val detection = detector.detect(root, metrics.widthPixels, metrics.heightPixels, cfg.extendedDetection)
        val now = SystemClock.elapsedRealtime()
        if (detection.adEvidence) {
            val frame = AdStateMachine.Frame(game, true, detection.completionHint, detection.candidates.map { it.kind }.distinct())
            when (val decision = fsm.observe(frame, getMode(game, cfg), getWait(game, cfg), now)) {
                is AdStateMachine.Decision.Started -> { stats.detected(); log(game, "Ad interface detected") }
                is AdStateMachine.Decision.Click -> {
                    val target = detection.target(decision.action)
                    if (target != null && target.isEnabled && target.isVisibleToUser && target.isClickable) {
                        fsm.actionAttempted(now)
                        if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            stats.actionSucceeded(); log(game, "Accessibility ${decision.action.name} activated; reward unknown")
                            scheduleScan(1700)
                        } else log(game, "Accessible action failed")
                    }
                }
                is AdStateMachine.Decision.Wait -> if (decision.remainingMs > 0) scheduleScan(decision.remainingMs.coerceAtLeast(500))
                is AdStateMachine.Decision.Expired -> log(game, "Timed out safely; no further clicks")
                AdStateMachine.Decision.Ignore -> Unit
            }
        }
        if ((cfg.visualDetection || cfg.learnedTargetsEnabled) && Build.VERSION.SDK_INT >= 30 &&
            !screenshotInFlight && now-lastScreenshotAt > 2100L && (fsm.session?.actionCount ?: 0) < 5) {
            captureScreen(game, cfg)
        }
    }
    private fun captureScreen(game: String, cfg: SettingsRepository.Settings) {
        if (Build.VERSION.SDK_INT < 30 || screenshotInFlight) return
        screenshotInFlight = true
        lastScreenshotAt = SystemClock.elapsedRealtime()
        takeScreenshot(android.view.Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(result: ScreenshotResult) {
                screenshotInFlight = false
                val buffer = result.hardwareBuffer
                val wrapped = try { Bitmap.wrapHardwareBuffer(buffer, result.colorSpace) } catch (_: Exception) { null }
                val image = wrapped?.copy(Bitmap.Config.ARGB_8888, false)
                buffer.close()
                if (image == null) return
                if (TeachCaptureStore.requestedPackage == game) {
                    TeachCaptureStore.receive(game, image)
                    image.recycle()
                    removeLearnOverlay()
                    log(game, "Captured a temporary screenshot for button teaching")
                    return
                }
                if (!settings.refresh().enabled || rootInActiveWindow?.packageName?.toString() != game) { image.recycle(); return }
                vision.analyze(game, image, cfg) { observation ->
                    image.recycle()
                    if (rootInActiveWindow?.packageName?.toString() != game || !settings.refresh().enabled) return@analyze
                    onVisualObservation(game, cfg, observation)
                }
            }
            override fun onFailure(errorCode: Int) { screenshotInFlight = false }
        })
    }
    private fun onVisualObservation(game: String, cfg: SettingsRepository.Settings, observation: VisualRecognition.Observation) {
        val now = SystemClock.elapsedRealtime()
        if (!observation.adEvidence) return
        val actions = observation.targets.map { it.action }.distinct()
        when (val decision = fsm.observe(AdStateMachine.Frame(game, true, observation.completionHint, actions), getMode(game, cfg), getWait(game, cfg), now)) {
            is AdStateMachine.Decision.Started -> { stats.detected(); log(game, "Visual ad evidence detected"); scheduleScan(2200) }
            is AdStateMachine.Decision.Click -> {
                val target = observation.targets.firstOrNull { it.action == decision.action } ?: return
                val bounds = target.bounds
                val w = resources.displayMetrics.widthPixels; val h = resources.displayMetrics.heightPixels
                if (!LearnedTargetStore.inCorner(bounds.centerX(), bounds.centerY(), w, h)) return
                val key = "$game:${target.source}:${decision.action}:${bounds.centerX()/25}:${bounds.centerY()/25}"
                if (key != lastVisualKey || now-lastVisualSeenAt > 6500L) {
                    lastVisualKey = key; lastVisualSeenAt = now
                    scheduleScan(2200)
                    return
                }
                lastVisualKey = null
                fsm.actionAttempted(now)
                val path = Path().apply { moveTo(bounds.centerX().toFloat(), bounds.centerY().toFloat()) }
                val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 70)).build()
                dispatchGesture(gesture, object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        stats.actionSucceeded(); log(game, "${target.source} ${decision.action} gesture sent; reward unknown")
                        scheduleScan(1850)
                    }
                    override fun onCancelled(gestureDescription: GestureDescription?) { log(game, "Visual gesture cancelled") }
                }, handler)
            }
            is AdStateMachine.Decision.Wait -> scheduleScan((decision.remainingMs.coerceAtMost(5000)).coerceAtLeast(1100))
            is AdStateMachine.Decision.Expired -> log(game, "Visual session timed out safely")
            AdStateMachine.Decision.Ignore -> Unit
        }
    }
    private fun getMode(game: String, cfg: SettingsRepository.Settings): AdStateMachine.Mode =
        if ((settings.modeForGame(game) ?: cfg.mode) == SettingsRepository.Mode.FAST_SKIP) AdStateMachine.Mode.FAST_SKIP else AdStateMachine.Mode.REWARD_PRIORITY
    private fun getWait(game: String, cfg: SettingsRepository.Settings): Long = (settings.waitForGame(game) ?: cfg.minimumWaitSeconds) * 1000L
    private fun log(game: String, message: String) { if (settings.refresh().loggingEnabled) history.append(game, message) }

    /** Small temporary accessibility overlay visible only while the approved game is foreground. */
    private fun showLearnOverlay() {
        if (learnOverlay != null) return
        val view = TextView(this).apply {
            text = "◎ CAPTURE AD"
            textSize = 14f
            setTextColor(android.graphics.Color.rgb(11, 26, 27))
            setBackgroundColor(android.graphics.Color.rgb(217, 249, 107))
            setPadding(22, 16, 22, 16)
            elevation = 10f
            setOnClickListener {
                val game = TeachCaptureStore.requestedPackage ?: return@setOnClickListener
                if (rootInActiveWindow?.packageName?.toString() != game) return@setOnClickListener
                removeLearnOverlay()
                handler.postDelayed({ if (Build.VERSION.SDK_INT >= 30) captureScreen(game, settings.refresh()) }, 250)
            }
        }
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.END; x = 12; y = 250 }
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).addView(view, params)
            learnOverlay = view
        } catch (_: Exception) { learnOverlay = null }
    }
    private fun removeLearnOverlay() {
        val view = learnOverlay ?: return
        learnOverlay = null
        try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(view) } catch (_: Exception) { }
    }
    override fun onInterrupt() { stopScanning() }
    override fun onDestroy() {
        settings.removeListener(this)
        stopScanning()
        vision.close()
        super.onDestroy()
    }
}
