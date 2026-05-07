package com.quicksettle.presentation.screens.crew

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.East
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quicksettle.domain.model.Friend
import com.quicksettle.presentation.components.GradientButton
import com.quicksettle.presentation.util.AmountFormatter
import com.quicksettle.presentation.theme.ManropeBold
import com.quicksettle.presentation.theme.ManropeExtraBold
import com.quicksettle.presentation.theme.OnPrimary
import com.quicksettle.presentation.theme.Primary
import com.quicksettle.presentation.theme.PrimaryContainer
import com.quicksettle.presentation.theme.PrimaryFixed

/**
 * Select Friends screen — Friends tab.
 *
 * Hosts the total-bill header, Equal/Unequal toggle,
 * frequent-friends list with selection, suggestions section,
 * and a glassmorphism "Go to Settle" CTA.
 */
@Composable
fun CrewScreen(
    onAddFriend: () -> Unit,
    onNavigateToSettle: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val (amounts, ownerAmt) = viewModel.currentSplit()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp), // room for glassmorphism bar
        ) {
            // ── Header ──────────────────────────────────────────────────────────
            item {
                CrewHeader(
                    totalAmount = uiState.totalAmount,
                    description = uiState.description,
                    onAddFriend = onAddFriend,
                )
            }

            // ── Split Mode Toggle ────────────────────────────────────────────────
            item {
                SplitModeToggle(
                    splitMode = uiState.splitMode,
                    onToggle = viewModel::toggleSplitMode,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }

            // ── "Paid for others" toggle (both modes) ───────────────────────────
            item {
                PaidForOthersToggle(
                    includeSelf = uiState.includeSelfInSplit,
                    onToggle = viewModel::toggleIncludeSelf,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }

            // ── Shares mode: editable "You" row (only when included in split) ──
            if (uiState.splitMode == SplitMode.SHARES &&
                uiState.includeSelfInSplit &&
                uiState.selectedFriends.isNotEmpty()
            ) {
                item {
                    OwnerCard(
                        selfAmount = ownerAmt,
                        ownerShares = uiState.ownerShares,
                        onOwnerSharesChange = viewModel::setOwnerShares,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
            }

            // ── Equal mode: "Your share" info ────────────────────────────────────
            if (uiState.splitMode == SplitMode.EQUAL && uiState.selectedFriends.isNotEmpty()) {
                item {
                    val yourShare = ownerAmt
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryFixed,
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .fillMaxWidth(),
                    ) {
                        Text(
                            text = if (uiState.includeSelfInSplit) "Your share: ₹${formatAmount(yourShare)}"
                            else "You pay ₹0 — splitting among friends only",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = ManropeBold,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
            }

            // ── Frequent Friends section ─────────────────────────────────────────
            if (uiState.frequentFriends.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Frequent Friends",
                        trailing = "${uiState.selectedFriends.size} Selected",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
                items(
                    items = uiState.frequentFriends,
                    key = { it.id },
                ) { friend ->
                    val isSelected = uiState.selectedFriends.any { it.friend.id == friend.id }
                    val friendAmount = if (isSelected) amounts[friend.id] ?: 0.0 else null
                    FriendCard(
                        friend = friend,
                        isSelected = isSelected,
                        amount = friendAmount,
                        splitMode = uiState.splitMode,
                        shares = uiState.selectedFriends
                            .find { it.friend.id == friend.id }?.shares ?: 0,
                        onSharesChange = { viewModel.setShares(friend.id, it) },
                        onClick = { viewModel.toggleFriend(friend.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
            }

            // ── Suggestions section ──────────────────────────────────────────────
            val suggestions = uiState.suggestions
            if (suggestions.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Suggestions",
                        trailing = null,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
                items(
                    items = suggestions,
                    key = { "suggestion_${it.id}" },
                ) { friend ->
                    SuggestionCard(
                        friend = friend,
                        onClick = { viewModel.toggleFriend(friend.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
            }

            // Spacer at the bottom before the CTA bar
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // ── Glassmorphism "Go to Settle" CTA ────────────────────────────────────
        GlassmorphismCtaBar(
            enabled = uiState.canProceed,
            onGoToSettle = onNavigateToSettle,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun CrewHeader(
    totalAmount: Double,
    description: String,
    onAddFriend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "TOTAL GROUP BILL",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 2.sp,
                    fontFamily = ManropeBold,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "₹${formatAmount(totalAmount)}",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = ManropeExtraBold,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Add Friend button — gradient bg, rounded-full, person_add icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Primary, PrimaryContainer),
                        start = Offset(0f, Float.POSITIVE_INFINITY),
                        end = Offset(Float.POSITIVE_INFINITY, 0f),
                    )
                )
                .clickable(onClick = onAddFriend)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = "Add Friend",
                    tint = OnPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Add Friend",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnPrimary,
                )
            }
        }
    }
}

// ── Split Mode Toggle ─────────────────────────────────────────────────────────

@Composable
private fun SplitModeToggle(
    splitMode: SplitMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
        ) {
            // Equal
            ToggleOption(
                label = "Equal",
                icon = {
                    Text(
                        text = "=",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (splitMode == SplitMode.EQUAL)
                            MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                selected = splitMode == SplitMode.EQUAL,
                onClick = { if (splitMode != SplitMode.EQUAL) onToggle() },
                modifier = Modifier.weight(1f),
            )

            // Shares
            ToggleOption(
                label = "Shares",
                icon = {
                    Icon(
                        imageVector = Icons.Filled.PieChart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (splitMode == SplitMode.SHARES)
                            MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                selected = splitMode == SplitMode.SHARES,
                onClick = { if (splitMode != SplitMode.SHARES) onToggle() },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ToggleOption(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surfaceContainerLowest
        else Color.Transparent,
        animationSpec = tween(200),
        label = "toggleBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "toggleText",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(
                if (selected) Modifier.shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = Color(0x0F002114),
                    spotColor = Color(0x0F002114),
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = ManropeBold,
                    fontWeight = FontWeight.Bold,
                ),
                color = textColor,
            )
        }
    }
}

// ── "Paid for Others" Toggle ─────────────────────────────────────────────────

@Composable
private fun PaidForOthersToggle(
    includeSelf: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (!includeSelf) PrimaryFixed else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (!includeSelf) Primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "I paid for others",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = ManropeBold,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = if (!includeSelf) Primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            // Toggle indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .border(
                        width = 2.dp,
                        color = if (!includeSelf) Color(0xFF2C694E)
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    )
                    .background(
                        color = if (!includeSelf) Color(0xFF2C694E)
                        else Color.Transparent,
                        shape = CircleShape,
                    ),
            ) {
                if (!includeSelf) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Paid for others",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ── Owner "You" Row (Shares Mode — editable share count, live amount) ───────

@Composable
private fun OwnerCard(
    selfAmount: Double,
    ownerShares: Int,
    onOwnerSharesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0F002114),
                spotColor = Color(0x0F002114),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left pill — always active for "You"
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(
                        color = PrimaryFixed,
                        shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                    ),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryFixed),
            ) {
                Text(
                    text = "Y",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ManropeBold,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Primary,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "You",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ManropeBold,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "₹${formatAmount(selfAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Editable share count
            SharesField(
                shares = ownerShares,
                onSharesChange = onOwnerSharesChange,
            )
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    trailing: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = ManropeBold,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Friend Card ───────────────────────────────────────────────────────────────

@Composable
private fun FriendCard(
    friend: Friend,
    isSelected: Boolean,
    amount: Double?,
    splitMode: SplitMode,
    shares: Int,
    onSharesChange: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0F002114),
                spotColor = Color(0x0F002114),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 4dp primary_fixed left pill indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(
                        color = if (isSelected) PrimaryFixed else Color.Transparent,
                        shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                    ),
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar placeholder
            FriendAvatar(
                name = friend.name,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Name + UPI handle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ManropeBold,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (friend.upiId != null) {
                    Text(
                        text = "@${friend.upiId.substringBefore('@')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right side: in SHARES mode show shares input + live amount; in EQUAL mode show amount only
            Column(horizontalAlignment = Alignment.End) {
                if (isSelected && splitMode == SplitMode.SHARES) {
                    SharesField(
                        shares = shares,
                        onSharesChange = onSharesChange,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${formatAmount(amount ?: 0.0)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = ManropeBold,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (isSelected && amount != null) {
                    Text(
                        text = "₹${formatAmount(amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = ManropeBold,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isSelected) {
                    // Green checkmark
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Primary),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = OnPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                } else {
                    // Empty circle indicator
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

// ── Suggestion Card ───────────────────────────────────────────────────────────

@Composable
private fun SuggestionCard(
    friend: Friend,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0F002114),
                spotColor = Color(0x0F002114),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // No selection pill — use transparent space to match padding
            Spacer(modifier = Modifier.width(4.dp))

            Spacer(modifier = Modifier.width(12.dp))

            FriendAvatar(
                name = friend.name,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ManropeBold,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Show masked phone hint or UPI
                val hint = if (friend.upiId != null) {
                    "@${friend.upiId.substringBefore('@')}"
                } else {
                    "Tap to add"
                }
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // "+" add circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = PrimaryFixed,
                        shape = CircleShape,
                    ),
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ManropeBold,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Primary,
                )
            }
        }
    }
}

// ── Avatar placeholder ────────────────────────────────────────────────────────

@Composable
fun FriendAvatar(
    name: String,
    modifier: Modifier = Modifier,
) {
    val initial = name.firstOrNull()?.uppercaseChar() ?: '?'
    // Derive a consistent hue-shifted color from name hashCode
    val hue = (name.hashCode().and(0xFF) / 255f) * 360f
    val bgColor = Color.hsl(hue = hue, saturation = 0.35f, lightness = 0.88f)
    val textColor = Color.hsl(hue = hue, saturation = 0.55f, lightness = 0.25f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(bgColor),
    ) {
        Text(
            text = initial.toString(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = ManropeBold,
                fontWeight = FontWeight.Bold,
            ),
            color = textColor,
        )
    }
}

// ── Glassmorphism CTA Bar ─────────────────────────────────────────────────────

@Composable
private fun GlassmorphismCtaBar(
    enabled: Boolean,
    onGoToSettle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xCCFFFFFF), // #ffffff at ~80% opacity
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        GradientButton(
            text = "Go to Settle",
            icon = Icons.Filled.East,
            enabled = enabled,
            onClick = onGoToSettle,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Shares Input (integer-only, 0..99) ───────────────────────────────────────

@Composable
private fun SharesField(
    shares: Int,
    onSharesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var textValue by remember(shares) {
        mutableStateOf(if (shares == 0) "" else shares.toString())
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = textValue,
            onValueChange = { newValue ->
                val digitsOnly = newValue.filter { it.isDigit() }.take(2) // max 2 digits → 99
                val parsed = digitsOnly.toIntOrNull() ?: 0
                val clamped = parsed.coerceIn(0, 99)
                textValue = if (clamped == 0) digitsOnly else clamped.toString()
                onSharesChange(clamped)
            },
            modifier = modifier
                .width(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            textStyle = TextStyle(
                fontFamily = ManropeBold,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
            ),
            decorationBox = { innerTextField ->
                if (textValue.isEmpty()) {
                    Text(
                        text = "0",
                        style = TextStyle(
                            fontFamily = ManropeBold,
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                innerTextField()
            },
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "shares",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

internal fun formatAmount(amount: Double): String = AmountFormatter.format(amount)
