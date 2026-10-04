package com.kamino.howanyalife.mobile.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Where the wall meets the floor, as a fraction of the room height. */
private const val FLOOR_TOP = 0.6f

private class RoomColors(
    val wall: Color,
    val wallStripe: Color,
    val floor: Color,
    val plank: Color,
    val rug: Color,
    val sky: List<Color>,
    val wood: Color,
    val curtain: Color,
)

private val Day = RoomColors(
    wall = Color(0xFFFFF4D6),
    wallStripe = Color(0xFFFFEAB8),
    floor = Color(0xFFDDB07A),
    plank = Color(0xFFC79A66),
    rug = Color(0xFFF9C6D8),
    sky = listOf(Color(0xFF8FD3F4), Color(0xFFDDF3FF)),
    wood = Color(0xFF9B6B43),
    curtain = Color(0xFFFFD54F),
)

private val Night = RoomColors(
    wall = Color(0xFF3B4068),
    wallStripe = Color(0xFF353A60),
    floor = Color(0xFF6E5A4A),
    plank = Color(0xFF5E4C3F),
    rug = Color(0xFF8C6680),
    sky = listOf(Color(0xFF0E1433), Color(0xFF2B3A67)),
    wood = Color(0xFF6B4A33),
    curtain = Color(0xFFB8963A),
)

/** Side view of Howanya's room: wall with a window and a picture, wooden floor, rug and a plant. */
fun DrawScope.drawRoom(night: Boolean) {
    val c = if (night) Night else Day
    val w = size.width
    val h = size.height
    val floorTop = h * FLOOR_TOP

    // Wall with soft vertical stripes.
    drawRect(c.wall, size = Size(w, floorTop))
    val stripe = w / 10
    for (i in 0 until 10 step 2) {
        drawRect(c.wallStripe, topLeft = Offset(i * stripe, 0f), size = Size(stripe, floorTop))
    }

    drawWindow(c, night, Offset(w * 0.08f, h * 0.08f), Size(w * 0.44f, h * 0.3f))
    drawPicture(c, Offset(w * 0.64f, h * 0.12f), Size(w * 0.24f, h * 0.17f))

    // Floor: planks running left to right.
    drawRect(c.floor, topLeft = Offset(0f, floorTop), size = Size(w, h - floorTop))
    val plankHeight = (h - floorTop) / 5
    for (i in 1 until 5) {
        val y = floorTop + i * plankHeight
        drawLine(c.plank, Offset(0f, y), Offset(w, y), strokeWidth = 2f)
    }
    // Skirting board.
    drawRect(c.wood, topLeft = Offset(0f, floorTop - h * 0.012f), size = Size(w, h * 0.016f))

    drawOval(c.rug, topLeft = Offset(w * 0.08f, h * 0.8f), size = Size(w * 0.84f, h * 0.13f))
    drawPlant(c, Offset(w * 0.82f, floorTop + h * 0.07f), h * 0.17f)
}

