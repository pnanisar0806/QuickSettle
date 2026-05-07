package com.quicksettle.presentation.screens.crew

import com.google.common.truth.Truth.assertThat
import com.quicksettle.domain.model.Friend
import com.quicksettle.domain.usecase.CalculateSplitUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.math.roundToLong

class FriendsUiStateTest {

    private lateinit var useCase: CalculateSplitUseCase

    private val alice = Friend(id = "alice-1", name = "Alice", upiId = "alice@upi")
    private val bob = Friend(id = "bob-2", name = "Bob", upiId = "bob@upi")
    private val charlie = Friend(id = "charlie-3", name = "Charlie")

    @BeforeEach
    fun setUp() {
        useCase = CalculateSplitUseCase()
    }

    /** Helper: returns (friendAmounts, ownerAmount) */
    private fun FriendsUiState.split() = computeSplit(useCase)

    // ── suggestions ──────────────────────────────────────────────────

    @Nested
    inner class Suggestions {
        @Test
        fun `suggestions excludes selected friends`() {
            val state = FriendsUiState(
                frequentFriends = listOf(alice, bob, charlie),
                selectedFriends = listOf(SelectedFriend(friend = alice)),
            )
            assertThat(state.suggestions.map { it.id }).containsExactly("bob-2", "charlie-3")
        }

        @Test
        fun `suggestions returns all when none selected`() {
            val state = FriendsUiState(
                frequentFriends = listOf(alice, bob),
                selectedFriends = emptyList(),
            )
            assertThat(state.suggestions).hasSize(2)
        }

        @Test
        fun `suggestions returns empty when all selected`() {
            val state = FriendsUiState(
                frequentFriends = listOf(alice),
                selectedFriends = listOf(SelectedFriend(friend = alice)),
            )
            assertThat(state.suggestions).isEmpty()
        }
    }

    // ── computeSplit — Equal mode ────────────────────────────────────

