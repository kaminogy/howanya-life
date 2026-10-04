package com.kamino.howanyalife.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import com.kamino.howanyalife.model.CatAction

/** Number of frames the placeholder cycles through for every action. */
const val PLACEHOLDER_FRAMES = 4

private val Fur = Color(0xFFF4A259)
private val Stripe = Color(0xFFD9822B)
private val Belly = Color(0xFFFFE3C2)
private val Ink = Color(0xFF3B2A20)
private val Blush = Color(0xFFE07A8B)
private val Tear = Color(0xFF7EC8F2)

/**
 * A simple cat drawn in code, used until the real character frames are provided.
 * It faces right; the caller mirrors it for left-facing movement.
 */
@Composable
fun PlaceholderCat(action: CatAction, frame: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        when (action) {
            CatAction.SLEEP -> drawSleeping(frame)
            else -> drawAwake(action, frame)
        }
    }
}

private fun DrawScope.drawAwake(action: CatAction, frame: Int) {
    val w = size.width
    val h = size.height
    val bounce = when (action) {
        CatAction.RUN -> if (frame % 2 == 0) -0.06f * h else 0f
        CatAction.WALK -> if (frame % 2 == 0) -0.02f * h else 0f
        CatAction.LAUGH -> if (frame % 2 == 0) -0.04f * h else 0f
        else -> 0f
    }
    val stretching = action == CatAction.STRETCH && frame in 1..2

    translate(top = bounce) {
        // Tail, swaying with the frame.
        val sway = listOf(0f, 0.04f, 0f, -0.04f)[frame % 4] * w
        val tail = Path().apply {
            moveTo(0.22f * w, 0.66f * h)
            quadraticTo(0.02f * w, 0.62f * h, 0.08f * w + sway, 0.36f * h)
        }
        drawPath(tail, Fur, style = Stroke(width = 0.07f * w, cap = StrokeCap.Round))

        // Legs: front and back pairs alternate while moving.
        val stride = when (action) {
            CatAction.RUN -> 0.07f * w
            CatAction.WALK -> 0.04f * w
            else -> 0f
        }
        val swing = if (frame % 2 == 0) stride else -stride
        val legTop = 0.72f * h
        val legBottom = 0.92f * h
        val frontLegY = if (stretching) 0.86f * h else legTop
        drawLeg(0.30f * w + swing, legTop, legBottom)
        drawLeg(0.40f * w - swing, legTop, legBottom)
        drawLeg(0.58f * w - swing, frontLegY, legBottom, front = stretching)
        drawLeg(0.68f * w + swing, frontLegY, legBottom, front = stretching)

        // Body: longer and tilted forward while stretching.
        val bodyTop = if (stretching) 0.56f * h else 0.50f * h
        val bodyWidth = if (stretching) 0.62f * w else 0.54f * w
        drawOval(Fur, topLeft = Offset(0.2f * w, bodyTop), size = Size(bodyWidth, 0.28f * h))
        drawOval(Belly, topLeft = Offset(0.32f * w, bodyTop + 0.13f * h), size = Size(0.30f * w, 0.13f * h))
        for (i in 0..2) {
            val x = 0.30f * w + i * 0.09f * w
            drawLine(Stripe, Offset(x, bodyTop + 0.01f * h), Offset(x + 0.02f * w, bodyTop + 0.08f * h), 0.025f * w, StrokeCap.Round)
        }

        // Head: lowered while eating or stretching.
        val headCenter = when {
            action == CatAction.EAT -> Offset(0.80f * w, 0.62f * h + if (frame % 2 == 0) 0.03f * h else 0f)
            stretching -> Offset(0.82f * w, 0.60f * h)
            else -> Offset(0.76f * w, 0.42f * h)
        }
        drawHead(headCenter, 0.19f * w, action, frame)
    }
}

private fun DrawScope.drawSleeping(frame: Int) {
    val w = size.width
    val h = size.height
    // Slow breathing.
    val breath = if (frame % 2 == 0) 0f else 0.015f * h

    val tail = Path().apply {
        moveTo(0.2f * w, 0.8f * h)
        quadraticTo(0.35f * w, 0.98f * h, 0.7f * w, 0.9f * h)
    }
    drawPath(tail, Fur, style = Stroke(width = 0.07f * w, cap = StrokeCap.Round))
    drawOval(Fur, topLeft = Offset(0.14f * w, 0.56f * h - breath), size = Size(0.62f * w, 0.34f * h + breath))
    for (i in 0..2) {
        val x = 0.28f * w + i * 0.1f * w
        drawLine(Stripe, Offset(x, 0.58f * h - breath), Offset(x + 0.02f * w, 0.66f * h), 0.025f * w, StrokeCap.Round)
    }
    drawHead(Offset(0.72f * w, 0.72f * h), 0.17f * w, CatAction.SLEEP, frame)

    // Floating "z"s that drift upward.
    val zs = listOf(Offset(0.80f * w, 0.42f * h), Offset(0.88f * w, 0.26f * h))
    zs.forEachIndexed { i, base ->
        val rise = ((frame + i) % PLACEHOLDER_FRAMES) * 0.025f * h
        drawZ(base.copy(y = base.y - rise), 0.07f * w * (1f - i * 0.25f))
    }
}

