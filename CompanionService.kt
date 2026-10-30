package com.capri.mobile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.random.Random

/**
 * Android overlay port of Capri's movement/presence layer.
 * Desktop window-edge perches cannot be enumerated by Android apps, so the mobile
 * equivalent uses safe screen-edge/bottom perches and keeps all movement within
 * the usable display area. Original sprites and animation names are retained.
 */
class CompanionService : Service() {
    private lateinit var windowManager: WindowManager
    private var companion: FrameLayout? = null
    private var sprite: SpriteStripView? = null
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private val random = Random(System.nanoTime())
    private var startX = 0
    private var startY = 0
    private var touchX = 0f
    private var touchY = 0f
    private var moved = false
    private var downAt = 0L
    private var dragging = false
    private var walking = false
    private var targetX = 0
    private var nextWanderAt = 0L
    private var lastMoveAt = 0L
    private val density get() = resources.displayMetrics.density
    private val petSize get() = (152 * density).toInt()
    private val screenWidth get() = resources.displayMetrics.widthPixels
    private val screenHeight get() = resources.displayMetrics.heightPixels
    private val maxX get() = (screenWidth - petSize).coerceAtLeast(0)
    private val floorY get() = (screenHeight - petSize - (64 * density).toInt()).coerceAtLeast(0)

    private val movement = object : Runnable {
        override fun run() {
            val view = companion
            if (view == null || !::params.isInitialized) return
            val now = System.currentTimeMillis()
            if (!dragging && now >= nextWanderAt) {
                if (!walking) {
                    // Pause naturally between walks, as a desktop pet would.
                    val edgePause = params.x <= (12 * density).toInt() || params.x >= maxX - (12 * density).toInt()
                    if (edgePause) sprite?.setSeated(true)
                    else sprite?.setSeated(false)
                    targetX = if (random.nextFloat() < 0.18f) {
                        if (params.x < maxX / 2) maxX else 0
                    } else random.nextInt(0, maxX.coerceAtLeast(1) + 1)
                    if (abs(targetX - params.x) > (42 * density).toInt()) {
                        walking = true
                        sprite?.setSeated(false)
                        sprite?.setWalking(true)
                        lastMoveAt = now
                    } else {
                        nextWanderAt = now + random.nextLong(7_000L, 18_000L)
                    }
                }
            }
            if (!dragging && walking) {
                val delta = targetX - params.x
                val step = (2.1f * density).toInt().coerceAtLeast(1)
                if (abs(delta) <= step || now - lastMoveAt > 9_000L) {
                    params.x = targetX.coerceIn(0, maxX)
                    walking = false
                    sprite?.setWalking(false)
                    val atEdge = params.x <= (12 * density).toInt() || params.x >= maxX - (12 * density).toInt()
                    sprite?.setSeated(atEdge)
                    nextWanderAt = now + random.nextLong(8_000L, 20_000L)
                } else {
                    params.x = (params.x + if (delta > 0) step else -step).coerceIn(0, maxX)
                }
                params.y = floorY
                runCatching { windowManager.updateViewLayout(view, params) }
            }
            handler.postDelayed(this, 100L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(41, notification())
        CapriAudio.playGreeting(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        sprite = SpriteStripView(this)
        companion = FrameLayout(this).apply { addView(sprite, FrameLayout.LayoutParams(-1, -1)) }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        params = WindowManager.LayoutParams(petSize, petSize, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (12 * density).toInt()
            y = floorY
        }
        targetX = params.x
        nextWanderAt = System.currentTimeMillis() + random.nextLong(5_000L, 11_000L)
        companion?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    moved = false; dragging = false; downAt = event.eventTime
                    walking = false; sprite?.setWalking(false); sprite?.setDragging(false)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX; val dy = event.rawY - touchY
                    if (dx * dx + dy * dy > 64) {
                        moved = true
                        if (!dragging) { dragging = true; sprite?.setDragging(true); sprite?.setSeated(false) }
                    }
                    if (dragging) {
                        params.x = (startX + dx.toInt()).coerceIn(0, maxX)
                        params.y = (startY + dy.toInt()).coerceIn(0, (screenHeight - petSize).coerceAtLeast(0))
                        runCatching { windowManager.updateViewLayout(view, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dx = event.rawX - touchX; val dy = event.rawY - touchY
                    if (!moved && dx * dx + dy * dy < 100) {
                        sprite?.onPetInteraction()
                        if (event.eventTime - downAt >= 550L) sprite?.nextAnimation() else openChat()
                    }
                    if (dragging) {
                        dragging = false
                        sprite?.setDragging(false)
                        params.x = params.x.coerceIn(0, maxX)
                        params.y = floorY
                        val edge = params.x <= (12 * density).toInt() || params.x >= maxX - (12 * density).toInt()
                        sprite?.setSeated(edge)
                        runCatching { windowManager.updateViewLayout(view, params) }
                    }
                    nextWanderAt = System.currentTimeMillis() + random.nextLong(6_000L, 13_000L)
                    true
                }
                else -> false
            }
        }
        runCatching { windowManager.addView(companion, params) }.onFailure { stopSelf() }
        handler.postDelayed(movement, 100L)
    }

    private fun openChat() {
        val intent = Intent(this, ChatActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    private fun notification(): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel("capri_companion", "Capri companion", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0))
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, "capri_companion").setContentTitle("Capri is here").setContentText("Capri wanders along the screen edge. Tap to chat.").setSmallIcon(android.R.drawable.ic_dialog_info).setContentIntent(open).setOngoing(true).build()
        else @Suppress("DEPRECATION") Notification.Builder(this).setContentTitle("Capri is here").setContentText("Tap Capri to chat.").setSmallIcon(android.R.drawable.ic_dialog_info).setContentIntent(open).setOngoing(true).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onDestroy() {
        handler.removeCallbacks(movement)
        CapriAudio.stop()
        companion?.let { runCatching { windowManager.removeView(it) } }
        companion = null; sprite = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
