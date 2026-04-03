package com.quicksettle.presentation.screens.settle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Settlement List screen — Settle tab.
 *
 * Placeholder: will host "Total to Collect" header, active-debts badge,
 * settlement cards with Show QR / WhatsApp buttons, and
 * "Settle All Balances" CTA.
 */
@Composable
fun SettleScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Text(
            text = "Settlement List",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
