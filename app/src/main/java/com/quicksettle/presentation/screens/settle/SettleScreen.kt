package com.quicksettle.presentation.screens.settle

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.East
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quicksettle.presentation.components.GradientButton
import com.quicksettle.presentation.components.QrCodeImage
import com.quicksettle.presentation.screens.crew.FriendAvatar
import com.quicksettle.presentation.theme.ManropeBold
import com.quicksettle.presentation.theme.ManropeExtraBold
import com.quicksettle.presentation.util.AmountFormatter

/**
 * Settlement List screen — Settle tab.
 *
 * Shows "Total to Collect" header, active-debts badge,
 * settlement cards with Show QR / WhatsApp buttons, and
 * "Settle All Balances" CTA.
 */
@Composable
fun SettleScreen(
    modifier: Modifier = Modifier,
    viewModel: SettleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            // ── Total to Collect Header ──────────────────────────────────────
            item {
                TotalToCollectHeader(
                    totalAmount = uiState.totalToCollect,
                    activeDebtsCount = uiState.activeDebtsCount,
                )
            }

            // ── Settlement Cards ─────────────────────────────────────────────
            itemsIndexed(
                items = uiState.settlements,
                key = { index, item -> "${item.participantName}_$index" },
            ) { index, item ->
                SettlementCard(
                    item = item,
                    onToggleQr = { viewModel.toggleQr(index) },
                    onMarkPaid = { viewModel.markPaid(index) },
                    onShareWhatsApp = { message ->
                        shareWhatsApp(context = context, message = message)
                    },
                )
            }
        }

        // ── Glassmorphism CTA bar ────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            GradientButton(
                text = "SETTLE ALL BALANCES",
                onClick = {
                    val message = viewModel.settleAllMessage()
                    if (message.isNotBlank()) {
                        shareGeneric(context = context, message = message)
                    }
                },
                icon = Icons.Filled.East,
                enabled = uiState.activeDebtsCount > 0,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Total to Collect Header ──────────────────────────────────────────────────

@Composable
private fun TotalToCollectHeader(
    totalAmount: Double,
    activeDebtsCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "TOTAL TO COLLECT",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = ManropeBold,
                letterSpacing = 2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "\u20B9${AmountFormatter.format(totalAmount)}",
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = ManropeExtraBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Active debts pill badge
        val debtLabel = if (activeDebtsCount == 1) "DEBT" else "DEBTS"
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                text = "\u26A1 $activeDebtsCount ACTIVE $debtLabel",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }
}

// ── Settlement Card ──────────────────────────────────────────────────────────

@Composable
private fun SettlementCard(
    item: SettlementItem,
    onToggleQr: () -> Unit,
    onMarkPaid: () -> Unit,
    onShareWhatsApp: (String) -> Unit,
) {
    val isPaid = item.status == SettlementStatus.PAID

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0F002114),
                spotColor = Color(0x0F002114),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(16.dp),
    ) {
        // ── Top row: avatar + info + amount ──────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            FriendAvatar(
                name = item.participantName,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Middle: name + description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.participantName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = ManropeBold,
                        fontSize = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: amount + status
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "\u20B9${AmountFormatter.format(item.amount)}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = ManropeBold,
                        fontSize = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.status.displayText(item.pendingDays),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (item.status == SettlementStatus.DUE_TODAY) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                    ),
                    color = when (item.status) {
                        SettlementStatus.URGENT -> MaterialTheme.colorScheme.error
                        SettlementStatus.PAID -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Action buttons ───────────────────────────────────────────────
        if (!isPaid) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Show QR button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleQr() },
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (item.showQr) "Hide QR" else "Show QR",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                // WhatsApp button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onShareWhatsApp(item.shareMessage) },
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WhatsApp",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        } else {
            // Paid: settled indicator
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 10.dp),
                ) {
                    Text(
                        text = "Settled",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        // ── QR code section (animated) ───────────────────────────────────
        AnimatedVisibility(
            visible = item.showQr && !isPaid,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (item.upiUri.isNotBlank()) {
                    QrCodeImage(
                        content = item.upiUri,
                        sizeDp = 250.dp,
                    )
                } else {
                    Text(
                        text = "Set up your UPI ID in profile to generate QR codes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ── Mark paid ────────────────────────────────────────────────────
        if (!isPaid) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to mark settled",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onMarkPaid() },
            )
        }
    }
}

// ── Share helpers ─────────────────────────────────────────────────────────────

private fun shareWhatsApp(context: Context, message: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        shareGeneric(context = context, message = message)
    }
}

private fun shareGeneric(context: Context, message: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(
        Intent.createChooser(intent, "Share settlement"),
    )
}
