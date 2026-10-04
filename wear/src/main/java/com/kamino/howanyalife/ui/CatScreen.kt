package com.kamino.howanyalife.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.kamino.howanyalife.R
import com.kamino.howanyalife.data.SoundMode
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val DaySky = listOf(Color(0xFF8FD3F4), Color(0xFFFFE5C4))
private val NightSky = listOf(Color(0xFF0E1433), Color(0xFF2B3A67))
private val DayFloor = Color(0xFFC9A27E)
private val NightFloor = Color(0xFF5A4A63)
private val FullnessColor = Color(0xFFFFB347)
private val EnergyColor = Color(0xFF7ED6A5)
private val TrackColor = Color.White.copy(alpha = 0.25f)

/** Floor line, as a fraction of the screen height. */
private const val FLOOR_Y = 0.66f
/** How far the cat's center may move from the screen center, as a fraction of the screen width. */
private const val WALK_RANGE = 0.3f
private const val CAT_SIZE = 0.36f
private const val BOWL_SIZE = 0.14f

@Composable
fun CatScreen(viewModel: CatViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val sprites by viewModel.sprites.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }

    // The watch starts with sound effects only: its speaker is small and BGM costs battery.
    val soundSettings = rememberSoundSettings(default = SoundMode.EFFECTS_ONLY)
    val soundMode by soundSettings.mode.collectAsStateWithLifecycle(initialValue = null)
    var petted by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    CatSounds(ui, soundMode, petted)

    MaterialTheme {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val screen = maxWidth
            Canvas(Modifier.fillMaxSize()) { drawRoom(ui.night) }

            val catSize = screen * CAT_SIZE
            // Where the cat's paws touch the ground (legs end at 92% of the sprite height).
            val groundY = screen * FLOOR_Y + catSize * 0.12f

            val catDescription = stringResource(R.string.cat_description)
            CatSprite(
                action = ui.action,
                frames = sprites.frames[ui.action],
                facingRight = ui.facingRight,
                mirrorWhenFacingLeft = sprites.mirrorWhenFacingLeft,
                modifier = Modifier
                    .offset(x = floorX(screen, ui.x) - catSize / 2, y = screen * FLOOR_Y - catSize * 0.8f)
                    .size(catSize)
                    .semantics { contentDescription = catDescription }
                    .clickable(interactionSource = null, indication = null) {
                        petted++
                        viewModel.pet()
                    },
            )

            // Drawn after the cat so the food stays visible while it eats.
            ui.bowlX?.let { bowlX ->
                val bowlSize = screen * BOWL_SIZE
                Canvas(
                    Modifier
                        .offset(x = floorX(screen, bowlX) - bowlSize / 2, y = groundY - bowlSize * 0.9f)
                        .size(bowlSize),
                ) { drawBowl(ui.bowlFull) }
            }

            Canvas(Modifier.fillMaxSize()) {
                drawStatusArcs(ui.stats.fullness / 100f, ui.stats.energy / 100f)
            }

            // Sound button at the top, between the two status arcs.
            soundMode?.let { mode ->
                val soundDescription = stringResource(mode.label)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                        .size(36.dp)
                        .semantics { contentDescription = soundDescription }
                        .clickable { scope.launch { soundSettings.set(mode.next()) } },
                ) {
                    Text(mode.icon)
                }
            }

            val feedDescription = stringResource(R.string.feed_description)
            EdgeButton(
                onClick = viewModel::feed,
                enabled = ui.loaded && ui.bowlX == null,
                buttonSize = EdgeButtonSize.ExtraSmall,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .semantics { contentDescription = feedDescription },
            ) {
                Text(stringResource(R.string.feed))
            }
        }
    }
}

private val SoundMode.label: Int
    get() = when (this) {
        SoundMode.ALL -> R.string.sound_all
        SoundMode.EFFECTS_ONLY -> R.string.sound_effects_only
        SoundMode.OFF -> R.string.sound_off
    }

private fun floorX(screen: Dp, x: Float): Dp = screen / 2 + screen * WALK_RANGE * x

private fun DrawScope.drawRoom(night: Boolean) {
    drawRect(Brush.verticalGradient(if (night) NightSky else DaySky))
    if (night) {
        val stars = listOf(0.25f to 0.22f, 0.45f to 0.14f, 0.62f to 0.28f, 0.78f to 0.2f, 0.35f to 0.38f, 0.7f to 0.42f)
        stars.forEach { (x, y) -> drawCircle(Color.White.copy(alpha = 0.8f), 1.5.dp.toPx(), Offset(x * size.width, y * size.height)) }
        drawCircle(Color(0xFFFFF4C2), size.width * 0.05f, Offset(size.width * 0.3f, size.height * 0.3f))
    } else {
        drawCircle(Color(0xFFFFE07A), size.width * 0.06f, Offset(size.width * 0.72f, size.height * 0.3f))
    }
    val floorTop = size.height * FLOOR_Y
    drawRect(if (night) NightFloor else DayFloor, topLeft = Offset(0f, floorTop), size = Size(size.width, size.height - floorTop))
}

private fun DrawScope.drawBowl(full: Boolean) {
    val w = size.width
    val h = size.height
    if (full) {
        drawOval(Color(0xFF8B5A2B), topLeft = Offset(0.15f * w, 0.25f * h), size = Size(0.7f * w, 0.35f * h))
    }
    drawArc(Color(0xFF4F7CAC), 0f, 180f, true, topLeft = Offset(0.05f * w, 0.05f * h), size = Size(0.9f * w, 0.85f * h))
}

/** Two arcs along the top edge: fullness on the left, energy on the right. */
private fun DrawScope.drawStatusArcs(fullness: Float, energy: Float) {
    val stroke = 5.dp.toPx()
    val inset = 6.dp.toPx() + stroke / 2
    val topLeft = Offset(inset, inset)
    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
    val sweep = 55f
    val leftStart = 205f
    val rightStart = 280f
    val style = Stroke(width = stroke, cap = StrokeCap.Round)

    drawArc(TrackColor, leftStart, sweep, false, topLeft, arcSize, style = style)
    drawArc(TrackColor, rightStart, sweep, false, topLeft, arcSize, style = style)
    if (fullness > 0f) drawArc(FullnessColor, leftStart, sweep * fullness, false, topLeft, arcSize, style = style)
    // The energy arc fills from its outer end so both arcs grow toward the top.
    if (energy > 0f) {
        drawArc(EnergyColor, rightStart + sweep, -sweep * energy, false, topLeft, arcSize, style = style)
    }

    // Small dot icons at the outer end of each arc to tell them apart.
    val radius = arcSize.width / 2
    val center = Offset(size.width / 2, size.height / 2)
    fun pointAt(angle: Float): Offset {
        val rad = Math.toRadians(angle.toDouble())
        return Offset(center.x + radius * cos(rad).toFloat(), center.y + radius * sin(rad).toFloat())
    }
    drawCircle(FullnessColor, stroke * 0.9f, pointAt(leftStart - 8f))
    drawCircle(EnergyColor, stroke * 0.9f, pointAt(rightStart + sweep + 8f))
}
