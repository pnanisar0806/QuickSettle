package com.quicksettle.presentation.screens.entry

import androidx.lifecycle.ViewModel
import com.quicksettle.data.local.UserProfile
import com.quicksettle.presentation.util.AmountFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    get() = AmountFormatter.formatRawInput(rawAmount)

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val userProfile: UserProfile,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EntryUiState(
            showOnboarding = !userProfile.isProfileSetup(),
            displayName = userProfile.getDisplayName() ?: "",
        ),
    )
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    fun onDigitPress(digit: Char) {
        _uiState.update { state ->
            val newAmount = AmountInputRules.appendDigit(state.rawAmount, digit)
            state.copy(rawAmount = newAmount)
        }
    }

    fun onDecimalPress() {
        _uiState.update { state ->
            val newAmount = AmountInputRules.appendDecimal(state.rawAmount)
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
        userProfile.saveProfile(name = name, upiId = upiId)
        _uiState.update { state ->
            state.copy(
                showOnboarding = false,
                displayName = name,
            )
        }
    }
}

/** Testable amount input rules — extracted from EntryViewModel for direct unit testing. */
internal object AmountInputRules {

    fun appendDigit(current: String, digit: Char): String {
        val hasDot = current.contains('.')
        if (hasDot) {
            val decimalPart = current.substringAfter('.')
            if (decimalPart.length >= 2) return current
        } else {
            if (current.length >= 7) return current
            if (current == "0" && digit != '.') return digit.toString()
        }
        if (current.isEmpty() && digit == '0') return "0"
        return current + digit
    }

    fun appendDecimal(current: String): String {
        if (current.contains('.')) return current
        if (current.isEmpty()) return "0."
        return "$current."
    }
}