private fun DrawScope.drawWindow(c: RoomColors, night: Boolean, topLeft: Offset, size: Size) {
    drawRect(Brush.verticalGradient(c.sky, startY = topLeft.y, endY = topLeft.y + size.height), topLeft, size)
    if (night) {
        val stars = listOf(0.2f to 0.25f, 0.45f to 0.15f, 0.7f to 0.3f, 0.3f to 0.6f, 0.85f to 0.7f, 0.6f to 0.75f)
        stars.forEach { (x, y) ->
            drawCircle(Color.White.copy(alpha = 0.85f), size.minDimension * 0.02f, topLeft + Offset(size.width * x, size.height * y))
        }
        drawCircle(Color(0xFFFFF4C2), size.minDimension * 0.13f, topLeft + Offset(size.width * 0.72f, size.height * 0.4f))
    } else {
        drawCircle(Color(0xFFFFE07A), size.minDimension * 0.14f, topLeft + Offset(size.width * 0.72f, size.height * 0.36f))
        drawCloud(topLeft + Offset(size.width * 0.28f, size.height * 0.62f), size.minDimension * 0.1f)
    }

    // Frame and cross bars.
    val frame = size.minDimension * 0.05f
    drawRect(c.wood, topLeft, size, style = Stroke(frame))
    drawLine(c.wood, topLeft + Offset(size.width / 2, 0f), topLeft + Offset(size.width / 2, size.height), frame * 0.6f)
    drawLine(c.wood, topLeft + Offset(0f, size.height / 2), topLeft + Offset(size.width, size.height / 2), frame * 0.6f)

    // Curtains tied back on both sides.
    for (side in listOf(-1f, 1f)) {
        val x = if (side < 0) topLeft.x else topLeft.x + size.width
        val curtain = Path().apply {
            moveTo(x - side * size.width * 0.02f, topLeft.y - frame)
            lineTo(x + side * size.width * 0.12f, topLeft.y - frame)
            quadraticTo(x - side * size.width * 0.02f, topLeft.y + size.height * 0.5f, x + side * size.width * 0.1f, topLeft.y + size.height + frame)
            lineTo(x - side * size.width * 0.04f, topLeft.y + size.height + frame)
            close()
        }
        drawPath(curtain, c.curtain)
    }
    drawRoundRect(
        c.wood,
        topLeft = Offset(topLeft.x - size.width * 0.06f, topLeft.y - frame * 2),
        size = Size(size.width * 1.12f, frame * 1.4f),
        cornerRadius = CornerRadius(frame),
    )
}

private fun DrawScope.drawCloud(center: Offset, r: Float) {
    val white = Color.White.copy(alpha = 0.9f)
    drawCircle(white, r, center)
    drawCircle(white, r * 0.8f, center + Offset(r * 1.1f, r * 0.15f))
    drawCircle(white, r * 0.75f, center + Offset(-r * 1.05f, r * 0.2f))
}

/** A framed picture of Howanya's favorite food, a donut. */
private fun DrawScope.drawPicture(c: RoomColors, topLeft: Offset, size: Size) {
    drawRect(Color(0xFFDFF4FF), topLeft, size)
    drawRect(c.wood, topLeft, size, style = Stroke(size.minDimension * 0.07f))
    val center = topLeft + Offset(size.width / 2, size.height / 2)
    val r = size.minDimension * 0.28f
    drawCircle(Color(0xFFD9A066), r, center, style = Stroke(r * 0.75f))
    drawArc(
        Color(0xFFF7A8C4), 180f, 200f, false,
        topLeft = center - Offset(r, r), size = Size(r * 2, r * 2),
        style = Stroke(r * 0.55f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawPlant(c: RoomColors, base: Offset, height: Float) {
    val leaf = if (c === Night) Color(0xFF4F7A55) else Color(0xFF7CC47F)
    val potW = height * 0.45f
    val potH = height * 0.35f
    for ((angle, len) in listOf(-35f to 0.75f, -10f to 0.95f, 15f to 0.85f, 40f to 0.7f)) {
        val rad = Math.toRadians(angle.toDouble() - 90)
        val tip = base + Offset(
            (kotlin.math.cos(rad) * height * len).toFloat(),
            (kotlin.math.sin(rad) * height * len).toFloat() - potH,
        )
        drawLine(leaf, base - Offset(0f, potH), tip, strokeWidth = height * 0.12f, cap = StrokeCap.Round)
    }
    val pot = Path().apply {
        moveTo(base.x - potW / 2, base.y - potH)
        lineTo(base.x + potW / 2, base.y - potH)
        lineTo(base.x + potW * 0.38f, base.y)
        lineTo(base.x - potW * 0.38f, base.y)
        close()
    }
    drawPath(pot, Color(0xFFD9774B))
}

fun DrawScope.drawBowl(full: Boolean) {
    val w = size.width
    val h = size.height
    if (full) {
        drawOval(Color(0xFF8B5A2B), topLeft = Offset(0.15f * w, 0.25f * h), size = Size(0.7f * w, 0.35f * h))
    }
    drawArc(Color(0xFF4F7CAC), 0f, 180f, true, topLeft = Offset(0.05f * w, 0.05f * h), size = Size(0.9f * w, 0.85f * h))
}
