# Share Mode (Split-by-Shares) — Design Spec

**Date:** 2026-05-06
**Author:** Anirban Sarkar (with Claude)
**Status:** Approved — ready for implementation plan
**Replaces:** The existing `SplitMode.UNEQUAL` (manual rupee amounts per friend)

## Problem

The Friends screen currently offers two split modes:

- **Equal** — divides the bill evenly among selected participants (paisa-safe).
- **Unequal** — each friend types a fixed rupee amount; the owner takes the remainder.

Unequal mode is awkward in practice: users have to do mental arithmetic ("I want this to be 1/3 vs 2/3 — what does that work out to?") and there is constant risk of over-budget input. The pending enhancement (`CLAUDE.md` line 86: "Equal/Unequal toggle (Share mode PLANNED TODO)") replaces Unequal with a Splitwise-style **Shares** mode where each participant declares an integer number of shares and the bill is divided proportionally.

## Goals

1. Replace `SplitMode.UNEQUAL` with `SplitMode.SHARES` — the toggle remains a 2-way choice (Equal | Shares).
2. Each participant (the owner and every selected friend) has an integer share count, starting at `0`.
3. The bill is divided proportionally across the total share count, paisa-safe (sum of all amounts equals the total exactly).
4. The existing "I paid for others" toggle (`includeSelfInSplit`) applies in Shares mode as well: when off, the owner has 0 shares and friends absorb the whole bill.
5. Maintain the architectural invariants: split math lives only in `CalculateSplitUseCase`; the ViewModel orchestrates state; Composables are stateless.

## Non-Goals

- Decimal/fractional shares (e.g., `1.5`). Integers only for v1; revisit later if user feedback asks.
- A third mode that keeps Unequal alongside Shares. The user explicitly requested replacement.
- Migration of any persisted UNEQUAL state — none exists; `SplitMode` is held only in `FriendsUiState` (in-memory) and resets on every new bill.
- Saving "preferred shares" per friend across sessions. Each session starts fresh.

## User-Facing Behavior

### Toggle

The split-mode toggle continues to render two options. The second option's label changes from "Unequal" to **"Shares"**, and its icon switches to a share-suggesting glyph (e.g., `Icons.Filled.PieChart`). The "I paid for others" toggle, currently rendered only in Equal mode, now renders in **both** modes.

### Owner row (in Shares mode)

When `splitMode == SHARES` and `includeSelfInSplit == true`, an editable "You" row appears at the top of the participant list (the same slot currently used by the read-only `OwnerCard` in Unequal mode). The row shows:

- "You" label and the owner's avatar initial.
- An integer text field bound to `ownerShares`. Placeholder `0`. Numeric keyboard. Digits only.
- The live calculated rupee amount on the right.

When `includeSelfInSplit == false`, the row is hidden — friends absorb the entire bill in proportion to their shares.

### Friend cards (in Shares mode)

Each `FriendCard` for a selected friend shows an integer shares field where the decimal `manualAmount` field used to be:

- `KeyboardType.Number` (no decimal).
- Filtered to digits only.
- Clamped to `0`–`99` to keep the UI compact.
- Live calculated rupee amount on the right (uses the existing right-side amount slot).

### "Your share" info pill

The Equal-mode-only pill ("Your share: ₹X" or "splitting among friends only") is unchanged. It does not appear in Shares mode because the editable owner row already shows the live amount.

### CTA gating

The glassmorphism "Go to Settle" CTA is enabled only when:

- At least one friend is selected, AND
- `totalAmount > 0`, AND
- (Shares mode only) the **sum of all share counts > 0** (i.e., at least one participant has a non-zero share).

## Architecture

### Domain layer — `CalculateSplitUseCase`

**Remove:**

```kotlin
fun unequalSplit(totalAmount: Double, fixedAmounts: Map<String, Double>): Double
```

**Add:**

```kotlin
fun sharesSplit(totalAmount: Double, sharesByPerson: Map<String, Int>): Map<String, Double>
```

#### Contract

- Preconditions:
    - `totalAmount >= 0.0`
    - every `shares >= 0`
    - `sum(shares) > 0`
- Throws `IllegalArgumentException` on any precondition violation.
- Returns a `Map<String, Double>` with the same keys as the input, each value rounded to two decimals.
- **Paisa invariant:** the sum of returned values equals `totalAmount` exactly (zero leftover, zero overage).

#### Algorithm

1. `totalPaisa = round(totalAmount * 100)` as `Long`.
2. `totalShares = sum(sharesByPerson.values)`.
3. For each person, compute:
    - `numerator = totalPaisa.toLong() * shares.toLong()` (avoid Int overflow on large bills).
    - `basePaisa = numerator / totalShares`.
    - `remainder = numerator % totalShares` (kept aside for ranking).
4. `leftoverPaisa = totalPaisa - sum(basePaisa)`.
5. Distribute `leftoverPaisa` one paisa at a time to participants ranked by descending `remainder` (ties broken by stable insertion order over the input map).
6. Convert each person's paisa back to `Double` (`/100.0`) and return.

#### Why remainder-based distribution

