package com.adskip.assist.automation

/** Pure Kotlin core; uses monotonic timestamps provided by the caller. */
class AdStateMachine {
    enum class Mode { REWARD_PRIORITY, FAST_SKIP }
    enum class Action { SKIP, CLOSE, RETURN_TO_GAME }
    enum class Stage { IDLE, WATCHING, WAITING, ACTION_SENT }
    data class Frame(
        val packageName: String,
        val verifiedAdContext: Boolean,
        val completionHint: Boolean,
        val actions: List<Action>
    )
    sealed interface Decision {
        data object Ignore : Decision
        data class Wait(val remainingMs: Long) : Decision
        data class Click(val action: Action) : Decision
        data class Started(val packageName: String) : Decision
        data object Expired : Decision
    }
    data class Session(
        val packageName: String,
        val startedMs: Long,
        var lastEvidenceMs: Long,
        var lastActionMs: Long = Long.MIN_VALUE / 2,
        var actionCount: Int = 0,
        var stage: Stage = Stage.WATCHING,
    )

    private var active: Session? = null
    val session: Session? get() = active?.copy()

    fun reset() { active = null }
    fun observe(frame: Frame, mode: Mode, minimumWatchMs: Long, nowMs: Long): Decision {
        if (frame.packageName.isBlank()) { reset(); return Decision.Ignore }
        var current = active
        if (current != null && frame.packageName != current.packageName) {
            reset(); current = null
        }
        if (current != null && (nowMs - current.startedMs > 150_000L || current.actionCount >= 5)) {
            reset()
            return Decision.Expired
        }
        if (!frame.verifiedAdContext) {
            if (current != null && nowMs - current.lastEvidenceMs > 5_000L) reset()
            // Never click controls on a frame without *current* ad evidence.
            return Decision.Ignore
        }
        if (current == null) {
            active = Session(frame.packageName, nowMs, nowMs)
            return Decision.Started(frame.packageName)
        }
        current.lastEvidenceMs = nowMs
        if (nowMs - current.lastActionMs < 1_500L) {
            current.stage = Stage.ACTION_SENT
            return Decision.Wait(1_500L - (nowMs - current.lastActionMs))
        }
        val chosen = frame.actions.firstOrNull() ?: run {
            current.stage = Stage.WATCHING
            return Decision.Wait(0)
        }
        val elapsed = nowMs - current.startedMs
        val remaining = (minimumWatchMs.coerceIn(5_000L, 120_000L) - elapsed).coerceAtLeast(0)
        if (mode == Mode.REWARD_PRIORITY && !frame.completionHint && remaining > 0) {
            current.stage = Stage.WAITING
            return Decision.Wait(remaining)
        }
        current.stage = Stage.WAITING
        return Decision.Click(chosen)
    }
    fun actionAttempted(nowMs: Long) {
        active?.let {
            it.actionCount++
            it.lastActionMs = nowMs
            it.stage = Stage.ACTION_SENT
        }
    }
}
