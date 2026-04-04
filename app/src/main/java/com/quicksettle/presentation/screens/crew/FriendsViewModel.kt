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
    val includeSelfInSplit: Boolean = true,
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
 * Computes per-person amounts and owner amount using CalculateSplitUseCase.
 * Returns a pair of (friendId → amountOwed map, ownerAmount).
 * This is called inside the ViewModel to keep use-case logic out of Composables.
 */
fun FriendsUiState.computeSplit(useCase: CalculateSplitUseCase): Pair<Map<String, Double>, Double> {
    if (selectedFriends.isEmpty()) return emptyMap<String, Double>() to totalAmount
    return when (splitMode) {
        SplitMode.EQUAL -> {
            val numberOfPeople = if (includeSelfInSplit) {
                selectedFriends.size + 1
            } else {
                selectedFriends.size
            }
            val splits = useCase.equalSplit(
                totalAmount = totalAmount,
                numberOfPeople = numberOfPeople,
            )
            val amounts = if (includeSelfInSplit) {
                selectedFriends.mapIndexed { index, sf ->
                    sf.friend.id to splits[index + 1]
                }.toMap()
            } else {
                selectedFriends.mapIndexed { index, sf ->
                    sf.friend.id to splits[index]
                }.toMap()
            }
            val ownerAmt = if (includeSelfInSplit) splits[0] else 0.0
            amounts to ownerAmt
        }
        SplitMode.UNEQUAL -> {
            val amounts = selectedFriends.associate { sf ->
                sf.friend.id to sf.manualAmount
            }
            val friendsTotal = selectedFriends.sumOf { (it.manualAmount * 100).toLong() }
            val ownerAmt = ((totalAmount * 100).toLong() - friendsTotal) / 100.0
            amounts to ownerAmt
        }
    }
}

val FriendsUiState.isUnequalOverBudget: Boolean
    get() = splitMode == SplitMode.UNEQUAL &&
        selectedFriends.sumOf { (it.manualAmount * 100).toLong() } > (totalAmount * 100).toLong()

val FriendsUiState.canProceed: Boolean
    get() {
        if (selectedFriends.isEmpty() || totalAmount <= 0.0) return false
        if (splitMode == SplitMode.UNEQUAL) {
            val friendsTotal = selectedFriends.sumOf { (it.manualAmount * 100).toLong() }
            val totalPaisa = (totalAmount * 100).toLong()
            // Friends' amounts must not exceed total, and at least some amount must be assigned
            return friendsTotal in 1..totalPaisa
        }
        return true
    }

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val repository: FriendRepository,
    private val calculateSplitUseCase: CalculateSplitUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    /**
     * Computes the current split. Called from the Composable to get amounts
     * without exposing the use case to the UI layer.
     */
    fun currentSplit(): Pair<Map<String, Double>, Double> =
        _uiState.value.computeSplit(calculateSplitUseCase)

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

    fun toggleIncludeSelf() {
        _uiState.update { it.copy(includeSelfInSplit = !it.includeSelfInSplit) }
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
