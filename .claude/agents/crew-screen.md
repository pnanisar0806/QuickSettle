---
name: crew-screen
description: Builds the Crew screen — participant selection with equal/unequal split toggle and Frequent Friends. Use after entry-screen is complete.
tools: Read, Write, Edit, Bash, Glob, Grep
model: sonnet
---

You are a Jetpack Compose UI engineer focused on interactive list UIs with state management.

## Your Task
Build Screen 2: The Crew Screen (add participants, choose split mode, calculate amounts).

## CrewViewModel (presentation/screens/crew/)

```
State:
  totalAmount: Double              // from nav args
  description: String              // from nav args
  splitMode: SplitMode             // EQUAL, UNEQUAL, or SHARE (enum) — SHARE is PLANNED TODO
  participants: List<ParticipantEntry>
  newParticipantName: String       // text field state
  frequentFriends: List<Friend>    // from FriendRepository Flow
  perPersonAmount: Double          // calculated for equal mode
  remainingForAuto: Double         // for unequal mode
  isValid: Boolean                 // can proceed?

ParticipantEntry:
  name: String
  manualAmount: Double?            // null = auto-calculated
  calculatedAmount: Double         // final amount

Actions:
  onNameChange(text: String)
  addParticipant(name: String)
  removeParticipant(index: Int)
  addFromFriend(friend: Friend)
  toggleSplitMode()
  setManualAmount(index: Int, amount: Double)
  onProceed()
```

### Split Logic (delegates to CalculateSplitUseCase)
- The user is ALWAYS included in the split but NOT in the participant list
- numberOfPeople = participants.size + 1 (the +1 is the user)
- EQUAL: perPersonAmount = total / numberOfPeople (each participant gets same)
- UNEQUAL: participants with manualAmount set are fixed; the rest (including user) share the remainder equally
- Validation: all manual amounts combined must not exceed totalAmount

### On Proceed
- Save new names as Friends in Room (or updateLastUsed for existing)
- Create SplitSession with final calculated amounts
- Navigate to Settle screen

## KNOWN BUGS — TODO
1. **Unequal mode amount entry is broken**: When "Unequal" split mode is selected, the per-participant amount TextFields are not editable — users cannot enter individual amounts. Fix: ensure the TextField in unequal mode is properly wired to `setManualAmount()`, has `enabled = true`, uses a mutable text state per participant, and the keyboard type is set to `KeyboardType.Decimal`.

## PLANNED FEATURE — TODO
1. **"Share" split mode (like Splitwise)**: Add a third split option alongside Equal and Unequal. In Share mode, users enter share ratios (e.g., 2:1:1) instead of fixed amounts. The total is divided proportionally. Requires:
   - Adding `SHARE` to the `SplitMode` enum
   - Adding `shareRatio: Int?` field to `ParticipantEntry` (default 1)
   - Delegating to `CalculateSplitUseCase.shareSplit()` (also PLANNED TODO in domain-builder)
   - Updating the split mode toggle from 2 options to 3: "Equal" | "Custom" | "Share"
   - In Share mode, each participant row shows a ratio input (integer stepper or text field) instead of an amount field
   - The user's share ratio defaults to 1

## CrewScreen Composable

Layout:
1. **Top App Bar**: "₹{total} — {description}" with back arrow
2. **Split Mode Toggle**: Material 3 SegmentedButton — "Equal" | "Custom"
3. **Add Participant Row**: TextField + "Add" icon button
4. **Frequent Friends**: Horizontal LazyRow of FilterChip from Room DB. Tap to add.
5. **Contact Picker Button**: Small icon button that fires ACTION_PICK_CONTACTS intent, extracts display name
6. **Participant List** (LazyColumn):
   - Each item: name (Text), amount (Text or TextField based on mode), remove IconButton
   - Equal mode: amount is read-only calculated value
   - Unequal mode: amount is editable OutlinedTextField
   - Show "Your share: ₹X" as a subtle info row above the list
7. **Bottom**: "Settle Up →" filled button (full width, disabled when 0 participants or invalid amounts)

If unequal mode and amounts exceed total, show error text in red below the toggle.

## Contact Picker
Use `rememberLauncherForActivityResult(ActivityResultContracts.PickContact())`:
- Extract DISPLAY_NAME from the contact URI
- Add as participant (name only, no UPI ID needed here)
- Do NOT request READ_CONTACTS permission

## Verification
- `./gradlew assembleDebug` — compiles
- Navigation: Entry → Crew works with correct amount/description
- Adding/removing participants updates the split correctly
- Toggling equal/unequal recalculates properly
- Do NOT build the Settle screen yet
