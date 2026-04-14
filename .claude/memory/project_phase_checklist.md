---
name: Build Phase Checklist
description: Tracks completion status of each build phase (0-6) with specific items verified against agent specs. Update at end of each phase.
type: project
---

## Phase Completion Status (verified 2026-04-04)

### Phase 0 — Scaffold: COMPLETE
- [x] Gradle deps: Compose BOM, M3, Navigation, Room+KSP, Hilt+KSP, security-crypto, ZXing, Google Fonts, JUnit 5, Truth
- [x] Color.kt — full palette (light + dark)
- [x] Type.kt — Manrope (Bold, ExtraBold) + Inter (Regular, Medium, SemiBold) via Google Fonts
- [x] Theme.kt — SovereignLedgerTheme with light + dark color schemes
- [x] Shape.kt — min 6dp radius (8/12/16/24dp scale)
- [x] Navigation — 3-tab bottom nav (Amount | Friends | Settle), Screen sealed class, NavGraph
- [x] @HiltAndroidApp Application, @AndroidEntryPoint MainActivity

### Phase 1 — Domain: COMPLETE
- [x] Friend model (@Entity, UUID PrimaryKey)
- [x] SplitSession model
- [x] ParticipantSplit model
- [x] CalculateSplitUseCase — equalSplit (paisa-safe) + unequalSplit
- [x] GenerateUpiLinkUseCase — generateUri + generateShareMessage
- [x] CalculateSplitUseCaseTest — 13 tests including 200-random paisa invariant
- [x] GenerateUpiLinkUseCaseTest — 13 tests (URI format, encoding, share message)

### Phase 2 — Data: COMPLETE
- [x] AppDatabase (@Database, entities = Friend)
- [x] FriendDao (getAllFriends, searchFriends, insertFriend, deleteFriend, updateLastUsed)
- [x] UserProfileStore (EncryptedSharedPreferences: save/get/clear profile)
- [x] FriendRepository (@Inject, getAllFriends, searchFriends, addFriend, removeFriend, updateLastUsed)
- [x] DatabaseModule (provides AppDatabase singleton + FriendDao)
- [x] AppModule (empty — deps auto-discovered via @Inject constructors)

### Phase 3 — Entry Screen: COMPLETE
- [x] EntryScreen — description field + amount display card + number pad + "Add Friends" CTA
- [x] EntryViewModel — digit/decimal/backspace/clear, description, canProceed, Indian comma formatting
- [x] Onboarding BottomSheet — name + UPI fields, saves to EncryptedSharedPreferences
- [x] NumberPad component — 4x3 grid, haptic feedback, scale animation on press
- [x] LedgerTextField component — surface_container_low bg, 3px bottom indicator
- [x] GradientButton component — primary gradient, xl rounded, disabled state
- [x] Amount input rules — max 2dp, max 7 digits, no leading zeros, Indian grouping

### Phase 4 — Friends Screen: COMPLETE
- [x] CrewScreen — header (total bill + "Add Friend" button), friend cards with left pill, suggestions section
- [x] Split mode toggle UI — 2 options (Equal | Unequal)
- [x] FriendCard — avatar, name, UPI handle, amount, checkmark selection
- [x] SuggestionCard — avatar, name, "+" add circle
- [x] Glassmorphism "Go to Settle" CTA bar
- [x] AddFriendScreen — top bar, search, import contacts (ACTION_PICK), suggested friends horizontal scroll, manual UPI entry, "Add to Group"
- [x] FriendsViewModel — full state, toggleFriend, toggleSplitMode, setManualAmount, addFriend variants
- [x] FriendChip component — avatar initial circle, name label, selected state with primary_fixed bg
- [x] Unequal mode — editable BasicTextField with KeyboardType.Decimal, wired to setManualAmount()

### Phase 5 — Settle Screen: COMPLETE (verified 2026-04-14)
- [x] SettleViewModel — state management with settlements, toggleQr, markPaid, settleAllMessage
- [x] UserProfile interface extracted for testability, Hilt binding in AppModule
- [x] SettleViewModelTest — 20 tests (init, setSettlementData, toggleQr, markPaid, settleAll)
- [x] SettlementStatusTest — 9 tests (displayText, badgeColor for all statuses)
- [x] QrCodeImage — ZXing QR rendering, black-on-white, memoized on (content, sizePx)
- [x] SettleScreen — TotalToCollect header with display-lg amount
- [x] Active debts badge (secondary_container pill)
- [x] Settlement cards (avatar, name, description, amount, status badge, Show QR + WhatsApp buttons)
- [x] QR code toggle with AnimatedVisibility (fadeIn + expandVertically)
- [x] WhatsApp share intent with fallback to generic share
- [x] "Settle All Balances" CTA with glassmorphism bar
- [x] Status logic (PENDING, URGENT, DUE_TODAY, PAID) with displayText + badgeColor
- [x] NavGraph wired: Entry→Crew→Settle end-to-end data flow
- [x] assembleDebug + testDebugUnitTest pass

### Phase 6 — Polish: NOT STARTED
- [ ] Edge cases + final QA
- [ ] Animations
- [ ] Dark theme verification
- [ ] Acceptance criteria check
