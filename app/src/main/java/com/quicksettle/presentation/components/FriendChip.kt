package com.quicksettle.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quicksettle.domain.model.Friend

/**
 * Friend selection chip — placeholder.
 *
 * Full implementation: avatar initial circle, name label, selected state
 * uses primary_fixed left-pill indicator (no borders).
 */
@Composable
fun FriendChip(
    friend: Friend,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO: implement in crew-screen phase
}
