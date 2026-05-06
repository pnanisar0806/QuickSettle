# Share Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `SplitMode.UNEQUAL` (per-friend rupee amounts) with `SplitMode.SHARES` (Splitwise-style integer share counts), where the bill divides proportionally and the existing "I paid for others" toggle applies in both modes.

**Architecture:** Three layers, each in a single Kotlin file. Domain layer gets a new pure use-case method `sharesSplit(totalAmount, sharesByPerson) → Map<id, amount>` using the largest-remainder method for paisa-safe rounding. Presentation state replaces `manualAmount: Double` on `SelectedFriend` with `shares: Int`, adds `ownerShares: Int` to `FriendsUiState`, and wires both into `computeSplit`. UI in `CrewScreen` updates the toggle label/icon, makes the owner row editable in Shares mode, swaps the friend-card decimal field for an integer field (digits only, clamped 0–99), and renders "I paid for others" in both modes.

**Tech Stack:** Kotlin 2.1, Jetpack Compose Material 3, Hilt, JUnit 5 + Google Truth, Gradle wrapper (`./gradlew`).

**Spec:** `docs/superpowers/specs/2026-05-06-share-mode-design.md`

**Files in scope:**

| File | Responsibility | Action |
|---|---|---|
| `app/src/main/java/com/quicksettle/domain/usecase/CalculateSplitUseCase.kt` | Pure split math | Modify (remove `unequalSplit`, add `sharesSplit`) |
| `app/src/test/java/com/quicksettle/domain/usecase/CalculateSplitUseCaseTest.kt` | Unit tests for split math | Modify (remove unequal tests, add shares tests) |
| `app/src/main/java/com/quicksettle/presentation/screens/crew/FriendsViewModel.kt` | Friends-screen state and orchestration | Modify (rename enum, change model, add `ownerShares`, replace API, rewrite `computeSplit` SHARES branch, update `canProceed`, drop `isUnequalOverBudget`) |
| `app/src/test/java/com/quicksettle/presentation/screens/crew/FriendsUiStateTest.kt` | State-extension tests (`computeSplit`, `canProceed`, `suggestions`) | Modify (replace UNEQUAL tests with SHARES tests) |
| `app/src/test/java/com/quicksettle/presentation/screens/crew/FriendsViewModelTest.kt` | ViewModel tests | Modify (rename `setManualAmount` tests to `setShares`, add `setOwnerShares` tests) |
| `app/src/main/java/com/quicksettle/presentation/screens/crew/CrewScreen.kt` | Friends-screen UI | Modify (toggle label/icon, gate "I paid for others", redesign `OwnerCard`, change `FriendCard` editable field, all `UNEQUAL`→`SHARES` references) |

No new files. No changes outside Crew screen + its use case.

---

## Task 1: Add `sharesSplit` to the use case (TDD)

**Files:**
- Modify: `app/src/test/java/com/quicksettle/domain/usecase/CalculateSplitUseCaseTest.kt`
- Modify: `app/src/main/java/com/quicksettle/domain/usecase/CalculateSplitUseCase.kt`

We add the new method first while leaving the old `unequalSplit` and its tests in place. Old code is removed in Task 2 once nothing references it.

- [ ] **Step 1: Write failing tests for `sharesSplit`**

Append the following block to `CalculateSplitUseCaseTest.kt` *immediately before* the closing `}` of the class:

```kotlin
    // ──────────────────────────────────────────────────────────────
    // sharesSplit — exact value checks
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `sharesSplit equal shares exact division 4 people`() {
        // ₹100 with each having 1 share → all 25.00
        val result = useCase.sharesSplit(
            totalAmount = 100.0,
            sharesByPerson = mapOf("a" to 1, "b" to 1, "c" to 1, "d" to 1),
        )
        assertThat(result["a"]).isEqualTo(25.0)
        assertThat(result["b"]).isEqualTo(25.0)
        assertThat(result["c"]).isEqualTo(25.0)
        assertThat(result["d"]).isEqualTo(25.0)
    }

    @Test
    fun `sharesSplit equal shares paisa leftover 3 people`() {
        // ₹100 / 3 shares → totals 100.00; leftover paisa go by remainder rank
        val result = useCase.sharesSplit(
            totalAmount = 100.0,
            sharesByPerson = mapOf("a" to 1, "b" to 1, "c" to 1),
        )
        val sumPaisa = result.values.fold(0L) { acc, v -> acc + (v * 100).roundToLong() }
        assertThat(sumPaisa).isEqualTo(10_000L)
        // Each person gets either 33.33 or 33.34
        result.values.forEach { v ->
            assertThat(v).isAnyOf(33.33, 33.34)
        }
    }

    @Test
    fun `sharesSplit weighted shares exact 120 by 2-1-1`() {
        val result = useCase.sharesSplit(
            totalAmount = 120.0,
            sharesByPerson = mapOf("a" to 2, "b" to 1, "c" to 1),
        )
        assertThat(result["a"]).isEqualTo(60.0)
        assertThat(result["b"]).isEqualTo(30.0)
        assertThat(result["c"]).isEqualTo(30.0)
    }

    @Test
    fun `sharesSplit weighted shares with leftover 100 by 2-1-1`() {
        val result = useCase.sharesSplit(
            totalAmount = 100.0,
            sharesByPerson = mapOf("a" to 2, "b" to 1, "c" to 1),
        )
        assertThat(result["a"]).isEqualTo(50.0)
        assertThat(result["b"]).isEqualTo(25.0)
        assertThat(result["c"]).isEqualTo(25.0)
    }

    @Test
    fun `sharesSplit awkward weights 3-1`() {
        val result = useCase.sharesSplit(
            totalAmount = 100.0,
            sharesByPerson = mapOf("a" to 3, "b" to 1),
        )
        assertThat(result["a"]).isEqualTo(75.0)
        assertThat(result["b"]).isEqualTo(25.0)
    }

    @Test
    fun `sharesSplit awkward weights 2-3`() {
        val result = useCase.sharesSplit(
            totalAmount = 100.0,
            sharesByPerson = mapOf("a" to 2, "b" to 3),
        )
        assertThat(result["a"]).isEqualTo(40.0)
        assertThat(result["b"]).isEqualTo(60.0)
    }

    @Test
    fun `sharesSplit hard rounding 10 by 7 ones`() {
        val result = useCase.sharesSplit(
            totalAmount = 10.0,
            sharesByPerson = mapOf("a" to 1, "b" to 1, "c" to 1, "d" to 1, "e" to 1, "f" to 1, "g" to 1),
        )
        val sumPaisa = result.values.fold(0L) { acc, v -> acc + (v * 100).roundToLong() }
        assertThat(sumPaisa).isEqualTo(1_000L)
        // Every person gets either 1.42 or 1.43
        result.values.forEach { v -> assertThat(v).isAnyOf(1.42, 1.43) }
    }

    @Test
    fun `sharesSplit zero share participant gets zero`() {
        val result = useCase.sharesSplit(
            totalAmount = 100.0,
            sharesByPerson = mapOf("a" to 1, "b" to 0, "c" to 1),
        )
        assertThat(result["a"]).isEqualTo(50.0)
        assertThat(result["b"]).isEqualTo(0.0)
        assertThat(result["c"]).isEqualTo(50.0)
    }

    @Test
    fun `sharesSplit single participant takes all`() {
        val result = useCase.sharesSplit(
            totalAmount = 100.0,
            sharesByPerson = mapOf("a" to 5),
        )
        assertThat(result["a"]).isEqualTo(100.0)
    }

    @Test
    fun `sharesSplit zero amount`() {
        val result = useCase.sharesSplit(
            totalAmount = 0.0,
            sharesByPerson = mapOf("a" to 1, "b" to 2),
        )
        assertThat(result["a"]).isEqualTo(0.0)
        assertThat(result["b"]).isEqualTo(0.0)
    }

    @Test
    fun `sharesSplit throws when total shares is zero`() {
        assertThrows<IllegalArgumentException> {
            useCase.sharesSplit(
                totalAmount = 100.0,
                sharesByPerson = mapOf("a" to 0, "b" to 0),
            )
        }
    }

    @Test
    fun `sharesSplit throws when shares are negative`() {
        assertThrows<IllegalArgumentException> {
            useCase.sharesSplit(
                totalAmount = 100.0,
                sharesByPerson = mapOf("a" to -1, "b" to 2),
            )
        }
    }

    @Test
    fun `sharesSplit throws when total amount is negative`() {
        assertThrows<IllegalArgumentException> {
            useCase.sharesSplit(
                totalAmount = -1.0,
                sharesByPerson = mapOf("a" to 1),
            )
        }
    }

    // ──────────────────────────────────────────────────────────────
    // sharesSplit — paisa invariant property test (200 random configs)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `sharesSplit paisa invariant holds for 200 random configurations`() {
        val rng = Random(seed = 7L)
        repeat(200) {
            val amount = rng.nextDouble(from = 0.01, until = 100_000.0)
                .let { "%.2f".format(it).toDouble() }   // snap to 2dp
            val n = rng.nextInt(from = 2, until = 9)
            val sharesMap = (0 until n).associate { idx ->
                "p$idx" to rng.nextInt(from = 1, until = 10)
            }
            val result = useCase.sharesSplit(totalAmount = amount, sharesByPerson = sharesMap)

            val expectedPaisa = (amount * 100).roundToLong()
            val actualPaisa = result.values.fold(0L) { acc, v -> acc + (v * 100).roundToLong() }
            assertThat(actualPaisa).isEqualTo(expectedPaisa)
        }
    }
```

