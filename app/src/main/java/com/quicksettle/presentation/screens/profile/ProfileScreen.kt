package com.quicksettle.presentation.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quicksettle.presentation.components.GradientButton
import com.quicksettle.presentation.components.LedgerTextField
import com.quicksettle.presentation.theme.ManropeBold
import com.quicksettle.presentation.theme.OnSurface
import com.quicksettle.presentation.theme.OnSurfaceVariant
import com.quicksettle.presentation.theme.Primary
import com.quicksettle.presentation.theme.PrimaryContainer
import com.quicksettle.presentation.theme.PrimaryFixed
import com.quicksettle.presentation.theme.SurfaceContainerLowest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val canSave = uiState.displayName.isNotBlank() && uiState.upiId.isNotBlank()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = ManropeBold,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = OnSurface,
                navigationIconContentColor = OnSurface,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // ── Avatar ───────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = Color(0x0F002114),
                        spotColor = Color(0x0F002114),
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Primary, PrimaryContainer),
                        ),
                    ),
            ) {
                Text(
                    text = uiState.displayName
                        .firstOrNull()
                        ?.uppercaseChar()
                        ?.toString()
                        ?: "",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = ManropeBold,
                    ),
                    color = Color.White,
                )
            }

            // ── Form Card ────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color(0x0F002114),
                        spotColor = Color(0x0F002114),
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLowest)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Display Name
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DISPLAY NAME",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 2.sp,
                            fontFamily = ManropeBold,
                        ),
                        color = OnSurfaceVariant,
                    )
                    LedgerTextField(
                        value = uiState.displayName,
                        onValueChange = viewModel::onDisplayNameChange,
                        placeholder = "Your name",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // UPI VPA
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "UPI ID",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 2.sp,
                            fontFamily = ManropeBold,
                        ),
                        color = OnSurfaceVariant,
                    )
                    LedgerTextField(
                        value = uiState.upiId,
                        onValueChange = viewModel::onUpiIdChange,
                        placeholder = "yourname@upi",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── Saved confirmation ───────────────────────────────────────
            if (uiState.saved) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryFixed)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "Profile saved",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = OnSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Save Button ──────────────────────────────────────────────
            GradientButton(
                text = "Save Profile",
                onClick = viewModel::saveProfile,
                icon = Icons.Filled.Check,
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
