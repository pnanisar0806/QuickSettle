package com.quicksettle.presentation.screens.crew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicksettle.data.FriendRepository
import com.quicksettle.domain.model.Friend
import com.quicksettle.domain.usecase.CalculateSplitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SplitMode { EQUAL, UNEQUAL }

data class SelectedFriend(
    val friend: Friend,
    val manualAmount: Double = 0.0,
)

data class FriendsUiState(
    val totalAmount: Double = 0.0,
    val description: String = "",
    val splitMode: SplitMode = SplitMode.EQUAL,
    val selectedFriends: List<SelectedFriend> = emptyList(),
    val frequentFriends: List<Friend> = emptyList(),
    val searchQuery: String = "",
    val filteredFriends: List<Friend> = emptyList(),
    val showAddFriend: Boolean = false,
    val upiInputValue: String = "",
    val addFriendNameInput: String = "",
)

/** Friends that appear in frequentFriends but are NOT already selected. */
val FriendsUiState.suggestions: List<Friend>
    get() {
        val selectedIds = selectedFriends.map { it.friend.id }.toSet()
        return frequentFriends.filter { it.id !in selectedIds }
    }

/**
 * Per-person amounts using CalculateSplitUseCase.
 * The owner (current user) is always participant slot index 0.
 * Returns a map of friendId → amountOwed.
 */
fun FriendsUiState.perPersonAmounts(useCase: CalculateSplitUseCase): Map<String, Double> {
    if (selectedFriends.isEmpty()) return emptyMap()
    return when (splitMode) {
        SplitMode.EQUAL -> {
            val splits = useCase.equalSplit(
                totalAmount = totalAmount,
                numberOfPeople = selectedFriends.size + 1, // +1 for the owner
            )
            // index 0 is the owner's share; indices 1..n map to selected friends
            selectedFriends.mapIndexed { index, sf ->
                sf.friend.id to splits[index + 1]
            }.toMap()
        }
        SplitMode.UNEQUAL -> {
            val fixedAmounts = selectedFriends.associate { sf ->
                sf.friend.id to sf.manualAmount
            }
            // remaining is what the owner owes; each friend keeps their manualAmount
            selectedFriends.associate { sf ->
                sf.friend.id to sf.manualAmount
            }
        }
    }
}

val FriendsUiState.ownerAmount: Double
    get() {
        if (selectedFriends.isEmpty()) return totalAmount
        return when (splitMode) {
            SplitMode.EQUAL -> {
                val totalParts = selectedFriends.size + 1
                // Owner always gets the first share from equal split (may have 1 extra paisa)
                val totalPaisa = (totalAmount * 100).toLong()
                val base = totalPaisa / totalParts
                val remainder = (totalPaisa % totalParts).toInt()
                val ownerPaisa = if (0 < remainder) base + 1 else base
                ownerPaisa / 100.0
            }
            SplitMode.UNEQUAL -> {
                val totalFixed = selectedFriends.sumOf { it.manualAmount }
                val remaining = (totalAmount * 100).toLong() - (totalFixed * 100).toLong()
                remaining / 100.0
            }
        }
    }

val FriendsUiState.unequalRemaining: Double
    get() {
        val totalFixed = selectedFriends.sumOf { it.manualAmount }
        val remaining = (totalAmount * 100).toLong() - (totalFixed * 100).toLong()
        return remaining / 100.0
    }

val FriendsUiState.canProceed: Boolean
    get() = selectedFriends.isNotEmpty() && totalAmount > 0.0

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val repository: FriendRepository,
    private val calculateSplitUseCase: CalculateSplitUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllFriends().collect { friends ->
                _uiState.update { state ->
                    state.copy(
                        frequentFriends = friends,
                        filteredFriends = if (state.searchQuery.isBlank()) friends else friends.filter {
                            it.name.contains(state.searchQuery, ignoreCase = true)
                        },
                    )
                }
            }
        }
    }

    /** Called from NavGraph / EntryViewModel to pass the shared bill amount and description. */
    fun setSessionData(totalAmount: Double, description: String) {
        _uiState.update { it.copy(totalAmount = totalAmount, description = description) }
    }

    fun toggleFriend(friendId: String) {
        _uiState.update { state ->
            val already = state.selectedFriends.any { it.friend.id == friendId }
            if (already) {
                state.copy(selectedFriends = state.selectedFriends.filter { it.friend.id != friendId })
            } else {
                val friend = state.frequentFriends.find { it.id == friendId } ?: return@update state
                state.copy(selectedFriends = state.selectedFriends + SelectedFriend(friend = friend))
            }
        }
        // Stamp last-used when selecting a friend
        val isNowSelected = _uiState.value.selectedFriends.any { it.friend.id == friendId }
        if (isNowSelected) {
            viewModelScope.launch { repository.updateLastUsed(friendId) }
        }
    }

    fun toggleSplitMode() {
        _uiState.update { state ->
            state.copy(
                splitMode = if (state.splitMode == SplitMode.EQUAL) SplitMode.UNEQUAL else SplitMode.EQUAL,
            )
        }
    }

    fun setManualAmount(friendId: String, amount: Double) {
        _uiState.update { state ->
            state.copy(
                selectedFriends = state.selectedFriends.map { sf ->
                    if (sf.friend.id == friendId) sf.copy(manualAmount = amount) else sf
                },
            )
        }
    }

    fun openAddFriend() {
        _uiState.update { it.copy(showAddFriend = true, addFriendNameInput = "", upiInputValue = "") }
    }

    fun closeAddFriend() {
        _uiState.update { it.copy(showAddFriend = false) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredFriends = if (query.isBlank()) state.frequentFriends else state.frequentFriends.filter {
                    it.name.contains(query, ignoreCase = true)
                },
            )
        }
    }

    fun onAddFriendNameChange(name: String) {
        _uiState.update { it.copy(addFriendNameInput = name) }
    }

    fun onUpiInputChange(upi: String) {
        _uiState.update { it.copy(upiInputValue = upi) }
    }

    fun addFriendFromContact(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addFriend(name = name.trim(), upiId = null)
        }
    }

    fun addFriendManually() {
        val state = _uiState.value
        val name = state.addFriendNameInput.trim()
        val upiId = state.upiInputValue.trim().ifBlank { null }
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addFriend(name = name, upiId = upiId)
            // Auto-select the newly added friend
            val updated = _uiState.value
            val newFriend = updated.frequentFriends.firstOrNull { it.name == name }
            if (newFriend != null) {
                toggleFriend(newFriend.id)
            }
            closeAddFriend()
        }
    }

    fun addFriendAndClose(name: String, upiId: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addFriend(name = name.trim(), upiId = upiId?.trim()?.ifBlank { null })
            closeAddFriend()
        }
    }
}
