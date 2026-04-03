package com.quicksettle.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.quicksettle.presentation.theme.OnPrimary
import com.quicksettle.presentation.theme.Primary
import com.quicksettle.presentation.theme.PrimaryContainer

/**
 * Gradient CTA button following the Sovereign Ledger design system.
 *
 * - Gradient: primary (#012d1d) → primary_container (#1b4332) at 45°
 * - xl roundedness (12dp corner radius)
 * - on_primary white text + optional leading icon
 * - Disabled at alpha 0.5 when [enabled] is false
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    // 45-degree gradient (bottom-left → top-right approximation using linearGradient)
    val gradientBrush = Brush.linearGradient(
        colors = listOf(Primary, PrimaryContainer),
        start = Offset(0f, Float.POSITIVE_INFINITY),
        end = Offset(Float.POSITIVE_INFINITY, 0f),
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = gradientBrush,
                alpha = if (enabled) 1f else 0.5f,
            ),
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = OnPrimary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = OnPrimary.copy(alpha = 0.7f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (enabled) OnPrimary else OnPrimary.copy(alpha = 0.7f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) OnPrimary else OnPrimary.copy(alpha = 0.7f),
                )
            }
        }
    }
}