- [ ] **Step 2: Run tests to verify the new ones fail**

Run: `./gradlew testDebugUnitTest --tests "com.quicksettle.domain.usecase.CalculateSplitUseCaseTest"`
Expected: New `sharesSplit_*` tests fail (compile error: "unresolved reference: sharesSplit"). Existing `equalSplit` and `unequalSplit` tests still pass.

- [ ] **Step 3: Implement `sharesSplit`**

Open `CalculateSplitUseCase.kt`. Add the following method *immediately after* `unequalSplit` (just before the closing `}` of the class). Keep `unequalSplit` in place for now — Task 2 removes it.

```kotlin
    /**
     * Splits [totalAmount] proportionally according to integer share counts in [sharesByPerson].
     *
     * Example: ₹100 with shares {a:2, b:1, c:1} → {a:50.00, b:25.00, c:25.00}.
     *
     * Strategy:
     * - Convert total to paisa (× 100, round to Long).
     * - For each person, base paisa = floor(totalPaisa × shares / totalShares); remainder kept aside.
     * - Distribute leftover paisa one-by-one to people with the largest remainder
     *   (ties broken by stable insertion order). This is the largest-remainder method —
     *   the standard fair-rounding algorithm used by Splitwise and similar apps.
     *
     * Guarantees: sum of returned values equals [totalAmount] exactly (paisa-safe).
     *
     * @throws IllegalArgumentException if [totalAmount] < 0, any share < 0, or sum of shares == 0.
     */
    fun sharesSplit(totalAmount: Double, sharesByPerson: Map<String, Int>): Map<String, Double> {
        require(totalAmount >= 0.0) { "totalAmount must be non-negative" }
        require(sharesByPerson.values.all { it >= 0 }) { "shares must be non-negative" }
        val totalShares = sharesByPerson.values.sum()
        require(totalShares > 0) { "sum of shares must be > 0" }

        val totalPaisa = (totalAmount * 100).roundToLong()

        // Compute base paisa and remainder per person, preserving insertion order.
        data class Allocation(val key: String, var basePaisa: Long, val remainder: Long, val index: Int)
        val allocations = sharesByPerson.entries.mapIndexed { idx, (key, shares) ->
            val numerator = totalPaisa * shares.toLong()
            Allocation(
                key = key,
                basePaisa = numerator / totalShares,
                remainder = numerator % totalShares,
                index = idx,
            )
        }

        val leftoverPaisa = totalPaisa - allocations.sumOf { it.basePaisa }

        // Rank by remainder DESC, ties broken by original index ASC. Take the top N to get +1 paisa each.
        val ranked = allocations.sortedWith(
            compareByDescending<Allocation> { it.remainder }.thenBy { it.index }
        )
        for (i in 0 until leftoverPaisa.toInt()) {
            ranked[i].basePaisa += 1
        }

        // Restore original insertion order on the way out.
        return allocations.associate { it.key to it.basePaisa / 100.0 }
    }
```

- [ ] **Step 4: Run tests to verify everything passes**

