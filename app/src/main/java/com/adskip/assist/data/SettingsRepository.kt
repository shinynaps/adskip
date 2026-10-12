package com.adskip.assist.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Stores no screen contents, raw accessibility events, credentials, or identifiers. */
class SettingsRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    data class Settings(
        val consent: Boolean = false,
        val enabled: Boolean = false,
        val mode: Mode = Mode.REWARD_PRIORITY,
        val minimumWaitSeconds: Int = 40,
        val allowlist: Set<String> = emptySet(),
        val loggingEnabled: Boolean = true,
        val extendedDetection: Boolean = false,
        val visualDetection: Boolean = false,
        val learnedTargetsEnabled: Boolean = false,
        val storeRecovery: Boolean = false,
        val revision: Int = 0,
    )
    enum class Mode { REWARD_PRIORITY, FAST_SKIP }

    private fun load(): Settings = Settings(
        consent = prefs.getBoolean("consent", false),
        enabled = prefs.getBoolean("enabled", false),
        mode = runCatching { Mode.valueOf(prefs.getString("mode", Mode.REWARD_PRIORITY.name)!!) }
            .getOrDefault(Mode.REWARD_PRIORITY),
        minimumWaitSeconds = prefs.getInt("wait", 40).coerceIn(5, 120),
        allowlist = prefs.getStringSet("games", emptySet())?.toSet() ?: emptySet(),
        loggingEnabled = prefs.getBoolean("logging", true),
        extendedDetection = prefs.getBoolean("extended", false),
        visualDetection = prefs.getBoolean("visual", false),
        learnedTargetsEnabled = prefs.getBoolean("learned", false),
        storeRecovery = prefs.getBoolean("store_recovery", false),
        revision = prefs.getInt("revision", 0)
    )
    private val mutableState = MutableStateFlow(load())
    val state: StateFlow<Settings> = mutableState

    private fun update(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).putInt("revision", prefs.getInt("revision", 0) + 1).apply()
        mutableState.value = load()
    }
    fun setConsent(value: Boolean) = update {
        putBoolean("consent", value)
        if (!value) putBoolean("enabled", false)
    }
    fun setEnabled(value: Boolean) = update { putBoolean("enabled", value && state.value.consent) }
    fun setMode(value: Mode) = update { putString("mode", value.name) }
    fun setWaitSeconds(value: Int) = update { putInt("wait", value.coerceIn(5, 120)) }
    fun setLoggingEnabled(value: Boolean) = update { putBoolean("logging", value) }
    fun setExtendedDetection(value: Boolean) = update { putBoolean("extended", value) }
    fun setVisualDetection(value: Boolean) = update { putBoolean("visual", value) }
    fun setLearnedTargetsEnabled(value: Boolean) = update { putBoolean("learned", value) }
    fun setStoreRecovery(value: Boolean) = update { putBoolean("store_recovery", value) }
    fun setGameEnabled(packageName: String, value: Boolean) {
        if (packageName.isBlank()) return
        val next = state.value.allowlist.toMutableSet()
        if (value) next.add(packageName) else next.remove(packageName)
        update { putStringSet("games", next) }
    }
    fun setGameMode(game: String, mode: Mode?) = update {
        val key = "game_mode_$game"
        if (mode == null) remove(key) else putString(key, mode.name)
    }
    fun modeForGame(game: String): Mode? = runCatching {
        prefs.getString("game_mode_$game", null)?.let(Mode::valueOf)
    }.getOrNull()
    fun setGameWait(game: String, seconds: Int?) = update {
        val key = "game_wait_$game"
        if (seconds == null) remove(key) else putInt(key, seconds.coerceIn(5, 120))
    }
    fun waitForGame(game: String): Int? = if (prefs.contains("game_wait_$game"))
        prefs.getInt("game_wait_$game", 40).coerceIn(5, 120) else null
    fun addListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(listener)
    fun removeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    /** SharedPreferences changes from UI are picked up synchronously when service receives events. */
    fun refresh(): Settings = load().also { mutableState.value = it }

    companion object {
        @Volatile private var instance: SettingsRepository? = null
        fun get(context: Context): SettingsRepository = instance ?: synchronized(this) {
            instance ?: SettingsRepository(context).also { instance = it }
        }
    }
}
