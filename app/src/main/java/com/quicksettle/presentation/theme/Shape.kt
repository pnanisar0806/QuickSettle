package com.quicksettle.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape scale — minimum 6 dp radius on every element, as mandated by the
 * Sovereign Ledger design system ("NO sharp corners").
 */
val SovereignLedgerShapes = Shapes(
    // extraSmall: e.g. tooltip, snackbar
    extraSmall = RoundedCornerShape(6.dp),
    // small: e.g. chips, small cards
    small = RoundedCornerShape(8.dp),
    // medium: e.g. input fields, list tiles
    medium = RoundedCornerShape(12.dp),
    // large: e.g. bottom sheets, dialog surfaces
    large = RoundedCornerShape(16.dp),
    // extraLarge: e.g. primary CTA buttons, number-pad keys
    extraLarge = RoundedCornerShape(24.dp),
)
