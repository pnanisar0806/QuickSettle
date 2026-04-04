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

    // ── computeSplit — Unequal mode ──────────────────────────────────

    @Nested
    inner class UnequalSplitAmounts {
        @Test
        fun `unequal split returns manual amounts`() {
            val state = FriendsUiState(
                totalAmount = 500.0,
                splitMode = SplitMode.UNEQUAL,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, manualAmount = 200.0),
                    SelectedFriend(friend = bob, manualAmount = 150.0),
                ),
            )
            val (amounts, _) = state.split()
            assertThat(amounts["alice-1"]).isEqualTo(200.0)
            assertThat(amounts["bob-2"]).isEqualTo(150.0)
        }

        @Test
        fun `unequal split with zero amounts returns zeros`() {
            val state = FriendsUiState(
                totalAmount = 500.0,
                splitMode = SplitMode.UNEQUAL,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, manualAmount = 0.0),
                ),
            )
            val (amounts, _) = state.split()
            assertThat(amounts["alice-1"]).isEqualTo(0.0)
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

        @Test
        fun `owner amount in unequal mode is remainder`() {
            val state = FriendsUiState(
                totalAmount = 500.0,
                splitMode = SplitMode.UNEQUAL,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, manualAmount = 200.0),
                    SelectedFriend(friend = bob, manualAmount = 150.0),
                ),
            )
            val (_, ownerAmt) = state.split()
            // 500 - 200 - 150 = 150
            assertThat(ownerAmt).isEqualTo(150.0)
        }

        @Test
        fun `owner amount in unequal when friends total equals bill`() {
            val state = FriendsUiState(
                totalAmount = 300.0,
                splitMode = SplitMode.UNEQUAL,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, manualAmount = 150.0),
                    SelectedFriend(friend = bob, manualAmount = 150.0),
                ),
            )
            val (_, ownerAmt) = state.split()
            assertThat(ownerAmt).isEqualTo(0.0)
        }

        @Test
        fun `owner amount negative when friends exceed total`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.UNEQUAL,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, manualAmount = 80.0),
                    SelectedFriend(friend = bob, manualAmount = 50.0),
                ),
            )
            val (_, ownerAmt) = state.split()
            // 100 - 130 = -30
            assertThat(ownerAmt).isEqualTo(-30.0)
        }
    }

    // ── isUnequalOverBudget ──────────────────────────────────────────

    @Nested
    inner class OverBudget {
        @Test
        fun `not over budget in equal mode`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.EQUAL,
                selectedFriends = listOf(SelectedFriend(friend = alice)),
            )
            assertThat(state.isUnequalOverBudget).isFalse()
        }

        @Test
        fun `not over budget when within total`() {
            val state = FriendsUiState(
                totalAmount = 500.0,
                splitMode = SplitMode.UNEQUAL,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, manualAmount = 200.0),
                    SelectedFriend(friend = bob, manualAmount = 200.0),
                ),
            )
            assertThat(state.isUnequalOverBudget).isFalse()
        }

        @Test
        fun `over budget when amounts exceed total`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.UNEQUAL,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, manualAmount = 60.0),
                    SelectedFriend(friend = bob, manualAmount = 60.0),
                ),
            )
            assertThat(state.isUnequalOverBudget).isTrue()
        }

        @Test
        fun `not over budget when amounts exactly equal total`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.UNEQUAL,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, manualAmount = 50.0),
                    SelectedFriend(friend = bob, manualAmount = 50.0),
                ),
            )
            assertThat(state.isUnequalOverBudget).isFalse()
        }

        @Test
        fun `over budget by 1 paisa`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.UNEQUAL,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, manualAmount = 100.01),
                ),
            )
            assertThat(state.isUnequalOverBudget).isTrue()
        }
    }

    // ── canProceed ───────────────────────────────────────────────────

    @Nested
    inner class CanProceed {
        @Test
        fun `cannot proceed with no friends`() {
            val state = FriendsUiState(totalAmount = 100.0, selectedFriends = emptyList())
            assertThat(state.canProceed).isFalse()
        }

        @Test
        fun `cannot proceed with zero total`() {
            val state = FriendsUiState(
                totalAmount = 0.0,
                selectedFriends = listOf(SelectedFriend(friend = alice)),
            )
            assertThat(state.canProceed).isFalse()
        }

        @Test
        fun `can proceed in equal mode with friends and amount`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.EQUAL,
                selectedFriends = listOf(SelectedFriend(friend = alice)),
            )
            assertThat(state.canProceed).isTrue()
        }

        @Test
        fun `cannot proceed in unequal mode when no amounts assigned`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.UNEQUAL,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, manualAmount = 0.0),
                ),
            )
            assertThat(state.canProceed).isFalse()
        }

        @Test
        fun `can proceed in unequal mode when amounts within total`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.UNEQUAL,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, manualAmount = 50.0),
                ),
            )
            assertThat(state.canProceed).isTrue()
        }

        @Test
        fun `cannot proceed in unequal mode when amounts exceed total`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.UNEQUAL,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, manualAmount = 60.0),
                    SelectedFriend(friend = bob, manualAmount = 60.0),
                ),
            )
            assertThat(state.canProceed).isFalse()
        }

        @Test
        fun `can proceed in unequal mode when amounts exactly equal total`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.UNEQUAL,
                selectedFriends = listOf(
                    SelectedFriend(friend = alice, manualAmount = 50.0),
                    SelectedFriend(friend = bob, manualAmount = 50.0),
                ),
            )
            assertThat(state.canProceed).isTrue()
        }

        @Test
        fun `can proceed in equal mode with self excluded`() {
            val state = FriendsUiState(
                totalAmount = 100.0,
                splitMode = SplitMode.EQUAL,
                includeSelfInSplit = false,
                selectedFriends = listOf(SelectedFriend(friend = alice)),
            )
            assertThat(state.canProceed).isTrue()
        }
    }
}
