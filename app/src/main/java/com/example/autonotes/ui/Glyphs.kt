package com.example.autonotes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun PlayPauseGlyph(isPlaying: Boolean, color: Color, modifier: Modifier = Modifier.size(28.dp)) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (isPlaying) {
            val barW = w * 0.28f
            val radius = CornerRadius(barW * 0.25f)
            drawRoundRect(color, Offset(w * 0.16f, h * 0.12f), Size(barW, h * 0.76f), radius)
            drawRoundRect(color, Offset(w * 0.56f, h * 0.12f), Size(barW, h * 0.76f), radius)
        } else {
            val path = Path().apply {
                moveTo(w * 0.24f, h * 0.10f)
                lineTo(w * 0.90f, h * 0.50f)
                lineTo(w * 0.24f, h * 0.90f)
                close()
            }
            drawPath(path, color)
        }
    }
}

@Composable
fun StopGlyph(color: Color, modifier: Modifier = Modifier.size(16.dp)) {
    Canvas(modifier) {
        drawRoundRect(color, cornerRadius = CornerRadius(size.width * 0.2f))
    }
}

@Composable
fun RecordGlyph(color: Color, modifier: Modifier = Modifier.size(14.dp)) {
    Canvas(modifier) { drawCircle(color) }
}