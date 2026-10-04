package com.kamino.howanyalife.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.kamino.howanyalife.model.CatAction
import java.io.IOException
import java.util.Properties

/** The character's frames per action, and whether to mirror them when the cat faces left. */
data class CatSprites(
    val frames: Map<CatAction, List<ImageBitmap>> = emptyMap(),
    val mirrorWhenFacingLeft: Boolean = true,
)

/**
 * Loads the character's animation frames: the PNG files in `assets/cat/<action>/`.
 *
 * Frames play in file-name order. An action with no folder (or no PNGs) is left out
 * of the result, and the UI draws the placeholder cat for it instead.
 */
class SpriteStore(private val context: Context) {

    fun loadAll(): CatSprites = CatSprites(
        frames = CatAction.entries.mapNotNull { action ->
            loadFrames(action).takeIf { it.isNotEmpty() }?.let { action to it }
        }.toMap(),
        mirrorWhenFacingLeft = loadSettings().getProperty("mirrorWhenFacingLeft", "true").toBoolean(),
    )

    /** The first frame of [action] as a plain Bitmap (for the home-screen widget), or null if there is no artwork. */
    fun loadFirstFrame(action: CatAction): Bitmap? = loadFrames(action).firstOrNull()?.asAndroidBitmap()

    /** Optional `assets/cat/settings.properties`. */
    private fun loadSettings(): Properties = Properties().apply {
        try {
            context.assets.open("$ROOT/settings.properties").use { load(it) }
        } catch (e: IOException) {
            // No settings file: keep the defaults.
        }
    }

    private fun loadFrames(action: CatAction): List<ImageBitmap> {
        val dir = "$ROOT/${action.assetDir}"
        val names = try {
            context.assets.list(dir).orEmpty()
        } catch (e: IOException) {
            emptyArray()
        }
        return names
            .filter { it.endsWith(".png", ignoreCase = true) }
            .sorted()
            .mapNotNull { decode("$dir/$it") }
    }

    private fun decode(path: String): ImageBitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }

        // Downsample oversized artwork to keep memory use reasonable on a watch.
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_FRAME_SIZE) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
    } catch (e: IOException) {
        Log.w(TAG, "Could not load sprite $path", e)
        null
    }

    private companion object {
        const val TAG = "SpriteStore"
        const val ROOT = "cat"
        const val MAX_FRAME_SIZE = 256
    }
}
