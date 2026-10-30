package com.capri.mobile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.View
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/** Android renderer ported from the original Capri desktop Clip/Body/Advance logic. */
class SpriteStripView(context: Context) : View(context) {
    private data class ClipSpec(val frames: Int, val fps: Int, val heightScale: Float, val loop: Boolean = true)
    private data class Clip(val bitmap: Bitmap, val spec: ClipSpec) {
        val frameWidth: Int = max(1, bitmap.width / spec.frames)
        val frameHeight: Int = bitmap.height
        val durationMs: Long = (spec.frames * 1000L / spec.fps).coerceAtLeast(1L)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val handler = Handler(Looper.getMainLooper())
    private val random = Random(System.nanoTime())
    private val specs = linkedMapOf(
        "idle" to ClipSpec(16, 8, 1.11f),
        "blink" to ClipSpec(4, 8, 1.11f),
        "blink_glitch" to ClipSpec(4, 8, 1.11f),
        "glitch" to ClipSpec(10, 15, 1.11f, false),
        "dangle" to ClipSpec(24, 10, 1.11f),
        "sleep" to ClipSpec(16, 6, 1.11f),
        "wake" to ClipSpec(13, 15, 1.11f, false),
        "perch" to ClipSpec(16, 8, 1f),
        "perch_blink" to ClipSpec(4, 8, 1f),
        "perch_blink_glitch" to ClipSpec(4, 8, 1f),
        "perch_glitch" to ClipSpec(10, 15, 1f, false),
        "mouth" to ClipSpec(5, 12, 1.11f),
        "walk" to ClipSpec(12, 12, 1.11f),
        "dance" to ClipSpec(16, 16, 1.11f),
        "perch_dance" to ClipSpec(16, 16, 1f),
        "arrive" to ClipSpec(14, 15, 1.11f, false),
        "leave" to ClipSpec(12, 15, 1.11f, false),
        "perch_arrive" to ClipSpec(14, 15, 1f, false),
        "perch_leave" to ClipSpec(12, 15, 1f, false),
        "grin" to ClipSpec(16, 8, 1.11f)
    )
    private val clips = linkedMapOf<String, Clip>()
    private var current = "arrive"
    private var startedAt = System.currentTimeMillis()
    private var lastTick = startedAt
    private var lastTouchAt = startedAt
    private var nextGlitchAt = startedAt + random.nextLong(30_000L, 60_001L)
    private var blinkVariant: String? = null
    private var sleeping = false
    private var seated = false
    private var talking = false
    private var dancing = false
    private var dragging = false
    private var breathe = 0f
    private var tilt = 0f
    private var blinkCycle = 0

