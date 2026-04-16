package com.quicksettle.presentation.screens.profile

import androidx.lifecycle.ViewModel
import com.quicksettle.data.local.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ProfileUiState(
    val displayName: String = "",
    val upiId: String = "",
    val saved: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfile: UserProfile,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                displayName = userProfile.getDisplayName() ?: "",
                upiId = userProfile.getUpiId() ?: "",
            )
        }
    }

    fun onDisplayNameChange(name: String) {
        _uiState.update { it.copy(displayName = name, saved = false) }
    }

    fun onUpiIdChange(upiId: String) {
        _uiState.update { it.copy(upiId = upiId, saved = false) }
    }

    fun saveProfile() {
        val state = _uiState.value
        if (state.displayName.isNotBlank() && state.upiId.isNotBlank()) {
            userProfile.saveProfile(
                name = state.displayName.trim(),
                upiId = state.upiId.trim(),
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
