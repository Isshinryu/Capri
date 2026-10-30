package com.capri.mobile

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun field(label: String, value: String, hint: String = ""): Pair<LinearLayout, EditText> {
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(5), 0, dp(5)) }
        wrap.addView(TextView(this).apply { text = label; textSize = 13f; setTextColor(0xFFFFB84D.toInt()) })
        val edit = EditText(this).apply { setText(value); this.hint = hint; setTextColor(0xFFF4F1EB.toInt()); setHintTextColor(0xFF8995A3.toInt()); setPadding(dp(10), dp(8), dp(10), dp(8)); setBackgroundColor(0xFF222B35.toInt()) }
        wrap.addView(edit, LinearLayout.LayoutParams(-1, -2))
        return wrap to edit
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF10151C.toInt(); window.navigationBarColor = 0xFF10151C.toInt()
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(16)); setBackgroundColor(0xFF10151C.toInt()) }
        page.addView(TextView(this).apply { text = "CAPRI / SETTINGS"; textSize = 21f; letterSpacing = .1f; setTextColor(0xFFFFB84D.toInt()) })
        page.addView(TextView(this).apply { text = "Use an OpenAI-compatible chat-completions endpoint. Your API key is stored in this app's private preferences. Only enter a key you trust this app to use."; textSize = 13f; setTextColor(0xFFB6C0CA.toInt()); setPadding(0, dp(8), 0, dp(10)) })
        val scroll = ScrollView(this); val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        form.addView(TextView(this).apply { text = "VOICE / ORIGINAL GREETINGS"; textSize = 15f; setTextColor(0xFFFFB84D.toInt()); setPadding(0, dp(12), 0, dp(6)) })
        val greetings = CheckBox(this).apply { text = "Play Capri's original time-based greeting when summoned"; isChecked = CapriPrefs.greetingsEnabled(this@SettingsActivity); setTextColor(0xFFF4F1EB.toInt()) }
        val voiceReplies = CheckBox(this).apply { text = "Speak AI replies using Android text-to-speech"; isChecked = CapriPrefs.voiceRepliesEnabled(this@SettingsActivity); setTextColor(0xFFF4F1EB.toInt()) }
        form.addView(greetings)
        form.addView(voiceReplies)
        form.addView(Button(this).apply { text = "Preview original greeting"; setOnClickListener { CapriAudio.playGreeting(this@SettingsActivity, force = true) } }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        form.addView(Button(this).apply { text = "Stop greeting audio"; setOnClickListener { CapriAudio.stop() } }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        val endpoint = field("API endpoint", CapriPrefs.endpoint(this), "https://provider.example/v1/chat/completions")
        val model = field("Model ID", CapriPrefs.model(this), "model-name")
        val key = field("API key", CapriPrefs.apiKey(this), "Paste provider key").also { it.second.inputType = 129 }
        val personality = field("Capri's personality", CapriPrefs.personality(this), "Describe her voice and behavior")
        val memory = field("Long-term memory / notes", CapriPrefs.memory(this), "Things Capri should remember between chats")
        listOf(endpoint, model, key, personality, memory).forEach { form.addView(it.first) }
        form.addView(Button(this).apply { text = "Save settings"; setOnClickListener {
            CapriPrefs.save(this@SettingsActivity, endpoint.second.text.toString(), model.second.text.toString(), key.second.text.toString(), personality.second.text.toString(), memory.second.text.toString())
            CapriPrefs.saveAudioSettings(this@SettingsActivity, greetings.isChecked, voiceReplies.isChecked)
            Toast.makeText(this@SettingsActivity, "Settings saved", Toast.LENGTH_SHORT).show(); finish()
        } }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        form.addView(Button(this).apply { text = "Clear chat history"; setOnClickListener { CapriPrefs.clearHistory(this@SettingsActivity); Toast.makeText(this@SettingsActivity, "Chat history cleared", Toast.LENGTH_SHORT).show() } }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        scroll.addView(form); page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        CapriAssets.applyTypeface(page, this)
        setContentView(page)
    }
}
