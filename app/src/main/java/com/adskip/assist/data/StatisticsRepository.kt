package com.adskip.assist.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StatisticsRepository private constructor(context: Context) {
    data class Statistics(val adsDetected: Int = 0, val actionsSucceeded: Int = 0)
    private val prefs = context.applicationContext.getSharedPreferences("statistics", Context.MODE_PRIVATE)
    private fun load() = Statistics(prefs.getInt("detected", 0), prefs.getInt("acted", 0))
    private val mutableState = MutableStateFlow(load())
    val state: StateFlow<Statistics> = mutableState
    @Synchronized fun detected() {
        prefs.edit().putInt("detected", load().adsDetected + 1).apply()
        mutableState.value = load()
    }
    @Synchronized fun actionSucceeded() {
        prefs.edit().putInt("acted", load().actionsSucceeded + 1).apply()
        mutableState.value = load()
    }
    companion object {
        @Volatile private var instance: StatisticsRepository? = null
        fun get(context: Context): StatisticsRepository = instance ?: synchronized(this) {
            instance ?: StatisticsRepository(context).also { instance = it }
        }
    }
}
