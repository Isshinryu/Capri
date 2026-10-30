package com.capri.mobile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(dp(24), dp(28), dp(24), dp(24)); setBackgroundColor(0xFF10151C.toInt()) }
        root.addView(ImageView(this).apply {
            setImageBitmap(CapriAssets.bitmap(this@MainActivity, "capri.png"))
            contentDescription = "Original Capri character artwork"
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }, LinearLayout.LayoutParams(dp(138), dp(158)).apply { bottomMargin = dp(8) })
        root.addView(TextView(this).apply { text = "CAPRI / MOBILE"; textSize = 25f; setTextColor(0xFFFFB84D.toInt()); letterSpacing = .12f }, LinearLayout.LayoutParams(-1, -2))
        root.addView(TextView(this).apply { text = "A mobile port in progress"; textSize = 15f; setTextColor(0xFFB6C0CA.toInt()); setPadding(0, dp(8), 0, dp(20)) })
        root.addView(TextView(this).apply { text = "Capri can now float over other apps, keep chat history, use a configurable AI chat-completions provider, listen through Android speech recognition, and speak replies with Android Text-to-Speech. Desktop-only powers still need Android replacements."; textSize = 15f; setTextColor(0xFFF4F1EB.toInt()); setLineSpacing(dp(4).toFloat(), 1f) })
        root.addView(Button(this).apply { text = "Open Capri Chat"; setOnClickListener { startActivity(Intent(this@MainActivity, ChatActivity::class.java)) } }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) })
        root.addView(Button(this).apply { text = "AI / Voice / Memory Settings"; setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) } }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        root.addView(Button(this).apply { text = "Summon floating Capri"; setOnClickListener { summon() } }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        root.addView(Button(this).apply { text = "Dismiss floating Capri"; setOnClickListener { stopService(Intent(this@MainActivity, CompanionService::class.java)) } }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        root.addView(TextView(this).apply { text = "First run: configure an API key in Settings for AI chat. Overlay permission is needed for the floating companion. API usage may cost money depending on your provider."; textSize = 12f; setTextColor(0xFF9EABB8.toInt()); setPadding(0, dp(20), 0, 0) })
        CapriAssets.applyTypeface(root, this)
        setContentView(root)
    }
    private fun summon() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Allow Display over other apps, then tap Summon again.", Toast.LENGTH_LONG).show(); return
        }
        val i = Intent(this, CompanionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
        Toast.makeText(this, "Capri is floating. Tap her to open chat; long-press to change animation.", Toast.LENGTH_SHORT).show()
    }
}
