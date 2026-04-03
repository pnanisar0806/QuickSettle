package com.quicksettle.presentation.screens.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quicksettle.presentation.components.GradientButton
import com.quicksettle.presentation.components.LedgerTextField
import com.quicksettle.presentation.components.NumberPad
import com.quicksettle.presentation.theme.ManropeBold
import com.quicksettle.presentation.theme.ManropeExtraBold

/**
 * Bill Entry screen — Amount tab.
 *
 * From top to bottom:
 * 1. Bill Description section (label + ledger-style text field)
 * 2. Amount Display Card (raised, ambient shadow, display-lg rupee amount)
 * 3. Custom Number Pad (hero component, 4x3 grid)
 * 4. "Add Friends" CTA (gradient button, disabled when amount is 0)
 *
 * On first launch shows an onboarding BottomSheet to capture display name + UPI ID.
 */
@Composable
fun EntryScreen(
    onNavigateToFriends: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EntryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        EntryContent(
            uiState = uiState,
            onDescriptionChange = viewModel::onDescriptionChange,
            onDigitPress = viewModel::onDigitPress,
            onDecimalPress = viewModel::onDecimalPress,
            onBackspace = viewModel::onBackspace,
            onAddFriends = onNavigateToFriends,
        )

        if (uiState.showOnboarding) {
            OnboardingSheet(onProfileSaved = viewModel::onProfileSaved)
        }
    }
}

@Composable
private fun EntryContent(
    uiState: EntryUiState,
    onDescriptionChange: (String) -> Unit,
    onDigitPress: (Char) -> Unit,
    onDecimalPress: () -> Unit,
    onBackspace: () -> Unit,
    onAddFriends: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        BillDescriptionSection(
            description = uiState.description,
            onDescriptionChange = onDescriptionChange,
        )

        AmountDisplayCard(formattedAmount = uiState.formattedAmount)

        NumberPad(
            onDigit = onDigitPress,
            onDecimal = onDecimalPress,
            onBackspace = onBackspace,
            modifier = Modifier.fillMaxWidth(),
        )

        GradientButton(
            text = "Add Friends",
            icon = Icons.Filled.PersonAdd,
            enabled = uiState.canProceed,
            onClick = onAddFriends,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BillDescriptionSection(
    description: String,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "BILL DESCRIPTION",
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 2.sp,
                fontFamily = ManropeBold,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LedgerTextField(
            value = description,
            onValueChange = onDescriptionChange,
            placeholder = "What's this for?",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AmountDisplayCard(
    formattedAmount: String,
    modifier: Modifier = Modifier,
) {
    // Ambient shadow: Y:12dp Blur:24dp Spread:-4dp, #002114 at 6% opacity
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(40.dp),
                ambientColor = Color(0x0F002114),
                spotColor = Color(0x0F002114),
            )
            .clip(RoundedCornerShape(40.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 24.dp, vertical = 40.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Total Amount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "₹",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = ManropeExtraBold,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = ManropeExtraBold,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Onboarding Bottom Sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingSheet(
    onProfileSaved: (name: String, upiId: String) -> Unit,
) {
    var displayName by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val canSave = displayName.isNotBlank() && upiId.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = { /* prevent dismissal until profile is saved */ },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Welcome to Quick-Settle",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = ManropeBold,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Set up your profile to start splitting bills.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "DISPLAY NAME",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 2.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LedgerTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    placeholder = "Your name",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "UPI ID",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 2.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LedgerTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    placeholder = "yourname@upi",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            GradientButton(
                text = "Get Started",
                onClick = { onProfileSaved(displayName.trim(), upiId.trim()) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
