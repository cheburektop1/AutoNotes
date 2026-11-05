package com.example.autonotes.ui

import androidx.compose.ui.graphics.Color
import com.example.autonotes.data.db.MarkerType

fun MarkerType.label(): String = when (this) {
    MarkerType.IMPORTANT -> "Важно"
    MarkerType.DEFINITION -> "Определение"
    MarkerType.EXAMPLE -> "Пример"
    MarkerType.HOMEWORK -> "Домашнее задание"
}

fun MarkerType.shortLabel(): String = when (this) {
    MarkerType.IMPORTANT -> "!"
    MarkerType.DEFINITION -> "О"
    MarkerType.EXAMPLE -> "П"
    MarkerType.HOMEWORK -> "ДЗ"
}

fun MarkerType.color(): Color = when (this) {
    MarkerType.IMPORTANT -> Color(0xFFB91C1C)
    MarkerType.DEFINITION -> Color(0xFF1D4ED8)
    MarkerType.EXAMPLE -> Color(0xFF15803D)
    MarkerType.HOMEWORK -> Color(0xFFB45309)
}