    @Nested
    inner class EqualSplitAmounts {
        @Test
        fun `equal split with self included divides among n+1`() {
            val state = FriendsUiState(
                totalAmount = 300.0,
                splitMode = SplitMode.EQUAL,
                includeSelfInSplit = true,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice),
                    SelectedFriend(friend = bob),
                ),
            )
            val (amounts, _) = state.split()
            // 300 / 3 = 100 each
            assertThat(amounts["alice-1"]).isEqualTo(100.0)
            assertThat(amounts["bob-2"]).isEqualTo(100.0)
        }

        @Test
        fun `equal split with self excluded divides among n only`() {
            val state = FriendsUiState(
                totalAmount = 300.0,
                splitMode = SplitMode.EQUAL,
                includeSelfInSplit = false,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice),
                    SelectedFriend(friend = bob),
                ),
            )
            val (amounts, _) = state.split()
            // 300 / 2 = 150 each (owner excluded)
            assertThat(amounts["alice-1"]).isEqualTo(150.0)
            assertThat(amounts["bob-2"]).isEqualTo(150.0)
        }

        @Test
        fun `equal split paisa invariant holds with self included`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.EQUAL,
                includeSelfInSplit = true,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice),
                    SelectedFriend(friend = bob),
                    SelectedFriend(friend = charlie),
                ),
            )
            val (amounts, ownerAmt) = state.split()
            val totalPaisa = amounts.values.sumOf { (it * 100).roundToLong() } +
                (ownerAmt * 100).roundToLong()
            assertThat(totalPaisa).isEqualTo(10000L)
        }

        @Test
        fun `equal split paisa invariant holds with self excluded`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.EQUAL,
                includeSelfInSplit = false,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice),
                    SelectedFriend(friend = bob),
                    SelectedFriend(friend = charlie),
                ),
            )
            val (amounts, _) = state.split()
            val totalPaisa = amounts.values.sumOf { (it * 100).roundToLong() }
            assertThat(totalPaisa).isEqualTo(10000L)
        }

        @Test
        fun `equal split empty friends returns empty map`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.EQUAL,
                selectedFriends = emptyList(),
            )
            val (amounts, _) = state.split()
            assertThat(amounts).isEmpty()
        }

        @Test
        fun `equal split odd amount distributes extra paisa correctly`() {
            val state = FriendsUiState(
                totalAmount = 10.0,
                splitMode = SplitMode.EQUAL,
                includeSelfInSplit = true,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice),
                    SelectedFriend(friend = bob),
                ),
            )
            val (amounts, ownerAmt) = state.split()
            val total = amounts.values.sumOf { (it * 100).roundToLong() } +
                (ownerAmt * 100).roundToLong()
            assertThat(total).isEqualTo(1000L)
        }
    }

    // ── computeSplit — Shares mode ──────────────────────────────────

    @Nested
    inner class Shares {
        @Test
        fun `computeSplit SHARES mode with includeSelf splits proportionally including owner`() {
            // ₹100, owner 2 shares + alice 1 share + bob 1 share → owner 50, alice 25, bob 25
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.SHARES,
                includeSelfInSplit = true,
                ownerShares = 2,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, shares = 1),
                    SelectedFriend(friend = bob, shares = 1),
                ),
            )
            val (amounts, ownerAmt) = state.split()
            assertThat(ownerAmt).isEqualTo(50.0)
            assertThat(amounts["alice-1"]).isEqualTo(25.0)
            assertThat(amounts["bob-2"]).isEqualTo(25.0)
        }

        @Test
        fun `computeSplit SHARES mode without includeSelf excludes owner from divisor`() {
            // ₹100, includeSelf = false, alice 1, bob 1 → owner 0, alice 50, bob 50
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.SHARES,
                includeSelfInSplit = false,
                ownerShares = 99, // ignored since includeSelf is false
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, shares = 1),
                    SelectedFriend(friend = bob, shares = 1),
                ),
            )
            val (amounts, ownerAmt) = state.split()
            assertThat(ownerAmt).isEqualTo(0.0)
            assertThat(amounts["alice-1"]).isEqualTo(50.0)
            assertThat(amounts["bob-2"]).isEqualTo(50.0)
        }

        @Test
        fun `computeSplit SHARES mode all zero shares returns zeros gracefully`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.SHARES,
                includeSelfInSplit = true,
                ownerShares = 0,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, shares = 0),
                    SelectedFriend(friend = bob, shares = 0),
                ),
            )
            val (amounts, ownerAmt) = state.split()
            assertThat(ownerAmt).isEqualTo(0.0)
            assertThat(amounts["alice-1"]).isEqualTo(0.0)
            assertThat(amounts["bob-2"]).isEqualTo(0.0)
        }

        @Test
        fun `computeSplit SHARES mode paisa-safe with awkward shares`() {
            // ₹100 total, includeSelf = true with 1 share, alice 1 share, bob 1 share = 3 shares
            // 10000 paisa / 3 = 3333 base + 1 leftover paisa
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.SHARES,
                includeSelfInSplit = true,
                ownerShares = 1,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, shares = 1),
                    SelectedFriend(friend = bob, shares = 1),
                ),
            )
            val (amounts, ownerAmt) = state.split()
            val sumPaisa = ((ownerAmt + amounts.values.sum()) * 100).roundToLong()
            assertThat(sumPaisa).isEqualTo(10_000L)
        }
    }

    // ── ownerAmount (via computeSplit) ────────────────────────────────

    @Nested
    inner class OwnerAmount {
        @Test
        fun `owner gets full amount when no friends selected`() {
            val state = FriendsUiState(totalAmount = 500.0, selectedFriends = emptyList())
            val (_, ownerAmt) = state.split()
            assertThat(ownerAmt).isEqualTo(500.0)
        }

        @Test
        fun `owner amount in equal mode with self included`() {
            val state = FriendsUiState(
                totalAmount = 300.0,
                splitMode = SplitMode.EQUAL,
                includeSelfInSplit = true,
                selectedFriends = listOf(SelectedFriend(friend = alice)),
            )
            val (_, ownerAmt) = state.split()
            // 300 / 2 = 150 each
            assertThat(ownerAmt).isEqualTo(150.0)
        }

        @Test
        fun `owner amount is zero when paid for others`() {
            val state = FriendsUiState(
                totalAmount = 300.0,
                splitMode = SplitMode.EQUAL,
                includeSelfInSplit = false,
                selectedFriends = listOf(SelectedFriend(friend = alice)),
            )
            val (_, ownerAmt) = state.split()
            assertThat(ownerAmt).isEqualTo(0.0)
        }
    }

    // ── canProceed ───────────────────────────────────────────────────

    @Nested
    inner class CanProceed {
        @Test
        fun `canProceed false when no friends selected`() {
            val state = FriendsUiState(totalAmount = 100.0, splitMode = SplitMode.SHARES)
            assertThat(state.canProceed).isFalse()
        }

        @Test
        fun `canProceed false when totalAmount is zero`() {
            val state = FriendsUiState(
                totalAmount = 0.0,
                splitMode = SplitMode.SHARES,
                selectedFriends = listOf(SelectedFriend(friend = alice, shares = 1)),
            )
            assertThat(state.canProceed).isFalse()
        }

        @Test
        fun `canProceed false in SHARES mode when all shares zero`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.SHARES,
                includeSelfInSplit = true,
                ownerShares = 0,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, shares = 0),
                    SelectedFriend(friend = bob, shares = 0),
                ),
            )
            assertThat(state.canProceed).isFalse()
        }

        @Test
        fun `canProceed true in SHARES mode when at least one share is positive`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.SHARES,
                includeSelfInSplit = true,
                ownerShares = 0,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, shares = 1),
                ),
            )
            assertThat(state.canProceed).isTrue()
        }

        @Test
        fun `canProceed true in SHARES mode when only owner has shares`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.SHARES,
                includeSelfInSplit = true,
                ownerShares = 2,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, shares = 0),
                ),
            )
            assertThat(state.canProceed).isTrue()
        }

        @Test
        fun `canProceed false in SHARES mode when only owner has shares but includeSelf is off`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.SHARES,
                includeSelfInSplit = false,
                ownerShares = 2,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, shares = 0),
                ),
            )
            assertThat(state.canProceed).isFalse()
        }

        @Test
        fun `canProceed true in EQUAL mode regardless of shares`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.EQUAL,
                selectedFriends = listOf(SelectedFriend(friend = alice, shares = 0)),
            )
            assertThat(state.canProceed).isTrue()
        }
    }
}
