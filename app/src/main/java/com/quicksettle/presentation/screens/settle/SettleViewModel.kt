package com.quicksettle.presentation.screens.settle

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.quicksettle.data.local.UserProfile
import com.quicksettle.domain.usecase.GenerateUpiLinkUseCase
import com.quicksettle.presentation.theme.Error
import com.quicksettle.presentation.theme.OnSurfaceVariant
import com.quicksettle.presentation.theme.TertiaryFixedDim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// ── Status Enum ───────────────────────────────────────────────────────────────

enum class SettlementStatus {
    PENDING, URGENT, DUE_TODAY, PAID;

    fun displayText(pendingDays: Int = 0): String = when (this) {
        PENDING -> "PENDING ${pendingDays}D"
        URGENT -> "URGENT"
        DUE_TODAY -> "DUE TODAY"
        PAID -> "PAID"
    }

    fun badgeColor(): Color = when (this) {
        PENDING -> OnSurfaceVariant
        URGENT -> Error
        DUE_TODAY -> OnSurfaceVariant
        PAID -> TertiaryFixedDim
    }
}

// ── Data Classes ──────────────────────────────────────────────────────────────

data class SettlementItem(
    val participantName: String,
    val description: String,
    val amount: Double,
    val status: SettlementStatus = SettlementStatus.PENDING,
    val pendingDays: Int = 0,
    val upiUri: String,
    val shareMessage: String,
    val showQr: Boolean = false,
    val avatarInitial: String,
    val friendUpiId: String?,
)

data class SettleUiState(
    val totalToCollect: Double = 0.0,
    val activeDebtsCount: Int = 0,
    val settlements: List<SettlementItem> = emptyList(),
    val userVpa: String = "",
    val userDisplayName: String = "",
)

data class SplitEntry(
    val name: String,
    val amount: Double,
    val upiId: String?,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class SettleViewModel @Inject constructor(
    private val generateUpiLinkUseCase: GenerateUpiLinkUseCase,
    private val userProfile: UserProfile,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettleUiState())
    val uiState: StateFlow<SettleUiState> = _uiState.asStateFlow()

    init {
        val vpa = userProfile.getUpiId() ?: ""
        val name = userProfile.getDisplayName() ?: ""
        _uiState.update { it.copy(userVpa = vpa, userDisplayName = name) }
    }

    /**
     * Builds the settlement list from a description and a list of split entries.
     * Uses [GenerateUpiLinkUseCase] to create UPI URIs and share messages.
     * Prefixes the description with "Shared: ".
     */
    fun setSettlementData(description: String, splits: List<SplitEntry>) {
        val prefixedDescription = "Shared: $description"
        val vpa = _uiState.value.userVpa

        val items = splits.map { entry ->
            val upiUri = if (vpa.isNotBlank() && entry.upiId != null) {
                generateUpiLinkUseCase.generateUri(
                    vpa = vpa,
                    name = entry.name,
                    amount = entry.amount,
                    description = prefixedDescription,
                )
            } else {
                ""
            }
            val shareMessage = generateUpiLinkUseCase.generateShareMessage(
                name = entry.name,
                amount = entry.amount,
                description = prefixedDescription,
                upiLink = upiUri,
            )
            SettlementItem(
                participantName = entry.name,
                description = prefixedDescription,
                amount = entry.amount,
                status = SettlementStatus.PENDING,
                pendingDays = 0,
                upiUri = upiUri,
                shareMessage = shareMessage,
                showQr = false,
                avatarInitial = entry.name.firstOrNull()?.uppercaseChar()?.toString() ?: "",
                friendUpiId = entry.upiId,
            )
        }

        val total = items
            .filter { it.status != SettlementStatus.PAID }
            .sumOf { it.amount }
        val activeCount = items.count { it.status != SettlementStatus.PAID }

        _uiState.update {
            it.copy(
                settlements = items,
                totalToCollect = total,
                activeDebtsCount = activeCount,
            )
        }
    }

    /** Flips the [SettlementItem.showQr] flag for the item at [index]. */
    fun toggleQr(index: Int) {
        _uiState.update { state ->
            val updated = state.settlements.mapIndexed { i, item ->
                if (i == index) item.copy(showQr = !item.showQr) else item
            }
            state.copy(settlements = updated)
        }
    }

    /** Marks the item at [index] as PAID and recalculates totals. */
    fun markPaid(index: Int) {
        _uiState.update { state ->
            val updated = state.settlements.mapIndexed { i, item ->
                if (i == index) item.copy(status = SettlementStatus.PAID) else item
            }
            val total = updated
                .filter { it.status != SettlementStatus.PAID }
                .sumOf { it.amount }
            val activeCount = updated.count { it.status != SettlementStatus.PAID }
            state.copy(
                settlements = updated,
                totalToCollect = total,
                activeDebtsCount = activeCount,
            )
        }
    }

    /**
     * Returns a single combined message from all non-PAID settlement share messages,
     * joined by two newlines.
     */
    fun settleAllMessage(): String =
        _uiState.value.settlements
            .filter { it.status != SettlementStatus.PAID }
            .joinToString(separator = "\n\n") { it.shareMessage }
}
