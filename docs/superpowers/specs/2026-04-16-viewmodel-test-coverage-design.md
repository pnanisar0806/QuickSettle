# Comprehensive ViewModel Test Coverage

**Date:** 2026-04-16
**Scope:** Fill all ViewModel test gaps — ProfileViewModel, SettleViewModel.refreshProfile, EntryViewModel, FriendsViewModel missing methods

## Context

Two bugs shipped that tests would have caught:
1. QR generation gated on friend's UPI ID (only user VPA needed)
2. SettleViewModel cached user VPA at init — profile edits had no effect

Existing test suite covers domain use cases well but has ViewModel gaps.

## Test Infrastructure

- JUnit 5 + Google Truth (existing)
- kotlinx-coroutines-test (existing)
- Hand-rolled fakes (existing pattern, no mocking library)

## New Shared Fake

### FakeUserProfileStore

In-memory fake that implements `UserProfile` interface AND exposes `saveProfile`/`clearProfile`/`isProfileSetup` matching `UserProfileStore`'s public API. Backed by nullable String fields. Reusable across ProfileViewModel, SettleViewModel, and EntryViewModel tests.

**Location:** `src/test/.../testutil/FakeUserProfileStore.kt`

## Tests by ViewModel

### 1. ProfileViewModelTest (NEW — ~9 tests)

| Test | Assertion |
|---|---|
| init loads name from store | `uiState.displayName == "Alice"` |
| init loads VPA from store | `uiState.upiId == "alice@upi"` |
| init with null profile defaults to empty | both fields `""` |
| onDisplayNameChange updates state | `uiState.displayName == "Bob"` |
| onUpiIdChange updates state | `uiState.upiId == "bob@upi"` |
| field change resets saved flag | `uiState.saved == false` after change |
| saveProfile persists and sets saved flag | store has new values, `saved == true` |
| saveProfile blank name is no-op | store unchanged, `saved == false` |
| saveProfile blank UPI is no-op | store unchanged, `saved == false` |
| saveProfile trims whitespace | store has trimmed values |

### 2. SettleViewModelTest extensions (~7 tests)

| Test | Assertion |
|---|---|
| refreshProfile re-reads VPA from store | `uiState.userVpa` updated |
| refreshProfile regenerates URIs with new VPA | all URIs contain new VPA |
| refreshProfile regenerates share messages | messages contain new UPI link |
| refreshProfile blank VPA produces empty URIs | all `upiUri == ""` |
| setSettlementData reads fresh VPA (not cached) | URI uses current store value |
| setSettlementData generates URI when friend has no UPI | `upiUri.isNotBlank()` |
| setSettlementData empty user VPA produces empty URI regardless | `upiUri == ""` |

### 3. EntryViewModelTest extensions (~12 tests)

| Test | Assertion |
|---|---|
| init shows onboarding when profile not set up | `showOnboarding == true` |
| init hides onboarding when profile set up | `showOnboarding == false` |
| init loads display name | `displayName == "Alice"` |
| onDigitPress updates rawAmount | `rawAmount == "5"` |
| onDecimalPress updates rawAmount | `rawAmount == "0."` |
| onBackspace removes last char | `"123" -> "12"` |
| onBackspace empty stays empty | `"" -> ""` |
| onClear resets rawAmount | `rawAmount == ""` |
| onDescriptionChange updates description | `description == "Dinner"` |
| onProfileSaved saves to store | store has values |
| onProfileSaved hides onboarding | `showOnboarding == false` |
| onProfileSaved updates display name | `displayName == "Alice"` |

### 4. FriendsViewModelTest extensions (~11 tests)

| Test | Assertion |
|---|---|
| addFriendManually adds friend to DAO | DAO contains new friend |
| addFriendManually auto-selects new friend | appears in `selectedFriends` |
| addFriendManually trims name | stored name is trimmed |
| addFriendManually blank name is no-op | DAO unchanged |
| addFriendManually blank UPI becomes null | friend.upiId is null |
| addFriendManually closes overlay | `showAddFriend == false` |
| addFriendFromContact adds to DAO | DAO contains friend |
| addFriendFromContact blank name is no-op | DAO unchanged |
| addFriendAndClose adds friend | DAO contains friend |
| addFriendAndClose blank name is no-op | DAO unchanged |
| addFriendAndClose blank UPI becomes null | friend.upiId is null |

## File Changes

| File | Action |
|---|---|
| `src/test/.../testutil/FakeUserProfileStore.kt` | CREATE |
| `src/test/.../screens/profile/ProfileViewModelTest.kt` | CREATE |
| `src/test/.../screens/settle/SettleViewModelTest.kt` | EXTEND |
| `src/test/.../screens/entry/EntryViewModelTest.kt` | EXTEND |
| `src/test/.../screens/crew/FriendsViewModelTest.kt` | EXTEND |

## Out of Scope

- Data layer tests (FriendDao, UserProfileStore, FriendRepository) — need Robolectric/instrumented
- UI/Compose tests — need compose-ui-test
- Navigation tests