    private val tick = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            updateState(now)
            invalidate()
            lastTick = now
            handler.postDelayed(this, 33L)
        }
    }

    init {
        // Load the exact upstream sprite files. No generated or substituted artwork.
        specs.forEach { (name, spec) ->
            runCatching { context.assets.open("sprites/$name.png").use { stream -> BitmapFactory.decodeStream(stream) } }
                .getOrNull()?.let { clips[name] = Clip(it, spec) }
        }
        play("arrive")
        handler.post(tick)
    }

    /** Called by the overlay when the user touches Capri. */
    fun onPetInteraction() {
        val now = System.currentTimeMillis()
        lastTouchAt = now
        if (sleeping) {
            sleeping = false
            play("wake")
        } else if (current == "idle" || current == "perch") {
            play("grin")
        }
    }

    fun setSeated(value: Boolean) {
        if (seated == value) return
        seated = value
        blinkVariant = null
        play(if (seated) "perch_arrive" else "arrive")
    }

    fun setTalking(value: Boolean) {
        talking = value
        if (value && current != "idle" && current != "perch") play(if (seated) "perch" else "idle")
    }

    fun setDragging(value: Boolean) {
        dragging = value
        if (value) {
            lastTouchAt = System.currentTimeMillis()
            if (current != "dangle") play("dangle")
        } else if (current == "dangle") {
            play(if (seated) "perch" else "idle")
        }
    }

    /** Walking is driven by the overlay's screen-safe movement controller. */
    fun setWalking(value: Boolean) {
        if (dragging || sleeping || talking || dancing) return
        if (value) {
            if (current != "walk" && current != "dangle") play("walk")
        } else if (current == "walk") {
            play(if (seated) "perch" else "idle")
        }
    }

    fun setDancing(value: Boolean) {
        dancing = value
        if (dancing && (current == "idle" || current == "perch")) play(if (seated) "perch_dance" else "dance")
        else if (!dancing && (current == "dance" || current == "perch_dance")) play(if (seated) "perch" else "idle")
    }

    /** Retained as a test hook; normal behavior is autonomous, not a manual animation carousel. */
    fun nextAnimation() {
        lastTouchAt = System.currentTimeMillis()
        when (current) {
            "idle", "perch" -> play(if (seated) "perch_glitch" else "glitch")
            else -> play(if (seated) "perch" else "idle")
        }
    }

    fun showAnimation(name: String) {
        if (clips.containsKey(name)) play(name)
    }

    private fun play(name: String) {
        val actual = if (clips.containsKey(name)) name else if (seated) "perch" else "idle"
        current = actual
        startedAt = System.currentTimeMillis()
        blinkVariant = null
        invalidate()
    }

    private fun updateState(now: Long) {
        if (!sleeping && now - lastTouchAt >= 240_000L && !dragging && !talking && !dancing && current == "idle") {
            sleeping = true
            play("sleep")
        }
        if (sleeping && now - lastTouchAt < 240_000L) {
            sleeping = false
            play("wake")
        }

        val clip = clips[current] ?: clips[if (seated) "perch" else "idle"] ?: return
        val elapsed = now - startedAt
        val frame = (elapsed * clip.spec.fps / 1000L).toInt()

        if (current == "grin" && elapsed >= clip.durationMs * 2L) {
            play(if (seated) "perch" else "idle")
            return
        }
        if (!clip.spec.loop && frame >= clip.spec.frames) {
            when (current) {
                "arrive", "wake", "grin" -> play(if (seated) "perch" else "idle")
                "perch_arrive", "perch_glitch" -> play("perch")
                "glitch" -> play("idle")
                "leave" -> play("idle")
                "perch_leave" -> play("perch")
                else -> play(if (seated) "perch" else "idle")
            }
            return
        }

        if (!sleeping && !talking && !dancing && (current == "idle" || current == "perch")) {
            val cycle = (elapsed / clip.durationMs).toInt()
            if (cycle != blinkCycle) {
                blinkCycle = cycle
                val roll = random.nextFloat()
                blinkVariant = when {
                    roll < 0.10f -> if (seated) "perch_blink_glitch" else "blink_glitch"
                    roll < 0.55f -> if (seated) "perch_blink" else "blink"
                    else -> null
                }
                if (now >= nextGlitchAt) {
                    nextGlitchAt = now + random.nextLong(30_000L, 60_001L)
                    play(if (seated) "perch_glitch" else "glitch")
                    return
                }
            }
        }

        // Match the desktop's subtle breathing and sway while settled.
        val settled = !dragging && current != "dangle" && !sleeping
        breathe = if (settled) (sin(now / 570.0) * 2.2).toFloat() else 0f
        tilt = if (settled) (sin(now / 1050.0) * 0.65).toFloat() else 0f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val base = clips[current] ?: clips[if (seated) "perch" else "idle"] ?: return
        val elapsed = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
        var shown = base
        var frame = ((elapsed * base.spec.fps / 1000L).toInt()).coerceAtLeast(0)
        if (base.spec.loop) frame %= base.spec.frames else frame = frame.coerceAtMost(base.spec.frames - 1)

        // The original idle/perch strips replace cells 9-12 with a separately timed blink strip.
        if ((current == "idle" || current == "perch") && frame in 9..12 && blinkVariant != null) {
            clips[blinkVariant]?.let {
                shown = it
                frame -= 9
                frame = frame.coerceAtMost(it.spec.frames - 1)
            }
        }

        val src = Rect(frame * shown.frameWidth, 0, (frame + 1) * shown.frameWidth, shown.frameHeight)
        val scale = shown.spec.heightScale
        val targetHeight = height * scale
        val targetWidth = targetHeight * shown.frameWidth / shown.frameHeight.toFloat()
        val left = (width - targetWidth) / 2f
        val top = (height - targetHeight) / 2f + breathe
        val target = RectF(left, top, left + targetWidth, top + targetHeight)
        val save = canvas.save()
        canvas.rotate(tilt, width / 2f, height / 2f)
        canvas.drawBitmap(shown.bitmap, src, target, paint)

        // The desktop draws mouth cells over the idle body while Capri is speaking.
        if (talking && current == "idle") {
            clips["mouth"]?.let { mouth ->
                val mouthFrame = ((System.currentTimeMillis() / 83L) % mouth.spec.frames).toInt()
                canvas.drawBitmap(mouth.bitmap, Rect(mouthFrame * mouth.frameWidth, 0, (mouthFrame + 1) * mouth.frameWidth, mouth.frameHeight), target, paint)
            }
        }
        canvas.restoreToCount(save)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tick)
        clips.values.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
        clips.clear()
        super.onDetachedFromWindow()
    }
}
