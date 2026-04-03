package com.quicksettle.presentation.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Ledger-style text field following the Sovereign Ledger design system:
 * - surface_container_low background
 * - NO box borders
 * - 3dp bottom indicator in primary when focused; transparent when unfocused
 */
@Composable
fun LedgerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.outlineVariant,
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = colorScheme.onSurface,
        ),
        singleLine = singleLine,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colorScheme.surfaceContainerLow,
            unfocusedContainerColor = colorScheme.surfaceContainerLow,
            disabledContainerColor = colorScheme.surfaceContainerLow,
            focusedIndicatorColor = colorScheme.primary,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = colorScheme.onSurface,
            unfocusedTextColor = colorScheme.onSurface,
            cursorColor = colorScheme.primary,
        ),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
    )
}
