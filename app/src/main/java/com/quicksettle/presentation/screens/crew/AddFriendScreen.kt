package com.quicksettle.presentation.screens.crew

import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quicksettle.domain.model.Friend
import com.quicksettle.presentation.theme.ManropeBold
import com.quicksettle.presentation.theme.OnPrimary
import com.quicksettle.presentation.theme.Primary
import com.quicksettle.presentation.theme.PrimaryContainer
import com.quicksettle.presentation.theme.PrimaryFixed

/**
 * Add Friend full-screen overlay.
 *
 * Hosts:
 * 1. Top bar with back arrow
 * 2. Search bar
 * 3. "Import from Contacts" button (uses ACTION_PICK — no READ_CONTACTS permission)
 * 4. Suggested Friends horizontal scroll
 * 5. Manual UPI ID entry
 * 6. "Add to Group" CTA
 */
@Composable
fun AddFriendScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Contact picker — ACTION_PICK, no permission required
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null,
            null,
            null,
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIdx = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val name = if (nameIdx >= 0) it.getString(nameIdx) else null
                if (!name.isNullOrBlank()) {
                    viewModel.addFriendFromContact(name)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Top Bar ──────────────────────────────────────────────────────────
        AddFriendTopBar(onBack = onDismiss)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Search Bar ───────────────────────────────────────────────────
            AddFriendSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
            )

            // ── Import from Contacts ─────────────────────────────────────────
            ImportContactsButton(onClick = { contactPickerLauncher.launch(null) })

            // ── Suggested Friends ────────────────────────────────────────────
            if (uiState.frequentFriends.isNotEmpty()) {
                SuggestedFriendsSection(
                    friends = uiState.frequentFriends,
                    onAdd = { friend -> viewModel.toggleFriend(friend.id) },
                )
            }

            // ── Manual UPI ID Entry ──────────────────────────────────────────
            ManualUpiSection(
                nameValue = uiState.addFriendNameInput,
                onNameChange = viewModel::onAddFriendNameChange,
                upiValue = uiState.upiInputValue,
                onUpiChange = viewModel::onUpiInputChange,
            )

            // ── Add to Group CTA ─────────────────────────────────────────────
            AddToGroupButton(
                enabled = uiState.addFriendNameInput.isNotBlank(),
                onClick = {
                    viewModel.addFriendAndClose(
                        name = uiState.addFriendNameInput,
                        upiId = uiState.upiInputValue.ifBlank { null },
                    )
                    onDismiss()
                },
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@Composable
private fun AddFriendTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Add Friend",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = ManropeBold,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Placeholder right icon for visual balance
        Box(modifier = Modifier.size(48.dp))
    }
}

// ── Search Bar ────────────────────────────────────────────────────────────────

@Composable
private fun AddFriendSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "Search contacts or enter name",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Search,
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

// ── Import from Contacts ──────────────────────────────────────────────────────

@Composable
private fun ImportContactsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Primary, PrimaryContainer),
                    start = Offset(0f, Float.POSITIVE_INFINITY),
                    end = Offset(Float.POSITIVE_INFINITY, 0f),
                ),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Contacts,
                contentDescription = null,
                tint = OnPrimary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = "Import from Contacts",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = ManropeBold,
                    fontWeight = FontWeight.Bold,
                ),
                color = OnPrimary,
            )
        }
    }
}

// ── Suggested Friends Grid ────────────────────────────────────────────────────

@Composable
private fun SuggestedFriendsSection(
    friends: List<Friend>,
    onAdd: (Friend) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Suggested Friends",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = ManropeBold,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            // FREQUENT chip
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PrimaryFixed)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "FREQUENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Primary,
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(
                items = friends,
                key = { "suggested_${it.id}" },
            ) { friend ->
                SuggestedFriendAvatar(
                    friend = friend,
                    onAdd = { onAdd(friend) },
                )
            }
        }
    }
}

@Composable
private fun SuggestedFriendAvatar(
    friend: Friend,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onAdd)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            FriendAvatar(
                name = friend.name,
                modifier = Modifier.size(64.dp),
            )
            // Green "+" badge bottom-right
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .background(
                        color = Color(0xFF2C694E),
                        shape = CircleShape,
                    ),
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = ManropeBold,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                )
            }
        }
        Text(
            text = friend.name.split(" ").firstOrNull() ?: friend.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Manual UPI ID Entry ───────────────────────────────────────────────────────

@Composable
private fun ManualUpiSection(
    nameValue: String,
    onNameChange: (String) -> Unit,
    upiValue: String,
    onUpiChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "MANUAL ENTRY",
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 2.sp,
                fontFamily = ManropeBold,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Name field
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Name",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextField(
                value = nameValue,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Friend's name",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.PersonSearch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                colors = upiTextFieldColors(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }

        // UPI ID field
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "UPI ID ENTRY",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 2.sp,
                    fontFamily = ManropeBold,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextField(
                value = upiValue,
                onValueChange = onUpiChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "username@upi",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.AlternateEmail,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                colors = upiTextFieldColors(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Text(
                text = "Verified UPI IDs allow direct settlement to bank accounts.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = FontStyle.Italic,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun upiTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary,
)

// ── Add to Group CTA ──────────────────────────────────────────────────────────

@Composable
private fun AddToGroupButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(
                brush = if (enabled) {
                    Brush.linearGradient(
                        colors = listOf(Primary, PrimaryContainer),
                        start = Offset(0f, Float.POSITIVE_INFINITY),
                        end = Offset(Float.POSITIVE_INFINITY, 0f),
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF012D1D).copy(alpha = 0.5f), Color(0xFF1B4332).copy(alpha = 0.5f)),
                        start = Offset(0f, Float.POSITIVE_INFINITY),
                        end = Offset(Float.POSITIVE_INFINITY, 0f),
                    )
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = null,
                tint = if (enabled) OnPrimary else OnPrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Add to Group",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = ManropeBold,
                    fontWeight = FontWeight.Bold,
                ),
                color = if (enabled) OnPrimary else OnPrimary.copy(alpha = 0.7f),
            )
        }
    }
}