private fun DrawScope.drawLeg(x: Float, top: Float, bottom: Float, front: Boolean = false) {
    val width = size.width * 0.07f
    drawLine(Fur, Offset(x, top), Offset(if (front) x + width else x, bottom), width, StrokeCap.Round)
}

private fun DrawScope.drawHead(center: Offset, r: Float, action: CatAction, frame: Int) {
    // Ears.
    for (dir in listOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(center.x + dir * 0.85f * r, center.y - 0.35f * r)
            lineTo(center.x + dir * 0.7f * r, center.y - 1.25f * r)
            lineTo(center.x + dir * 0.15f * r, center.y - 0.8f * r)
            close()
        }
        drawPath(ear, Fur)
    }
    drawCircle(Fur, r, center)
    drawLine(Stripe, Offset(center.x, center.y - r), Offset(center.x, center.y - 0.6f * r), 0.12f * r, StrokeCap.Round)

    val eyeY = center.y - 0.1f * r
    val eyeDx = 0.42f * r
    val eyeStroke = Stroke(width = 0.12f * r, cap = StrokeCap.Round)
    for (dir in listOf(-1f, 1f)) {
        val eye = Offset(center.x + dir * eyeDx, eyeY)
        when (action) {
            CatAction.SLEEP, CatAction.EAT -> drawArc(
                Ink, 0f, 180f, false,
                topLeft = Offset(eye.x - 0.18f * r, eye.y - 0.1f * r), size = Size(0.36f * r, 0.2f * r), style = eyeStroke,
            )
            CatAction.LAUGH, CatAction.STRETCH -> drawArc(
                Ink, 180f, 180f, false,
                topLeft = Offset(eye.x - 0.18f * r, eye.y - 0.05f * r), size = Size(0.36f * r, 0.3f * r), style = eyeStroke,
            )
            else -> drawCircle(Ink, 0.11f * r, eye)
        }
        drawCircle(Blush.copy(alpha = 0.5f), 0.13f * r, Offset(eye.x + dir * 0.12f * r, center.y + 0.3f * r))
    }

    // Nose and mouth.
    drawCircle(Blush, 0.08f * r, Offset(center.x, center.y + 0.15f * r))
    val mouthTop = center.y + 0.3f * r
    when (action) {
        CatAction.LAUGH -> drawArc(
            Blush, 0f, 180f, true,
            topLeft = Offset(center.x - 0.22f * r, mouthTop - 0.1f * r), size = Size(0.44f * r, 0.4f * r),
        )
        CatAction.CRY -> drawArc(
            Ink, 200f, 140f, false,
            topLeft = Offset(center.x - 0.18f * r, mouthTop + 0.05f * r), size = Size(0.36f * r, 0.3f * r), style = eyeStroke,
        )
        else -> drawArc(
            Ink, 20f, 140f, false,
            topLeft = Offset(center.x - 0.2f * r, mouthTop - 0.12f * r), size = Size(0.4f * r, 0.2f * r), style = eyeStroke,
        )
    }

    if (action == CatAction.CRY) {
        // Tears that fall a little further each frame.
        val fall = (frame % PLACEHOLDER_FRAMES) * 0.18f * r
        for (dir in listOf(-1f, 1f)) {
            drawCircle(Tear, 0.1f * r, Offset(center.x + dir * eyeDx, eyeY + 0.25f * r + fall))
        }
    }
}

private fun DrawScope.drawZ(topLeft: Offset, s: Float) {
    val z = Path().apply {
        moveTo(topLeft.x, topLeft.y)
        lineTo(topLeft.x + s, topLeft.y)
        lineTo(topLeft.x, topLeft.y + s)
        lineTo(topLeft.x + s, topLeft.y + s)
    }
    drawPath(z, Color.White.copy(alpha = 0.85f), style = Stroke(width = s * 0.18f, cap = StrokeCap.Round))
}
