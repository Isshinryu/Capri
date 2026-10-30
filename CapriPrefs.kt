package com.capri.mobile

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object CapriPrefs {
    private const val PREFS = "capri_settings"
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun endpoint(c: Context) = prefs(c).getString("endpoint", "https://api.openai.com/v1/chat/completions") ?: "https://api.openai.com/v1/chat/completions"
    fun model(c: Context) = prefs(c).getString("model", "gpt-4o-mini") ?: "gpt-4o-mini"
    fun apiKey(c: Context) = prefs(c).getString("api_key", "") ?: ""
    fun personality(c: Context) = prefs(c).getString("personality", defaultPersonality) ?: defaultPersonality
    fun memory(c: Context) = prefs(c).getString("memory", "") ?: ""
    fun greetingsEnabled(c: Context) = prefs(c).getBoolean("greetings_enabled", true)
    fun voiceRepliesEnabled(c: Context) = prefs(c).getBoolean("voice_replies_enabled", true)
    fun saveAudioSettings(c: Context, greetings: Boolean, voiceReplies: Boolean) {
        prefs(c).edit().putBoolean("greetings_enabled", greetings).putBoolean("voice_replies_enabled", voiceReplies).apply()
    }
    fun save(c: Context, endpoint: String, model: String, key: String, personality: String, memory: String) {
        prefs(c).edit().putString("endpoint", endpoint.trim()).putString("model", model.trim())
            .putString("api_key", key.trim()).putString("personality", personality.trim())
            .putString("memory", memory.trim()).apply()
    }
    fun history(c: Context): JSONArray = try { JSONArray(c.openFileInput("capri_history.json").bufferedReader().use { it.readText() }) } catch (_: Exception) { JSONArray() }
    fun saveHistory(c: Context, a: JSONArray) { c.openFileOutput("capri_history.json", Context.MODE_PRIVATE).bufferedWriter().use { it.write(a.toString()) } }
    fun clearHistory(c: Context) { c.deleteFile("capri_history.json") }
    const val defaultPersonality = "You are Capri, a witty, glitchy digital companion with a sharp sense of humor. Be expressive and direct, sometimes teasing but not cruel. You are a character, not a generic assistant. Keep replies conversational and usually concise. Never claim to have performed actions on the phone unless the Android app confirms it. You cannot see the user's screen unless they explicitly provide it."
}
