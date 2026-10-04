package com.kamino.howanyalife.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import com.kamino.howanyalife.model.CatAction
import kotlinx.coroutines.delay

/** Plays the frames for [action], or the placeholder cat when no artwork is provided. */
@Composable
fun CatSprite(
    action: CatAction,
    frames: List<ImageBitmap>?,
    facingRight: Boolean,
    mirrorWhenFacingLeft: Boolean,
    modifier: Modifier = Modifier,
) {
    val frameCount = frames?.size ?: PLACEHOLDER_FRAMES
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(action, frameCount) {
        frame = 0
        while (true) {
            delay(1000L / action.fps)
            frame = if (action.loop) (frame + 1) % frameCount else minOf(frame + 1, frameCount - 1)
        }
    }

    // The placeholder is drawn side-on, so it always turns to face where it walks.
    val mirror = !facingRight && (frames.isNullOrEmpty() || mirrorWhenFacingLeft)
    val flip = Modifier.graphicsLayer { scaleX = if (mirror) -1f else 1f }
    if (frames.isNullOrEmpty()) {
        PlaceholderCat(action, frame, modifier.then(flip))
    } else {
        Image(bitmap = frames[frame % frames.size], contentDescription = null, modifier = modifier.then(flip))
    }
}
