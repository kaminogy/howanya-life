package com.kamino.howanyalife.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kamino.howanyalife.mobile.R
import com.kamino.howanyalife.data.SoundMode
import com.kamino.howanyalife.model.CatState
import com.kamino.howanyalife.ui.CatSounds
import com.kamino.howanyalife.ui.CatSprite
import com.kamino.howanyalife.ui.CatViewModel
import com.kamino.howanyalife.ui.icon
import com.kamino.howanyalife.ui.rememberSoundSettings
import kotlinx.coroutines.launch

/** Where the cat's paws touch the floor, as a fraction of the room height. */
private const val GROUND_Y = 0.86f
/** How far the cat's center may move from the room center, as a fraction of the room width. */
private const val WALK_RANGE = 0.32f

@Composable
fun RoomScreen(viewModel: CatViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val sprites by viewModel.sprites.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }

    val soundSettings = rememberSoundSettings(default = SoundMode.ALL)
    val soundMode by soundSettings.mode.collectAsStateWithLifecycle(initialValue = null)
    var petted by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    CatSounds(ui, soundMode, petted)

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            StatusPanel(
                stats = ui.stats,
                soundMode = soundMode,
                onSoundClick = { soundMode?.let { scope.launch { soundSettings.set(it.next()) } } },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp)),
            ) {
                val roomWidth = maxWidth
                val roomHeight = maxHeight
                val groundY = roomHeight * GROUND_Y
                Canvas(Modifier.fillMaxSize()) { drawRoom(ui.night) }

                val catSize = min(roomWidth * 0.5f, roomHeight * 0.42f)
                val catDescription = stringResource(R.string.cat_description)
                CatSprite(
                    action = ui.action,
                    frames = sprites.frames[ui.action],
                    facingRight = ui.facingRight,
                    mirrorWhenFacingLeft = sprites.mirrorWhenFacingLeft,
                    modifier = Modifier
                        .offset(x = floorX(roomWidth, ui.x) - catSize / 2, y = groundY - catSize * 0.92f)
                        .size(catSize)
                        .semantics { contentDescription = catDescription }
                        .clickable(interactionSource = null, indication = null) {
                            petted++
                            viewModel.pet()
                        },
                )

                // Drawn after the cat so the food stays visible while it eats.
                ui.bowlX?.let { bowlX ->
                    val bowlSize = catSize * 0.4f
                    Canvas(
                        Modifier
                            .offset(x = floorX(roomWidth, bowlX) - bowlSize / 2, y = groundY - bowlSize * 0.9f)
                            .size(bowlSize),
                    ) { drawBowl(ui.bowlFull) }
                }
            }

            Button(
                onClick = viewModel::feed,
                enabled = ui.loaded && ui.bowlX == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
            ) {
                Text(stringResource(R.string.feed), style = MaterialTheme.typography.titleMedium)
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

private fun floorX(roomWidth: Dp, x: Float): Dp = roomWidth / 2 + roomWidth * WALK_RANGE * x

@Composable
private fun StatusPanel(
    stats: CatState,
    soundMode: SoundMode?,
    onSoundClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.screen_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                if (soundMode != null) {
                    TextButton(onClick = onSoundClick) {
                        Text("${soundMode.icon} ${stringResource(soundMode.label)}")
                    }
                }
            }
            StatusRow(stringResource(R.string.fullness), stats.fullness, FullnessColor)
            StatusRow(stringResource(R.string.energy), stats.energy, EnergyColor)
            StatusRow(stringResource(R.string.mood), stats.mood, MoodColor)
        }
    }
}

@Composable
private fun StatusRow(label: String, value: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(56.dp))
        LinearProgressIndicator(
            progress = { value / 100f },
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .weight(1f)
                .height(10.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "${value.toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(40.dp),
        )
    }
}
