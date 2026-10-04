package com.kamino.howanyalife.mobile.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.kamino.howanyalife.data.CatRepository
import com.kamino.howanyalife.data.catTime
import com.kamino.howanyalife.mobile.MainActivity
import com.kamino.howanyalife.mobile.R
import com.kamino.howanyalife.mobile.ui.FullnessColor
import com.kamino.howanyalife.mobile.ui.HowanyaBlue
import com.kamino.howanyalife.model.CatAction
import com.kamino.howanyalife.model.CatState
import com.kamino.howanyalife.ui.SpriteStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Home-screen widget: shows how Howanya is doing right now with her fullness, and has a
 * button to feed her without opening the app. Tapping the picture opens the app.
 */
class HowanyaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = CatRepository(context)
        val initial = repository.load()
        val pictures = withContext(Dispatchers.IO) {
            val store = SpriteStore(context)
            STATUS_TEXT.keys.associateWith { store.loadFirstFrame(it) }
        }
        val status = STATUS_TEXT.mapValues { (_, text) -> context.getString(text) }
        val feedLabel = context.getString(R.string.widget_feed)
        val catDescription = context.getString(R.string.cat_description)

        provideContent {
            // Read inside the content so the widget redraws whenever the saved state changes:
            // update() on a running Glance session only recomposes, it does not call provideGlance again.
            val saved by repository.observe().collectAsState(initial)
            val state = saved?.let { it.state.advance(catTime(System.currentTimeMillis() - it.savedAtMillis)) }
                ?: CatState()
            val action = widgetAction(state)
            WidgetContent(state, pictures[action], status.getValue(action), feedLabel, catDescription)
        }
    }

    private companion object {
        /** The poses the widget can show, with the caption for each. */
        val STATUS_TEXT = mapOf(
            CatAction.SLEEP to R.string.state_sleeping,
            CatAction.CRY to R.string.state_hungry,
            CatAction.LAUGH to R.string.state_happy,
            CatAction.IDLE to R.string.state_normal,
        )

        fun widgetAction(state: CatState): CatAction = when {
            state.asleep -> CatAction.SLEEP
            state.isHungry -> CatAction.CRY
            state.mood >= 70f -> CatAction.LAUGH
            else -> CatAction.IDLE
        }
    }
}

private val Background = fixed(Color(0xFFFFF7CC))
private val TextColor = fixed(Color(0xFF3B3200))

/** The same color in light and dark mode. */
private fun fixed(color: Color) = ColorProvider(day = color, night = color)

@Composable
private fun WidgetContent(
    state: CatState,
    picture: Bitmap?,
    status: String,
    feedLabel: String,
    catDescription: String,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Background)
            .cornerRadius(20.dp)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (picture != null) {
            Image(
                provider = ImageProvider(picture),
                contentDescription = catDescription,
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxWidth()
                    .clickable(actionStartActivity<MainActivity>()),
            )
        }
        Text(
            status,
            style = TextStyle(color = TextColor, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(GlanceModifier.height(4.dp))
        LinearProgressIndicator(
            progress = state.fullness / 100f,
            color = fixed(FullnessColor),
            backgroundColor = fixed(FullnessColor.copy(alpha = 0.25f)),
            modifier = GlanceModifier.fillMaxWidth().height(6.dp),
        )
        Spacer(GlanceModifier.height(6.dp))
        Button(
            text = feedLabel,
            onClick = actionRunCallback<FeedAction>(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = fixed(HowanyaBlue),
                contentColor = fixed(Color.White),
            ),
            modifier = GlanceModifier.fillMaxWidth(),
        )
    }
}

/** Feeds Howanya from the widget and redraws it. */
class FeedAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        CatRepository(context).feed()
        HowanyaWidget().update(context, glanceId)
    }
}

class HowanyaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HowanyaWidget()
}
