package com.adskip.assist.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Log only our own state transitions. Never persist accessibility text or UI tree data. */
class ActivityLogRepository private constructor(context: Context) {
    data class Entry(val timestamp: Long, val packageName: String, val message: String)
    private val prefs = context.applicationContext.getSharedPreferences("history", Context.MODE_PRIVATE)
    private fun read(): List<Entry> = runCatching {
        val records = JSONArray(prefs.getString("recent", "[]"))
        (0 until records.length()).map { index ->
            val item = records.getJSONObject(index)
            Entry(item.getLong("time"), item.getString("game"), item.getString("event"))
        }
    }.getOrDefault(emptyList())
    private val mutableEntries = MutableStateFlow(read())
    val entries: StateFlow<List<Entry>> = mutableEntries

    @Synchronized fun append(game: String, event: String, loggingEnabled: Boolean = true) {
        if (!loggingEnabled) return
        val next = (listOf(Entry(System.currentTimeMillis(), game, event)) + read()).take(150)
        val array = JSONArray()
        next.forEach { item ->
            array.put(JSONObject().put("time", item.timestamp).put("game", item.packageName).put("event", item.message))
        }
        prefs.edit().putString("recent", array.toString()).apply()
        mutableEntries.value = next
    }
    @Synchronized fun clear() {
        prefs.edit().remove("recent").apply()
        mutableEntries.value = emptyList()
    }
    companion object {
        @Volatile private var instance: ActivityLogRepository? = null
        fun get(context: Context): ActivityLogRepository = instance ?: synchronized(this) {
            instance ?: ActivityLogRepository(context).also { instance = it }
        }
    }
}
