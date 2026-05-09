package com.quicksettle.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private sealed class PadKey {
    data class Digit(val value: Char) : PadKey()
    data object Decimal : PadKey()
    data object Backspace : PadKey()
}

private val padRows: List<List<PadKey>> = listOf(
    listOf(PadKey.Digit('1'), PadKey.Digit('2'), PadKey.Digit('3')),
    listOf(PadKey.Digit('4'), PadKey.Digit('5'), PadKey.Digit('6')),
    listOf(PadKey.Digit('7'), PadKey.Digit('8'), PadKey.Digit('9')),
    listOf(PadKey.Decimal, PadKey.Digit('0'), PadKey.Backspace),
)

/**
 * Custom number pad -- 4x3 grid using Column+Row (NOT LazyVerticalGrid)
 * so it can safely live inside a vertically scrollable parent.
 */
@Composable
fun NumberPad(
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    onConfirm: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        padRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    NumberPadKey(
                        padKey = key,
                        onClick = {
                            when (key) {
                                is PadKey.Digit -> onDigit(key.value)
                                is PadKey.Decimal -> onDecimal()
                                is PadKey.Backspace -> onBackspace()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberPadKey(
    padKey: PadKey,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "key-scale",
    )
    val backgroundColor = if (pressed) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val semanticDesc = when (padKey) {
        is PadKey.Digit -> padKey.value.toString()
        is PadKey.Decimal -> "decimal point"
        is PadKey.Backspace -> "backspace"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(ratio = 1.4f)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                        onClick()
                    },
                )
            }
            .semantics {
                contentDescription = semanticDesc
                role = Role.Button
            },
    ) {
        when (padKey) {
            is PadKey.Digit -> {
                Text(
                    text = padKey.value.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            is PadKey.Decimal -> {
                Text(
                    text = ".",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            is PadKey.Backspace -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
