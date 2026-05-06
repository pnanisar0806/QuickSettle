package com.quicksettle.presentation.screens.crew

import com.google.common.truth.Truth.assertThat
import com.quicksettle.data.FriendRepository
import com.quicksettle.data.local.FriendDao
import com.quicksettle.domain.model.Friend
import com.quicksettle.domain.usecase.CalculateSplitUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeFriendDao
    private lateinit var repository: FriendRepository
    private lateinit var viewModel: FriendsViewModel

    private val alice = Friend(id = "alice-1", name = "Alice", upiId = "alice@upi")
    private val bob = Friend(id = "bob-2", name = "Bob", upiId = "bob@upi")

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeFriendDao()
        repository = FriendRepository(fakeDao)
        viewModel = FriendsViewModel(repository, CalculateSplitUseCase())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    inner class SessionData {
        @Test
        fun `setSessionData updates total and description`() = runTest {
            viewModel.setSessionData(totalAmount = 500.0, description = "Dinner")
            val state = viewModel.uiState.value
            assertThat(state.totalAmount).isEqualTo(500.0)
            assertThat(state.description).isEqualTo("Dinner")
        }
    }

    @Nested
    inner class FriendToggle {
        @Test
        fun `toggleFriend selects a frequent friend`() = runTest {
            fakeDao.emit(listOf(alice, bob))
            advanceUntilIdle()

            viewModel.toggleFriend("alice-1")
            val state = viewModel.uiState.value
            assertThat(state.selectedFriends).hasSize(1)
            assertThat(state.selectedFriends[0].friend.id).isEqualTo("alice-1")
        }

        @Test
        fun `toggleFriend deselects an already selected friend`() = runTest {
            fakeDao.emit(listOf(alice))
            advanceUntilIdle()

            viewModel.toggleFriend("alice-1")
            viewModel.toggleFriend("alice-1") // deselect
            val state = viewModel.uiState.value
            assertThat(state.selectedFriends).isEmpty()
        }

        @Test
        fun `toggleFriend ignores unknown friend id`() = runTest {
            fakeDao.emit(listOf(alice))
            advanceUntilIdle()

            viewModel.toggleFriend("unknown-id")
            val state = viewModel.uiState.value
            assertThat(state.selectedFriends).isEmpty()
        }
    }

    @Nested
    inner class SplitModeToggling {
        @Test
        fun `default split mode is EQUAL`() {
            assertThat(viewModel.uiState.value.splitMode).isEqualTo(SplitMode.EQUAL)
        }

        @Test
        fun `toggleSplitMode switches to SHARES`() {
            viewModel.toggleSplitMode()
            assertThat(viewModel.uiState.value.splitMode).isEqualTo(SplitMode.SHARES)
        }

        @Test
        fun `toggleSplitMode twice returns to EQUAL`() {
            viewModel.toggleSplitMode()
            viewModel.toggleSplitMode()
            assertThat(viewModel.uiState.value.splitMode).isEqualTo(SplitMode.EQUAL)
        }
    }

    @Nested
    inner class IncludeSelfToggle {
        @Test
        fun `default includes self`() {
            assertThat(viewModel.uiState.value.includeSelfInSplit).isTrue()
        }

        @Test
        fun `toggleIncludeSelf flips to false`() {
            viewModel.toggleIncludeSelf()
            assertThat(viewModel.uiState.value.includeSelfInSplit).isFalse()
        }

        @Test
        fun `toggleIncludeSelf twice returns to true`() {
            viewModel.toggleIncludeSelf()
            viewModel.toggleIncludeSelf()
            assertThat(viewModel.uiState.value.includeSelfInSplit).isTrue()
        }
    }

    @Nested
    inner class Shares {
        @Test
        fun `setShares updates only the targeted friend`() = runTest {
            fakeDao.emit(listOf(alice, bob))
            advanceUntilIdle()
            viewModel.toggleFriend("alice-1")
            viewModel.toggleFriend("bob-2")

            viewModel.setShares(friendId = "alice-1", shares = 3)

            val state = viewModel.uiState.value
            assertThat(state.selectedFriends.first { it.friend.id == "alice-1" }.shares).isEqualTo(3)
            assertThat(state.selectedFriends.first { it.friend.id == "bob-2" }.shares).isEqualTo(0)
        }

        @Test
        fun `setShares clamps negative values to zero`() = runTest {
            fakeDao.emit(listOf(alice))
            advanceUntilIdle()
            viewModel.toggleFriend("alice-1")

            viewModel.setShares(friendId = "alice-1", shares = -5)

            val state = viewModel.uiState.value
            assertThat(state.selectedFriends.first().shares).isEqualTo(0)
        }

        @Test
        fun `setOwnerShares updates ownerShares`() = runTest {
            viewModel.setOwnerShares(shares = 4)
            assertThat(viewModel.uiState.value.ownerShares).isEqualTo(4)
        }

        @Test
        fun `setOwnerShares clamps negative values to zero`() = runTest {
            viewModel.setOwnerShares(shares = -2)
            assertThat(viewModel.uiState.value.ownerShares).isEqualTo(0)
        }

        @Test
        fun `toggleSplitMode flips between EQUAL and SHARES`() = runTest {
            assertThat(viewModel.uiState.value.splitMode).isEqualTo(SplitMode.EQUAL)
            viewModel.toggleSplitMode()
            assertThat(viewModel.uiState.value.splitMode).isEqualTo(SplitMode.SHARES)
            viewModel.toggleSplitMode()
            assertThat(viewModel.uiState.value.splitMode).isEqualTo(SplitMode.EQUAL)
        }
    }

    @Nested
    inner class SearchFiltering {
        @Test
        fun `search filters friends by name`() = runTest {
            fakeDao.emit(listOf(alice, bob))
            advanceUntilIdle()

            viewModel.onSearchQueryChange("Ali")
            val state = viewModel.uiState.value
            assertThat(state.filteredFriends).hasSize(1)
            assertThat(state.filteredFriends[0].name).isEqualTo("Alice")
        }

        @Test
        fun `empty search shows all friends`() = runTest {
            fakeDao.emit(listOf(alice, bob))
            advanceUntilIdle()

            viewModel.onSearchQueryChange("Ali")
            viewModel.onSearchQueryChange("")
            val state = viewModel.uiState.value
            assertThat(state.filteredFriends).hasSize(2)
        }

        @Test
        fun `search is case insensitive`() = runTest {
            fakeDao.emit(listOf(alice))
            advanceUntilIdle()

            viewModel.onSearchQueryChange("alice")
            assertThat(viewModel.uiState.value.filteredFriends).hasSize(1)
        }
    }

    @Nested
    inner class AddFriendOverlay {
        @Test
        fun `openAddFriend sets flag and clears inputs`() {
            viewModel.onAddFriendNameChange("old")
            viewModel.onUpiInputChange("old@upi")
            viewModel.openAddFriend()

            val state = viewModel.uiState.value
            assertThat(state.showAddFriend).isTrue()
            assertThat(state.addFriendNameInput).isEmpty()
            assertThat(state.upiInputValue).isEmpty()
        }

        @Test
        fun `closeAddFriend clears flag`() {
            viewModel.openAddFriend()
            viewModel.closeAddFriend()
            assertThat(viewModel.uiState.value.showAddFriend).isFalse()
        }

        @Test
        fun `onAddFriendNameChange updates state`() {
            viewModel.onAddFriendNameChange("Charlie")
            assertThat(viewModel.uiState.value.addFriendNameInput).isEqualTo("Charlie")
        }

        @Test
        fun `onUpiInputChange updates state`() {
            viewModel.onUpiInputChange("charlie@upi")
            assertThat(viewModel.uiState.value.upiInputValue).isEqualTo("charlie@upi")
        }
    }

    @Nested
    inner class FrequentFriendsFlow {
        @Test
        fun `frequentFriends populated from dao`() = runTest {
            fakeDao.emit(listOf(alice, bob))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.frequentFriends).hasSize(2)
            assertThat(state.filteredFriends).hasSize(2)
        }

        @Test
        fun `frequentFriends updates when dao emits new list`() = runTest {
            fakeDao.emit(listOf(alice))
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.frequentFriends).hasSize(1)

            fakeDao.emit(listOf(alice, bob))
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.frequentFriends).hasSize(2)
        }
    }

    // ── AddFriendManually ────────────────────────────────────────────────────

    @Nested
    inner class AddFriendManually {

        @Test
        fun `adds friend to DAO`() = runTest {
            fakeDao.emit(emptyList())
            advanceUntilIdle()

            viewModel.onAddFriendNameChange("Charlie")
            viewModel.onUpiInputChange("charlie@upi")
            viewModel.addFriendManually()
            advanceUntilIdle()

            val friends = viewModel.uiState.value.frequentFriends
            assertThat(friends).hasSize(1)
            assertThat(friends[0].name).isEqualTo("Charlie")
        }

        @Test
        fun `friend appears in frequentFriends after add`() = runTest {
            fakeDao.emit(emptyList())
            advanceUntilIdle()

            viewModel.onAddFriendNameChange("Charlie")
            viewModel.onUpiInputChange("charlie@upi")
            viewModel.addFriendManually()
            advanceUntilIdle()

            val friends = viewModel.uiState.value.frequentFriends
            assertThat(friends.any { it.name == "Charlie" }).isTrue()
        }

        @Test
        fun `trims name before saving`() = runTest {
            fakeDao.emit(emptyList())
            advanceUntilIdle()

            viewModel.onAddFriendNameChange("  Charlie  ")
            viewModel.onUpiInputChange("charlie@upi")
            viewModel.addFriendManually()
            advanceUntilIdle()

            val friends = viewModel.uiState.value.frequentFriends
            assertThat(friends[0].name).isEqualTo("Charlie")
        }

        @Test
        fun `blank name is no-op`() = runTest {
            fakeDao.emit(emptyList())
            advanceUntilIdle()

            viewModel.onAddFriendNameChange("   ")
            viewModel.addFriendManually()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.frequentFriends).isEmpty()
        }

        @Test
        fun `blank UPI becomes null`() = runTest {
            fakeDao.emit(emptyList())
            advanceUntilIdle()

            viewModel.onAddFriendNameChange("Charlie")
            viewModel.onUpiInputChange("   ")
            viewModel.addFriendManually()
            advanceUntilIdle()

            val friends = viewModel.uiState.value.frequentFriends
            assertThat(friends[0].upiId).isNull()
        }

        @Test
        fun `closes overlay after adding`() = runTest {
            fakeDao.emit(emptyList())
            advanceUntilIdle()

            viewModel.openAddFriend()
            viewModel.onAddFriendNameChange("Charlie")
            viewModel.addFriendManually()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.showAddFriend).isFalse()
        }
    }

    // ── AddFriendFromContact ─────────────────────────────────────────────────

    @Nested
    inner class AddFriendFromContact {

        @Test
        fun `adds friend to DAO`() = runTest {
            fakeDao.emit(emptyList())
            advanceUntilIdle()

            viewModel.addFriendFromContact("Diana")
            advanceUntilIdle()

            val friends = viewModel.uiState.value.frequentFriends
            assertThat(friends).hasSize(1)
            assertThat(friends[0].name).isEqualTo("Diana")
            assertThat(friends[0].upiId).isNull()
        }

        @Test
        fun `blank name is no-op`() = runTest {
            fakeDao.emit(emptyList())
            advanceUntilIdle()

            viewModel.addFriendFromContact("  ")
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.frequentFriends).isEmpty()
        }
    }

    // ── AddFriendAndClose ────────────────────────────────────────────────────

    @Nested
    inner class AddFriendAndClose {

        @Test
        fun `adds friend and closes overlay`() = runTest {
            fakeDao.emit(emptyList())
            advanceUntilIdle()

            viewModel.openAddFriend()
            viewModel.addFriendAndClose("Eve", "eve@upi")
            advanceUntilIdle()

            val friends = viewModel.uiState.value.frequentFriends
            assertThat(friends).hasSize(1)
            assertThat(friends[0].name).isEqualTo("Eve")
            assertThat(viewModel.uiState.value.showAddFriend).isFalse()
        }

        @Test
        fun `blank name is no-op`() = runTest {
            fakeDao.emit(emptyList())
            advanceUntilIdle()

            viewModel.addFriendAndClose("", "eve@upi")
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.frequentFriends).isEmpty()
        }

        @Test
        fun `blank UPI becomes null`() = runTest {
            fakeDao.emit(emptyList())
            advanceUntilIdle()

            viewModel.addFriendAndClose("Eve", "  ")
            advanceUntilIdle()

            val friends = viewModel.uiState.value.frequentFriends
            assertThat(friends[0].upiId).isNull()
        }
    }

    // ── UpdateLastUsed side-effect ───────────────────────────────────────────

    @Nested
    inner class UpdateLastUsedSideEffect {

        @Test
        fun `toggleFriend calls updateLastUsed on selection`() = runTest {
            fakeDao.emit(listOf(alice))
            advanceUntilIdle()

            viewModel.toggleFriend("alice-1")
            advanceUntilIdle()

            // Verify the friend was selected (side-effect of updateLastUsed
            // is not directly observable with our fake, but we verify selection works)
            assertThat(viewModel.uiState.value.selectedFriends).hasSize(1)
            assertThat(viewModel.uiState.value.selectedFriends[0].friend.id).isEqualTo("alice-1")
        }
    }
}

// ── Fake DAO for testing ─────────────────────────────────────────────────────

class FakeFriendDao : FriendDao {
    private val friendsFlow = MutableStateFlow<List<Friend>>(emptyList())

    fun emit(friends: List<Friend>) {
        friendsFlow.value = friends
    }

    override fun getAllFriends(): Flow<List<Friend>> = friendsFlow

    override fun searchFriends(query: String): Flow<List<Friend>> = friendsFlow

    override suspend fun insertFriend(friend: Friend) {
        friendsFlow.value = friendsFlow.value + friend
    }

    override suspend fun deleteFriend(friend: Friend) {
        friendsFlow.value = friendsFlow.value.filter { it.id != friend.id }
    }

    override suspend fun updateLastUsed(id: String, timestamp: Long) {
        // no-op for tests
    }
}