Run: `./gradlew testDebugUnitTest --tests "com.quicksettle.domain.usecase.CalculateSplitUseCaseTest"`
Expected: All `sharesSplit_*` tests pass. All existing `equalSplit_*` and `unequalSplit_*` tests still pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/quicksettle/domain/usecase/CalculateSplitUseCase.kt app/src/test/java/com/quicksettle/domain/usecase/CalculateSplitUseCaseTest.kt
git commit -m "feat(domain): add sharesSplit use case for proportional bill division"
```

---

## Task 2: Switch ViewModel state and `computeSplit` to Shares mode

**Files:**
- Modify: `app/src/main/java/com/quicksettle/presentation/screens/crew/FriendsViewModel.kt`

This task changes the in-memory model and the use-case wiring. The UI still references the old API (`setManualAmount`, `manualAmount`, `SplitMode.UNEQUAL`) so the build will break temporarily — Task 5 fixes the UI. Tests are migrated in Tasks 3 and 4.

- [ ] **Step 1: Replace the file's contents**

Overwrite `FriendsViewModel.kt` with the content below. Key differences from the current file:

- `enum class SplitMode { EQUAL, UNEQUAL }` → `EQUAL, SHARES`
- `SelectedFriend.manualAmount: Double` → `shares: Int`
- New `FriendsUiState.ownerShares: Int = 0`
- `isUnequalOverBudget` extension is deleted
- `computeSplit` UNEQUAL branch is replaced with the SHARES branch
- `canProceed` SHARES rule: at least one share > 0
- `setManualAmount` → `setShares`; new `setOwnerShares`
- `OWNER_KEY` private const

```kotlin
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
```

- [ ] **Step 2: Confirm scope of breakage**

Run: `./gradlew compileDebugKotlin`
Expected: FAILS — `CrewScreen.kt` and existing tests still reference `SplitMode.UNEQUAL`, `manualAmount`, `setManualAmount`, `isUnequalOverBudget`. That's expected; we'll fix tests next (Task 3, 4) and UI (Task 5). Do NOT commit yet — the build is broken until the dependents are migrated.

- [ ] **Step 3: Hold the commit**

No commit at the end of this task. The next two tasks update the tests; we commit ViewModel + tests together after Task 4.

---

## Task 3: Migrate `FriendsUiStateTest` to SHARES

**Files:**
- Modify: `app/src/test/java/com/quicksettle/presentation/screens/crew/FriendsUiStateTest.kt`

- [ ] **Step 1: Read the current file to know which UNEQUAL tests exist**

Run: `./gradlew compileDebugUnitTestKotlin 2>&1 | head -40` — read the compile errors. They tell you which lines reference `manualAmount`, `SplitMode.UNEQUAL`, `isUnequalOverBudget`. Cross-reference with the file.

- [ ] **Step 2: Replace UNEQUAL test cases with SHARES test cases**

In `FriendsUiStateTest.kt`:

1. Find every `nested class` block named `Unequal` (or similar) and rename to `Shares`.
2. Replace every `SplitMode.UNEQUAL` with `SplitMode.SHARES`.
3. Replace `manualAmount = X` parameter on `SelectedFriend(...)` with `shares = N` (translate the test intent: a manual amount of `200.0` out of `500.0` becomes "this friend has 2 shares out of 5 total" — but for migration we don't care about exact equivalence; rewrite each test to test the SHARES contract).
4. Delete any test of `isUnequalOverBudget`.
5. Add the cases below if not already covered.

Add (or modify existing tests to cover) these scenarios in a `Shares` `@Nested inner class`:

```kotlin
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
```

Make sure:
- Any preexisting `Unequal` `@Nested inner class` is deleted entirely (don't leave dead tests).
- Any preexisting `CanProceed` block is replaced wholesale by the new one above.
- Equal-mode tests are left untouched.
- Add `import kotlin.math.roundToLong` if not already imported (check the top of the file).

- [ ] **Step 3: Hold the commit**

Same as Task 2 — do not commit yet.

---

## Task 4: Migrate `FriendsViewModelTest` to SHARES

**Files:**
- Modify: `app/src/test/java/com/quicksettle/presentation/screens/crew/FriendsViewModelTest.kt`

- [ ] **Step 1: Find every breakage in the file**

Run: `./gradlew compileDebugUnitTestKotlin 2>&1 | grep FriendsViewModelTest`
Read the compile errors. Common ones: `setManualAmount` references, `manualAmount = X` in `SelectedFriend(...)`, `SplitMode.UNEQUAL`, any test class named `Unequal*`.

- [ ] **Step 2: Apply the migration**

Walk through each compile error:

- Replace `viewModel.setManualAmount(friendId, amountDouble)` calls with `viewModel.setShares(friendId, sharesInt)`. Translate the intent — a test that previously asserted "alice's manualAmount = 200.0" becomes "alice's shares = 2" (or any positive integer). The numeric value doesn't need to match the old amount; the test's purpose is to verify the setter works.
- `SplitMode.UNEQUAL` → `SplitMode.SHARES`.
- `manualAmount = X` parameter on `SelectedFriend(...)` → `shares = N`.
- Rename any `nested class Unequal` blocks to `Shares` (or appropriate names).

Then add the following two test classes near the end of the existing test class (just before the outer closing `}`):

```kotlin
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
```

- [ ] **Step 3: Run the unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: All tests pass. (The Compose UI is still broken — but `testDebugUnitTest` doesn't compile main UI code; it only compiles `unitTest` against the changed ViewModel.) If you see "unresolved reference: setManualAmount" or "manualAmount" errors, you missed a spot — find and fix.

> **Note:** If `testDebugUnitTest` *does* fail to compile because `app/src/main` depends on `CrewScreen.kt`, run `./gradlew :app:compileDebugUnitTestKotlin` instead — it isolates the unit-test compilation.

- [ ] **Step 4: Commit ViewModel + tests together**

```bash
git add app/src/main/java/com/quicksettle/presentation/screens/crew/FriendsViewModel.kt \
        app/src/test/java/com/quicksettle/presentation/screens/crew/FriendsUiStateTest.kt \
        app/src/test/java/com/quicksettle/presentation/screens/crew/FriendsViewModelTest.kt