The `equalSplit` strategy of "first N participants get +1 paisa" works because every share is equal. With weighted shares it would unfairly favor low-share participants. Distributing leftover paisa to the participants whose ideal share has the largest fractional remainder is the canonical "largest-remainder method" used by Splitwise and matches what users expect when they sanity-check the math.

#### Edge cases

- **Zero shares for a person** — that person gets `0.00`. They are excluded from leftover distribution because their `remainder` is `0`.
- **All zero shares** — throws `IllegalArgumentException`. The ViewModel's `canProceed` guard prevents reaching the use case in this state, but the throw is a defensive contract guarantee.
- **`totalAmount == 0.0`** — every participant receives `0.00`.
- **Single participant with positive shares** — that participant receives the entire `totalAmount`.

### Presentation layer — `FriendsViewModel.kt`

**Enum rename:**

```kotlin
enum class SplitMode { EQUAL, SHARES }   // was: EQUAL, UNEQUAL
```

**Model change:**

```kotlin
data class SelectedFriend(
    val friend: Friend,
    val shares: Int = 0,            // was: manualAmount: Double = 0.0
)
```

**State change:**

```kotlin
data class FriendsUiState(
    // ... existing fields ...
    val ownerShares: Int = 0,       // NEW
)
```

**Removed extension:** `isUnequalOverBudget` is deleted — over-budget cannot occur in Shares mode (the use case always splits the actual `totalAmount`).

**`computeSplit` — SHARES branch (replaces UNEQUAL branch):**

```kotlin
SplitMode.SHARES -> {
    val sharesMap = buildMap {
        selectedFriends.forEach { put(it.friend.id, it.shares) }
        if (includeSelfInSplit) put(OWNER_KEY, ownerShares)
    }
    val totalShares = sharesMap.values.sum()
    if (totalShares <= 0) {
        return selectedFriends.associate { it.friend.id to 0.0 } to 0.0
    }
    val amounts = useCase.sharesSplit(totalAmount, sharesMap)
    val friendAmounts = selectedFriends.associate {
        it.friend.id to (amounts[it.friend.id] ?: 0.0)
    }
    val ownerAmt = if (includeSelfInSplit) amounts[OWNER_KEY] ?: 0.0 else 0.0
    friendAmounts to ownerAmt
}
```

`OWNER_KEY` is a private file-level `const val OWNER_KEY = "__owner__"`. UUIDs cannot start with two underscores so collision is structurally impossible.

**`canProceed` — SHARES branch:**

- `selectedFriends.isNotEmpty()` AND
- `totalAmount > 0` AND
- `selectedFriends.sumOf { it.shares } + (if (includeSelfInSplit) ownerShares else 0) > 0`

**ViewModel API changes:**

| Before | After |
|---|---|
| `setManualAmount(friendId: String, amount: Double)` | `setShares(friendId: String, shares: Int)` (clamps negatives to `0`) |
| — | `setOwnerShares(shares: Int)` (clamps negatives to `0`) |
| `toggleSplitMode()` | unchanged in shape — still flips between the two modes |

When toggling between Equal and Shares, existing share values are retained on the state (so users can flip back-and-forth without losing input). `manualAmount` is gone, so there is nothing to migrate.

### UI layer — `CrewScreen.kt`

**1. `SplitModeToggle`:**
- Second option label: "Unequal" → "Shares".
- Icon: `Icons.Filled.DragHandle` → a share-suggesting icon (e.g., `Icons.Filled.PieChart`); final pick during implementation.

**2. "I paid for others" toggle:**
- Currently gated by `splitMode == EQUAL`. Move the gate so it renders in both modes.
- Composable itself unchanged.

**3. `OwnerCard`:**
- The `isOverBudget` parameter and red-warning UI are deleted.
- New parameters: `ownerShares: Int`, `onOwnerSharesChange: (Int) -> Unit`.
- When `splitMode == SHARES && includeSelfInSplit`: shows "You" + initial avatar + integer text field + live amount.
- When `splitMode == SHARES && !includeSelfInSplit`: card is not rendered.

**4. `FriendCard`:**
- Parameter renames: `manualAmount: Double` → `shares: Int`; `onManualAmountChange: (Double) -> Unit` → `onSharesChange: (Int) -> Unit`.
- The `BasicTextField` switches to `KeyboardType.Number`, digits-only filter, clamped to `0`–`99`, placeholder `0`.
- Right-side live amount slot is unchanged (still displays `amount` from `computeSplit`).

**5. "Your share" info pill** — unchanged (Equal-mode only).

**6. Top-level `CrewScreen` bindings:**
- Every `splitMode == SplitMode.UNEQUAL` check becomes `splitMode == SplitMode.SHARES`.
- `viewModel.setManualAmount(friend.id, it)` → `viewModel.setShares(friend.id, it)`.
- New: `viewModel::setOwnerShares` and `uiState.ownerShares` are passed into the owner row.

**Out of scope:** `CrewHeader`, `SectionHeader`, `SuggestionCard`, the glassmorphism CTA bar, navigation, and all other screens.

## Data Flow

