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

private const val OWNER_KEY = "__owner__"

enum class SplitMode { EQUAL, SHARES }

data class SelectedFriend(
    val friend: Friend,
    val shares: Int = 0,
)

data class FriendsUiState(
    val totalAmount: Double = 0.0,
    val description: String = "",
    val splitMode: SplitMode = SplitMode.EQUAL,
    val includeSelfInSplit: Boolean = true,
    val ownerShares: Int = 0,
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
 * Computes per-friend amounts and owner amount using CalculateSplitUseCase.
 * Returns (friendId → amountOwed map, ownerAmount).
 *
 * In SHARES mode the owner is included in the split when [includeSelfInSplit] is true,
 * using a private internal key to avoid collision with friend IDs.
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
        SplitMode.SHARES -> {
            val sharesMap = buildMap {
                selectedFriends.forEach { put(it.friend.id, it.shares) }
                if (includeSelfInSplit) put(OWNER_KEY, ownerShares)
            }
            val totalShares = sharesMap.values.sum()
            if (totalShares <= 0) {
                // Defensive: canProceed prevents reaching here in production. Return zeros.
                return selectedFriends.associate { it.friend.id to 0.0 } to 0.0
            }
            val amounts = useCase.sharesSplit(totalAmount = totalAmount, sharesByPerson = sharesMap)
            val friendAmounts = selectedFriends.associate { sf ->
                sf.friend.id to (amounts[sf.friend.id] ?: 0.0)
            }
            val ownerAmt = if (includeSelfInSplit) amounts[OWNER_KEY] ?: 0.0 else 0.0
            friendAmounts to ownerAmt
        }
    }
}

val FriendsUiState.canProceed: Boolean
    get() {
        if (selectedFriends.isEmpty() || totalAmount <= 0.0) return false
        if (splitMode == SplitMode.SHARES) {
            val totalShares = selectedFriends.sumOf { it.shares } +
                if (includeSelfInSplit) ownerShares else 0
            return totalShares > 0
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
                splitMode = if (state.splitMode == SplitMode.EQUAL) SplitMode.SHARES else SplitMode.EQUAL,
            )
        }
    }

    fun toggleIncludeSelf() {
        _uiState.update { it.copy(includeSelfInSplit = !it.includeSelfInSplit) }
    }

    fun setShares(friendId: String, shares: Int) {
        val clamped = shares.coerceAtLeast(0)
        _uiState.update { state ->
            state.copy(
                selectedFriends = state.selectedFriends.map { sf ->
                    if (sf.friend.id == friendId) sf.copy(shares = clamped) else sf
                },
            )
        }
    }

    fun setOwnerShares(shares: Int) {
        _uiState.update { it.copy(ownerShares = shares.coerceAtLeast(0)) }
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
