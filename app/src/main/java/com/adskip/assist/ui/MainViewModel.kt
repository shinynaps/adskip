package com.adskip.assist.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.adskip.assist.data.ActivityLogRepository
import com.adskip.assist.data.SettingsRepository
import com.adskip.assist.data.StatisticsRepository

/** View-model owns app state sources; Android UI only observes them. */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    val settingsRepository = SettingsRepository.get(application)
    val logRepository = ActivityLogRepository.get(application)
    val statisticsRepository = StatisticsRepository.get(application)

    val settings = settingsRepository.state
    val activity = logRepository.entries
    val statistics = statisticsRepository.state
}
