---
name: friends-screen
description: Builds the Select Friends screen (Friends tab) and the Add Friend overlay. Use after entry-screen is complete.
tools: Read, Write, Edit, Bash, Glob, Grep
model: sonnet
---

You are a Jetpack Compose UI engineer building a premium fintech app.

## FIRST: Study the designs
Look at `docs/designs/2._select_friends_updated/screen.png` for the Friends tab layout.
Look at `docs/designs/4._add_friend_updated/screen.png` for the Add Friend overlay.
Read `docs/designs/forest_mint_ledger/DESIGN.md` for design rules.

## Your Task
Build TWO screens: the Select Friends tab and the Add Friend full-screen overlay.

---

## SCREEN: Select Friends (Friends tab)

### Layout (match Stitch design)

1. **Header**:
   - "TOTAL GROUP BILL" uppercase label, tracking-widest, on_surface_variant
   - "₹4,280" in display-lg / headline-lg Manrope ExtraBold, on_surface
   - "Add Friend" button on the right — gradient bg (primary → primary_container), on_primary text + person_add icon, rounded-full
   - This button opens the Add Friend overlay

2. **Split Mode Toggle**:
   - Two options: "Equal" (with = icon) | "Unequal" (with sort icon)
   - **(PLANNED — TODO)**: Add third option "Share" (with pie_chart icon) for ratio-based splitting
   - Selected: surface_container_lowest white bg with ambient shadow, rounded-xl
   - Unselected: transparent, on_surface_variant text
   - Wrap in a surface_container_high or outline_variant/20 rounded container
   - ~~**KNOWN BUG — FIXED**: Unequal mode TextFields now use BasicTextField wired to setManualAmount() with KeyboardType.Decimal~~
   - Below the toggle, an **UnequalInfoBar** shows remaining amount, over-budget error (red), or "Fully allocated" (green)
   - `canProceed` blocks navigation when unequal amounts exceed total or nothing is assigned

3. **Frequent Friends** section:
   - "Frequent Friends" headline in headlineMedium Manrope Bold
   - "4 Selected" count on the right in on_surface_variant
   - Friend cards — each is a surface_container_lowest white card with:
     - 4dp primary_fixed (#c1ecd4) left pill/indicator
     - Avatar image placeholder (rounded, ~48dp)
     - Name in headline-sm Manrope Bold
     - UPI handle below in body-sm on_surface_variant (e.g., "@arjun_m")
     - Amount on right: "₹1,070" in headline-sm Manrope Bold
     - Green checkmark circle when selected
   - NO line dividers between cards — use 12dp spacing per design rules
   - Cards have ambient shadow

4. **Suggestions** section:
   - "Suggestions" heading
   - Similar card layout but with "+" add icon instead of checkmark
   - Shows phone number hint instead of UPI handle (masked: "+91 98XXX XXX01")

5. **"Go to Settle →" CTA** — fixed at bottom (glassmorphism bar):
   - Semi-transparent white (#ffffff/80) + backdrop-blur(24px)
   - Gradient button (primary → primary_container)
   - Text: "Go to Settle →" + arrow icon, on_primary
   - Navigates to Settle tab
   - Disabled when 0 friends selected

6. **Bottom Nav** — Friends tab active

### FriendsViewModel
```
State:
  totalAmount: Double           // from shared state or nav
  description: String
  splitMode: SplitMode          // EQUAL or UNEQUAL (SHARE is PLANNED — TODO)
  selectedFriends: List<SelectedFriend>
  frequentFriends: List<Friend> // from Room
  suggestions: List<Friend>     // frequent but not yet selected
  perPersonAmount: Double       // auto-calculated
  showAddFriend: Boolean        // controls overlay visibility

Actions:
  toggleFriend(friendId: String)
  toggleSplitMode()
  setManualAmount(friendId: String, amount: Double)
  openAddFriend()
  closeAddFriend()
  onProceedToSettle()
```

Split logic:
- User is ALWAYS included in the split (participants + 1)
- EQUAL: total / (selected + 1), handle rounding with zero leftover paisa
- UNEQUAL: editable per-friend amounts, show remaining
- SHARE **(PLANNED — TODO)**: each friend has a ratio (default 1), total split proportionally via `CalculateSplitUseCase.shareSplit()`

---

## SCREEN: Add Friend (full-screen overlay / bottom sheet)

### Layout (match Stitch screen 4)

1. **Top Bar**: centered "Sovereign Ledger" with back arrow, notification + avatar icons

2. **Search Bar**:
   - surface_container_low bg, rounded-xl, no border
   - Search/person icon + "Search contacts or enter name" hint
   - Large, prominent placement

3. **"Import from Contacts" button**:
   - Full width, gradient bg (primary → primary_container), rounded-xl
   - Contacts book icon + "Import from Contacts" text, on_primary
   - Uses ACTION_PICK intent (no READ_CONTACTS permission)

4. **Suggested Friends** section:
   - "Suggested Friends" heading + "FREQUENT" chip/badge on right
   - Horizontal scrollable grid of friend avatars:
     - Circular avatar (~64dp)
     - Name below in body-sm
     - Green "+" badge overlay on bottom-right of avatar
   - Tap adds them to the split

5. **Manual UPI ID Entry** section:
   - "MANUAL UPI ID ENTRY" uppercase label
   - TextField with @ icon prefix, hint "username@upi"
   - surface_container_low bg, ledger-style (no border, bottom indicator)
   - Helper text below: "Verified UPI IDs allow direct settlement to bank accounts." in on_surface_variant italic

6. **"Add to Group" button** — bottom right, gradient bg, rounded-full:
   - "Add to Group" + person_add icon
   - Saves the friend to Room DB and adds to current split

### Contact Picker
Use `rememberLauncherForActivityResult(ActivityResultContracts.PickContact())`.
Extract DISPLAY_NAME only. No READ_CONTACTS permission.

## Unit Tests (REQUIRED)
Write/update tests in `app/src/test/java/com/quicksettle/presentation/screens/crew/`. Use JUnit 5 + Google Truth.

Existing test files to update if features change:
- `FriendsUiStateTest.kt` — pure state extension functions (suggestions, perPersonAmounts, ownerAmount, canProceed, isUnequalOverBudget, includeSelfInSplit logic)
- `FriendsViewModelTest.kt` — ViewModel actions using `FakeFriendDao` + `kotlinx-coroutines-test`
- `FormatAmountTest.kt` — Indian amount formatting

### Rules
- Run `./gradlew testDebugUnitTest` after writing tests — ALL must pass
- If you add or change a feature, update/add tests to cover the change
- Test pure state extension functions separately from ViewModel coroutine actions

## Verification
- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest` — all existing + new tests pass
- Friends tab shows selected friends with amounts
- Add Friend overlay opens/closes properly
- Equal/Unequal toggle recalculates
- Frequent friends come from Room DB
- Contact picker works without permissions
