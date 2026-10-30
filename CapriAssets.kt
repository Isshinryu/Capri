package com.capri.mobile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.io.IOException

/** Accessors for assets mirrored from upstream assets/ without renaming or conversion. */
object CapriAssets {
    @Volatile private var mono: Typeface? = null

    fun typeface(context: Context): Typeface {
        mono?.let { return it }
        return synchronized(this) {
            mono ?: Typeface.createFromAsset(context.applicationContext.assets, "fonts/ShareTechMono-Regular.ttf").also { mono = it }
        }
    }

    fun bitmap(context: Context, relativePath: String): Bitmap {
        require(!relativePath.startsWith("/") && !relativePath.contains("..")) { "Expected an asset-relative path" }
        return context.assets.open(relativePath).use { stream ->
            BitmapFactory.decodeStream(stream) ?: throw IOException("Could not decode original Capri asset: $relativePath")
        }
    }

    /** Apply the original Share Tech Mono typeface to native text controls in a screen tree. */
    fun applyTypeface(root: View, context: Context) {
        val typeface = typeface(context)
        fun visit(view: View) {
            if (view is TextView) view.typeface = typeface
            if (view is ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index))
        }
        visit(root)
    }
}