git commit -m "feat(crew): replace UNEQUAL split mode with SHARES (state + tests)"
```

---

## Task 5: Update `CrewScreen.kt` UI

**Files:**
- Modify: `app/src/main/java/com/quicksettle/presentation/screens/crew/CrewScreen.kt`

This is the largest UI change but it's mostly mechanical search-and-replace plus three localized rewrites: the toggle's "Unequal" option, the `OwnerCard`, and the editable input inside `FriendCard`.

- [ ] **Step 1: Update the second `ToggleOption` in `SplitModeToggle`**

Find the block currently labelled `"Unequal"` (around line 333–349 in the current file). Replace the entire `ToggleOption(...)` call with:

```kotlin
            // Shares
            ToggleOption(
                label = "Shares",
                icon = {
                    Icon(
                        imageVector = Icons.Filled.PieChart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (splitMode == SplitMode.SHARES)
                            MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                selected = splitMode == SplitMode.SHARES,
                onClick = { if (splitMode != SplitMode.SHARES) onToggle() },
                modifier = Modifier.weight(1f),
            )
```

Then update imports near the top of the file:

- Replace `import androidx.compose.material.icons.filled.DragHandle` with `import androidx.compose.material.icons.filled.PieChart`.

- [ ] **Step 2: Render "I paid for others" in both modes**

Find this block in the `LazyColumn` body (around line 109):

```kotlin
            // ── Equal mode: "Paid for others" toggle ────────────────────────────
            if (uiState.splitMode == SplitMode.EQUAL) {
                item {
                    PaidForOthersToggle(
                        includeSelf = uiState.includeSelfInSplit,
                        onToggle = viewModel::toggleIncludeSelf,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
            }
```

Replace with (drop the gate):

```kotlin
            // ── "Paid for others" toggle (both modes) ───────────────────────────
            item {
                PaidForOthersToggle(
                    includeSelf = uiState.includeSelfInSplit,
                    onToggle = viewModel::toggleIncludeSelf,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
```

- [ ] **Step 3: Replace the `OwnerCard` invocation block**

Find this block (around line 119):

```kotlin
            // ── Unequal mode: "You" card (shows auto-calculated remainder) ─────
            if (uiState.splitMode == SplitMode.UNEQUAL && uiState.selectedFriends.isNotEmpty()) {
                item {
                    OwnerCard(
                        selfAmount = ownerAmt,
                        isOverBudget = uiState.isUnequalOverBudget,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
            }
```

Replace with:

```kotlin
            // ── Shares mode: editable "You" row (only when included in split) ──
            if (uiState.splitMode == SplitMode.SHARES &&
                uiState.includeSelfInSplit &&
                uiState.selectedFriends.isNotEmpty()
            ) {
                item {
                    OwnerCard(
                        selfAmount = ownerAmt,
                        ownerShares = uiState.ownerShares,
                        onOwnerSharesChange = viewModel::setOwnerShares,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
            }
```

- [ ] **Step 4: Update the `FriendCard` invocation in the items loop**

Find this block (around line 170):

```kotlin
                    FriendCard(
                        friend = friend,
                        isSelected = isSelected,
                        amount = friendAmount,
                        splitMode = uiState.splitMode,
                        manualAmount = uiState.selectedFriends
                            .find { it.friend.id == friend.id }?.manualAmount ?: 0.0,
                        onManualAmountChange = { viewModel.setManualAmount(friend.id, it) },
                        onClick = { viewModel.toggleFriend(friend.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
```

Replace with:

```kotlin
                    FriendCard(
                        friend = friend,
                        isSelected = isSelected,
                        amount = friendAmount,
                        splitMode = uiState.splitMode,
                        shares = uiState.selectedFriends
                            .find { it.friend.id == friend.id }?.shares ?: 0,
                        onSharesChange = { viewModel.setShares(friend.id, it) },
                        onClick = { viewModel.toggleFriend(friend.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
```

- [ ] **Step 5: Rewrite the `OwnerCard` composable**

Find the existing `OwnerCard` composable (starts around line 476, marked by the `// ── Owner "You" Card (Unequal Mode — read-only, shows remainder) ──` comment) and replace the entire composable, including its banner comment, with:

```kotlin
// ── Owner "You" Row (Shares Mode — editable share count, live amount) ───────

@Composable
private fun OwnerCard(
    selfAmount: Double,
    ownerShares: Int,
    onOwnerSharesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0F002114),
                spotColor = Color(0x0F002114),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left pill — always active for "You"
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(
                        color = PrimaryFixed,
                        shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                    ),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryFixed),
            ) {
                Text(
                    text = "Y",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ManropeBold,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Primary,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "You",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ManropeBold,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "₹${formatAmount(selfAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Editable share count
            SharesField(
                shares = ownerShares,
                onSharesChange = onOwnerSharesChange,
            )
        }
    }
}
```

- [ ] **Step 6: Add a private `SharesField` composable**

The `OwnerCard` (Step 5) and the `FriendCard` editable branch (Step 7) both need an integer-only field that clamps to 0–99. Add this composable in `CrewScreen.kt` near the bottom of the file (just before the closing of the file, after the last existing composable):

```kotlin
// ── Shares Input (integer-only, 0..99) ───────────────────────────────────────

@Composable
private fun SharesField(
    shares: Int,
    onSharesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var textValue by remember(shares) {
        mutableStateOf(if (shares == 0) "" else shares.toString())
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = textValue,
            onValueChange = { newValue ->
                val digitsOnly = newValue.filter { it.isDigit() }.take(2) // max 2 digits → 99
                val parsed = digitsOnly.toIntOrNull() ?: 0
                val clamped = parsed.coerceIn(0, 99)
                textValue = if (clamped == 0) digitsOnly else clamped.toString()
                onSharesChange(clamped)
            },
            modifier = modifier
                .width(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            textStyle = TextStyle(
                fontFamily = ManropeBold,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
            ),
            decorationBox = { innerTextField ->
                if (textValue.isEmpty()) {
                    Text(
                        text = "0",
                        style = TextStyle(
                            fontFamily = ManropeBold,
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                innerTextField()
            },
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "shares",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

- [ ] **Step 7: Update `FriendCard` to use shares input**

Find the `FriendCard` composable (currently around line 603). Update its parameter list:

Replace:
```kotlin
private fun FriendCard(
    friend: Friend,
    isSelected: Boolean,
    amount: Double?,
    splitMode: SplitMode,
    manualAmount: Double,
    onManualAmountChange: (Double) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
```

With:
```kotlin
private fun FriendCard(
    friend: Friend,
    isSelected: Boolean,
    amount: Double?,
    splitMode: SplitMode,
    shares: Int,
    onSharesChange: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
```

Then find the inner block that renders the editable amount field — currently:

```kotlin
            // Amount + selection indicator
            Column(horizontalAlignment = Alignment.End) {
                if (isSelected && splitMode == SplitMode.UNEQUAL) {
                    // Editable amount field for unequal mode
                    var textValue by remember(friend.id) {
                        mutableStateOf(
                            if (manualAmount == 0.0) "" else manualAmount.toBigDecimal().stripTrailingZeros().toPlainString()
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹",
                            ...
                        )
                        BasicTextField(
                            value = textValue,
                            onValueChange = { newValue ->
                                val filtered = newValue.filter { it.isDigit() || it == '.' }
                                val parts = filtered.split(".")
                                val valid = when {
                                    parts.size > 2 -> false
                                    parts.size == 2 && parts[1].length > 2 -> false
                                    else -> true
                                }
                                if (valid) {
                                    textValue = filtered
                                    val parsed = filtered.toDoubleOrNull() ?: 0.0
                                    onManualAmountChange(parsed)
                                }
                            },
                            ...
                        )
                    }
                } else if (isSelected && amount != null) {
                    Text(text = "₹${formatAmount(amount)}", ...)
                }
                ...
            }
```

Replace the entire `Column(horizontalAlignment = Alignment.End) { ... }` block with the version below. The shares mode now shows BOTH the editable shares field AND the live calculated amount underneath:

```kotlin
            // Right side: in SHARES mode show shares input + live amount; in EQUAL mode show amount only
            Column(horizontalAlignment = Alignment.End) {
                if (isSelected && splitMode == SplitMode.SHARES) {
                    SharesField(
                        shares = shares,
                        onSharesChange = onSharesChange,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${formatAmount(amount ?: 0.0)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = ManropeBold,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (isSelected && amount != null) {
                    Text(
                        text = "₹${formatAmount(amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = ManropeBold,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isSelected) {
                    // Green checkmark
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Primary),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = OnPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
```

> **Note on the checkmark block:** the existing file places the green checkmark inside the same outer `Column`. The replacement above preserves that. If your local file has the checkmark in a different place, leave that part as-is and only swap the inner `if/else` chain that picks between `SHARES` editable field and `EQUAL` read-only amount.

- [ ] **Step 8: Compile and run all tests**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL. All unit tests pass. No `UNEQUAL`, `manualAmount`, `setManualAmount`, `isUnequalOverBudget`, or `DragHandle` references remain.

- [ ] **Step 9: Verify no stale references**

Run: `./gradlew compileDebugKotlin compileDebugUnitTestKotlin`
Run a search:
```
git grep -nE "UNEQUAL|manualAmount|setManualAmount|isUnequalOverBudget|DragHandle" -- 'app/'
```
Expected: zero hits. (If any file under `app/` still references these, fix it. The DragHandle import stays in scope only if some other component uses it — verify the only hit was the toggle's icon, which we replaced.)

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/quicksettle/presentation/screens/crew/CrewScreen.kt
git commit -m "feat(crew): wire Shares mode UI — editable owner row, shares field, label/icon"
```

---

## Task 6: Manual sanity check + update CLAUDE.md

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Manually run the app**

Build and install:
```
./gradlew installDebug
```
Then on device/emulator:
- Enter an amount (₹500) and a description.
- Tap Add Friends.
- Toggle the split mode to "Shares". Confirm the second tab now reads "Shares" with a pie-chart icon.
- Confirm the "I paid for others" toggle is visible in Shares mode (was previously hidden).
- Select 2 friends. Both `FriendCard`s show a small `[0] shares` field on the right. Tap into one, type `2`. The right-side amount under the field updates immediately.
- Confirm a "You" row appears at the top (since `includeSelfInSplit` defaults to true). Type `1` shares for yourself. Sum of all amounts equals ₹500 exactly.
- Toggle "I paid for others" off. The "You" row disappears. Friends absorb the full ₹500 proportional to their shares.
- Toggle it back on. The "You" row reappears with the previously typed `1` share.
- Set every share to 0. The "Go to Settle" CTA becomes disabled. Set one back to ≥1. CTA enables.
- Switch back to "Equal" tab. Owner-shares value is preserved internally; equal split renders normally.

- [ ] **Step 2: Update `CLAUDE.md` line 86**

Open `CLAUDE.md`. Find this line under "Screens":

```
2. **Select Friends** (Friends tab): Total bill header + Equal/Unequal toggle (Share mode PLANNED TODO) + Frequent Friends list with avatars + Suggestions section + "Go to Settle" CTA
```

Replace with:

```
2. **Select Friends** (Friends tab): Total bill header + Equal/Shares toggle + "I paid for others" toggle + editable "You" row (Shares mode) + Frequent Friends list with avatars + Suggestions section + "Go to Settle" CTA
```

- [ ] **Step 3: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: replace Equal/Unequal toggle reference with Equal/Shares in CLAUDE.md"
```

- [ ] **Step 4: Update the project phase checklist**

Open `.claude/memory/project_phase_checklist.md`. Under Phase 4 — Friends Screen, add a line:

```
- [x] Share mode (replaces Unequal) — integer share counts per participant, paisa-safe largest-remainder split, "I paid for others" applies in both modes (verified 2026-05-06)
```

(Place it inside the existing Phase 4 list, before the existing closing block. Do not modify other phases.)

Then commit:

```bash
git add .claude/memory/project_phase_checklist.md
git commit -m "docs(memory): record Share mode completion in phase checklist"
```

---

## Self-review (writing-plans)

**Spec coverage:**
- Replace `SplitMode.UNEQUAL` with `SplitMode.SHARES` → Task 2 (enum), Task 5 (UI references).
- `SelectedFriend.shares: Int` field → Task 2.
- `FriendsUiState.ownerShares: Int` → Task 2.
- `CalculateSplitUseCase.sharesSplit` (largest-remainder, paisa-safe) → Task 1.
- Use case throws on `sum=0`, negative shares, negative total → Task 1 (tests + `require` checks).
- "I paid for others" applies in both modes → Task 5 Step 2.
- Owner editable row in Shares mode (hidden when toggle off) → Task 5 Steps 3, 5.
- Friend card integer field with 0–99 clamp, digits-only filter → Task 5 Steps 6, 7.
- "Your share" pill stays Equal-only → unchanged in Task 5 (no edit to that block).
- `canProceed` requires sum-of-shares > 0 in Shares mode → Task 2 (state) + Task 3 (tests).
- Tests: 13 use-case tests including 200-iter paisa invariant → Task 1.
- Tests: ViewModel + UiState shares coverage → Tasks 3, 4.
- CLAUDE.md update → Task 6.

**Placeholder scan:** No "TBD"/"TODO"/"implement later"/"add validation". The two notes in Task 5 (about local file variations) are explicit defensive guidance, not placeholders.

**Type consistency:**
- `setShares(friendId: String, shares: Int)` — used identically in ViewModel (Task 2), tests (Task 4), CrewScreen wiring (Task 5 Step 4). ✓
- `setOwnerShares(shares: Int)` — defined Task 2, tested Task 4, wired Task 5 Step 3. ✓
- `OwnerCard(selfAmount, ownerShares, onOwnerSharesChange, modifier)` — defined Task 5 Step 5, called with the same kwargs Task 5 Step 3. ✓
- `FriendCard(friend, isSelected, amount, splitMode, shares, onSharesChange, onClick, modifier)` — defined Task 5 Step 7, called with the same kwargs Task 5 Step 4. ✓
- `SharesField(shares: Int, onSharesChange: (Int) -> Unit, modifier)` — defined Task 5 Step 6, called Task 5 Step 5 (without explicit modifier — uses the default), Task 5 Step 7 (without explicit modifier — uses the default). ✓
- `SplitMode.SHARES` everywhere; no stragglers. ✓
- `OWNER_KEY` private to `FriendsViewModel.kt`; not referenced from tests (tests only check returned `Map<String, Double>` values via friend ids and the `ownerAmt` return value — no need to know the internal key). ✓