```
User types share count
        ↓
CrewScreen calls viewModel.setShares(id, n) or setOwnerShares(n)
        ↓
FriendsViewModel updates _uiState (clamps negatives to 0)
        ↓
Composable recomposes; calls viewModel.currentSplit()
        ↓
FriendsViewModel delegates to FriendsUiState.computeSplit(useCase)
        ↓
computeSplit (SHARES branch) builds Map<String, Int> and calls
CalculateSplitUseCase.sharesSplit(totalAmount, sharesMap)
        ↓
Use case returns Map<String, Double> (paisa-exact)
        ↓
ViewModel returns (friendAmounts, ownerAmt) → UI renders rupee amounts inline
```

The use case knows nothing about `SelectedFriend`, `FriendsUiState`, owner identity, or the `__owner__` key convention. It is a pure `(Double, Map<String, Int>) → Map<String, Double>` function and is fully unit-testable in isolation.

## Error Handling

| Situation | Where caught | Behavior |
|---|---|---|
| User types non-digit | UI input filter | Character is dropped before reaching state. |
| User types value > 99 | UI clamp | Clamped to 99 before reaching state. |
| User pastes negative | ViewModel `setShares` | Clamped to 0. |
| All shares are 0 | `canProceed` returns false | CTA disabled; user cannot navigate forward. `computeSplit` returns zero amounts. |
| Use case called with `sum == 0` (defensive) | `CalculateSplitUseCase.sharesSplit` | Throws `IllegalArgumentException`. Indicates a ViewModel guard bug if it ever fires; surfaces clearly in tests and crash logs rather than silently returning bad data. |
| Negative `totalAmount` | `CalculateSplitUseCase.sharesSplit` | Throws `IllegalArgumentException`. (Same guard as `equalSplit`.) |

## Testing

### Unit tests — `CalculateSplitUseCaseTest.kt`

Delete every `unequalSplit` test case. Add the following `sharesSplit` cases:

1. **Equal shares, exact division** — `₹100, {a:1, b:1, c:1, d:1}` → all `25.00`. Sum `100.00`.
2. **Equal shares, paisa leftover** — `₹100, {a:1, b:1, c:1}` → two `33.34`, one `33.33`. Sum `100.00`.
3. **Weighted shares, exact** — `₹120, {a:2, b:1, c:1}` → `60.00, 30.00, 30.00`.
4. **Weighted shares, leftover** — `₹100, {a:2, b:1, c:1}` → `50.00, 25.00, 25.00`.
5. **Awkward weights** — `₹100, {a:3, b:1}` → `75.00, 25.00`. `₹100, {a:2, b:3}` → `40.00, 60.00`.
6. **Hard rounding case** — `₹10, 7 participants @ 1 share each` → sum `10.00`, each ≈ `1.43`.
7. **Zero-share participants** — `₹100, {a:1, b:0, c:1}` → `50.00, 0.00, 50.00`.
8. **Single participant takes all** — `₹100, {a:5}` → `100.00`.
9. **Zero total amount** — `₹0, {a:1, b:2}` → both `0.00`.
10. **Total shares = 0 throws** — `IllegalArgumentException`.
11. **Negative shares throws** — defensive guard.
12. **Negative total throws** — defensive guard.
13. **Randomized paisa invariant** — 200 iterations of random total amounts (`₹1`–`₹100,000`) with 2–8 random share weights (each `1`–`9`). Assert exact sum every time. Mirrors the existing `equalSplit` randomized test.

### ViewModel tests

Add to `FriendsViewModelTest` (or create it if absent):

- `toggleSplitMode` flips between `EQUAL` and `SHARES`.
- `setShares(friendId, n)` updates only the targeted friend; clamps negatives to `0`.
- `setOwnerShares(n)` updates owner shares; clamps negatives to `0`.
- `computeSplit` in SHARES mode with `includeSelfInSplit = true` includes the owner share in the divisor.
- `computeSplit` in SHARES mode with `includeSelfInSplit = false` excludes the owner; friends absorb the entire bill.
- `canProceed` is `false` in SHARES mode when all shares are `0`, even if friends are selected.
- `canProceed` is `true` once any participant has shares > 0.

### Manual / instrumented sanity checks

- Toggle UI: Equal | Shares renders, label and icon correct.
- Switch to Shares mode: enter shares for self + friends → amounts update live.
- Toggle "I paid for others" off in Shares mode: owner row disappears, friends absorb full bill.
- Bottom CTA disabled until at least one share > 0.

### Build verification

- `./gradlew testDebugUnitTest` — all green.
- `./gradlew assembleDebug` — clean compile.

## Open questions / explicit deferrals

- **Decimal shares (e.g., `1.5`)** — out of scope for v1. Splitwise supports them; we'll revisit if real-world feedback asks for it.
- **Per-friend default shares persisted across sessions** — out of scope. Every session starts at `0`.
- **Share count > 99** — clamped to 99 in the UI. This is a deliberate convenience cap; if a user has a real bill with >99 share weighting, they can use a workaround (e.g., 1:99 instead of 1:100). Not expected in real bill-splitting scenarios.
