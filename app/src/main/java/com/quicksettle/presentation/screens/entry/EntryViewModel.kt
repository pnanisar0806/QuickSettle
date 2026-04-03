package com.quicksettle.presentation.screens.entry

import androidx.lifecycle.ViewModel
import com.quicksettle.data.local.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

data class EntryUiState(
    val rawAmount: String = "",
    val description: String = "",
    val displayName: String = "",
    val showOnboarding: Boolean = false,
)

val EntryUiState.canProceed: Boolean
    get() = rawAmount.isNotEmpty() && rawAmount != "0" && rawAmount.toDoubleOrNull() != 0.0

val EntryUiState.formattedAmount: String
    get() {
        if (rawAmount.isEmpty()) return "0"
        // Split on decimal point
        val parts = rawAmount.split(".")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) parts[1] else null

        // Format integer part with Indian grouping
        val intValue = intPart.toLongOrNull() ?: 0L
        val formatted = formatIndian(intValue)

        return if (decPart != null) "$formatted.$decPart" else formatted
    }

/**
 * Formats a long with Indian comma grouping:
 * last 3 digits, then groups of 2.
 * e.g. 1234567 -> "12,34,567"
 */
private fun formatIndian(number: Long): String {
    if (number == 0L) return "0"
    val str = number.toString()
    if (str.length <= 3) return str

    val lastThree = str.takeLast(3)
    val remaining = str.dropLast(3)
    val groups = mutableListOf<String>()
    var i = remaining.length
    while (i > 0) {
        val start = maxOf(0, i - 2)
        groups.add(0, remaining.substring(start, i))
        i = start
    }
    return groups.joinToString(",") + "," + lastThree
}

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val userProfileStore: UserProfileStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EntryUiState(
            showOnboarding = !userProfileStore.isProfileSetup(),
            displayName = userProfileStore.getDisplayName() ?: "",
        )
    )
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    fun onDigitPress(digit: Char) {
        _uiState.update { state ->
            val current = state.rawAmount
            val newAmount = appendDigit(current, digit)
            state.copy(rawAmount = newAmount)
        }
    }

    fun onDecimalPress() {
        _uiState.update { state ->
            val current = state.rawAmount
            val newAmount = appendDecimal(current)
            state.copy(rawAmount = newAmount)
        }
    }

    fun onBackspace() {
        _uiState.update { state ->
            val current = state.rawAmount
            val newAmount = if (current.isEmpty()) "" else current.dropLast(1)
            state.copy(rawAmount = newAmount)
        }
    }

    fun onClear() {
        _uiState.update { state -> state.copy(rawAmount = "") }
    }

    fun onDescriptionChange(text: String) {
        _uiState.update { state -> state.copy(description = text) }
    }

    fun onProfileSaved(name: String, upiId: String) {
        userProfileStore.saveProfile(name = name, upiId = upiId)
        _uiState.update { state ->
            state.copy(
                showOnboarding = false,
                displayName = name,
            )
        }
    }

    // ── Amount input rules ────────────────────────────────────────────────────

    private fun appendDigit(current: String, digit: Char): String {
        val hasDot = current.contains('.')

        if (hasDot) {
            val decimalPart = current.substringAfter('.')
            // Max 2 decimal places
            if (decimalPart.length >= 2) return current
        } else {
            val intPart = current
            // Max 7 digits before decimal
            if (intPart.length >= 7) return current
            // Prevent leading zeros: "0" + digit (non-dot) should replace rather than prepend
            if (intPart == "0" && digit != '.') return digit.toString()
        }

        // Starting from empty, digit 0 becomes "0"
        if (current.isEmpty() && digit == '0') return "0"

        return current + digit
    }

    private fun appendDecimal(current: String): String {
        // Already has decimal
        if (current.contains('.')) return current
        // Start with "0." if empty
        if (current.isEmpty()) return "0."
        return "$current."
    }
}
