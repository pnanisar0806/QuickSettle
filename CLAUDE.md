# Quick-Settle — Android Bill Splitter (India/UPI)

Zero-friction, offline-first bill splitting with instant UPI settlement. No sign-up, no cloud, no groups.

## Tech Stack
- Kotlin 2.1+, Jetpack Compose (Material 3), MVVM + Clean Architecture
- Room DB (Frequent Friends), EncryptedSharedPreferences (user profile)
- Hilt (DI), ZXing (QR generation), Navigation Compose
- Min SDK 28, Target/Compile SDK 35

## App Name
"Quick-Settle" (shown in top bar)

## Design System — "Forest Mint Ledger" (see docs/designs/forest_mint_ledger/DESIGN.md for full spec)
Reference screens in docs/designs/: 1._bill_entry_final/, 2._select_friends_updated/, 3._settlement_list_updated/, 4._add_friend_updated/

### Color Palette (Material 3 custom)
- primary: #012d1d (deep forest green)
- primary_container: #1b4332
- secondary: #2c694e
- secondary_container: #aeeecb
- tertiary: #002d1b
- tertiary_container: #00452c
- surface: #f9f9f8
- surface_container: #edeeed
- surface_container_low: #f3f4f3
- surface_container_high: #e7e8e7
- surface_container_highest: #e1e3e2
- surface_container_lowest: #ffffff
- on_surface: #191c1c (NEVER use pure black)
- on_surface_variant: #414844
- on_primary: #ffffff
- primary_fixed: #c1ecd4 (highlight backgrounds)
- tertiary_fixed_dim: #75daa8 ("Paid" status chips)
- error_container: #ffdad6 ("Owed" status chips)
- outline_variant: #c1c8c2

### Typography
- Headlines/Amounts: Manrope Bold 700 (display-lg for ₹ amounts, headline-lg for names)
- Body/Details: Inter 400/500/600 (body text, UPI IDs, labels)
- NEVER use system default fonts — always Manrope + Inter

### Design Rules (STRICT)
- NO 1px solid borders — use tonal background shifts only
- NO standard Material "Outlined" buttons
- NO pure black (#000000) text — use on_surface #191c1c
- NO sharp corners — minimum 6dp radius everywhere
- NO line dividers between list items — use 12dp vertical spacing + 4dp primary_fixed left pill for selection
- Primary buttons: gradient from #012d1d → #1b4332 at 45°, xl roundedness
- Floating elements (sticky bars): glassmorphism — semi-transparent white + 20dp backdrop blur
- Number pad keys: surface_container_high (#e7e8e7) background, headline-lg Manrope, xl rounded
- Input fields: surface_container_low background + 3px bottom indicator in primary when focused (no box borders)
- Ambient shadows: Y:12px Blur:24px Spread:-4px, color #002114 at 6% opacity

### Navigation
- Bottom navigation bar with 3 tabs: Amount | Friends | Settle
- Active tab: filled icon + mint background chip
- This is a bottom-nav app, NOT a sequential screen flow

## Architecture
```
com.quicksettle/
  data/local/          → Room DB, DAOs, EncryptedPrefs wrapper
  domain/model/        → Friend, SplitSession, ParticipantSplit
  domain/usecase/      → CalculateSplitUseCase, GenerateUpiLinkUseCase
  presentation/
    theme/             → Money Green Material 3 palette
    screens/entry/     → Amount input (number pad)
    screens/crew/      → Participant selection + split mode
    screens/settle/    → QR codes + share links
    components/        → NumberPad, FriendChip, QrCodeImage
    navigation/        → NavGraph, Screen sealed class
  di/                  → Hilt modules
```

## Commands
```bash
./gradlew assembleDebug                    # Debug build
./gradlew testDebugUnitTest                # Unit tests
./gradlew connectedDebugAndroidTest        # Instrumented tests
./gradlew ktlintCheck                      # Lint
```

## Screens (4 total — see docs/designs/ for reference screenshots)
1. **Bill Entry** (Amount tab): Description field + large ₹ amount display + custom number pad + "Add Friends" CTA
2. **Select Friends** (Friends tab): Total bill header + Equal/Unequal/Share toggle + Frequent Friends list with avatars + Suggestions section + "Go to Settle" CTA
3. **Settlement List** (Settle tab): "Total to Collect" header + active debts badge + settlement cards with Show QR / WhatsApp buttons + "Settle All Balances" CTA
4. **Add Friend** (overlay/sheet from Friends tab): Search/name entry + Import from Contacts + Suggested Friends grid + Manual UPI ID entry + "Add to Group" CTA

## Code Conventions
- Use Flow (not LiveData) for all reactive streams
- ViewModels expose StateFlow via MutableStateFlow
- Business logic lives ONLY in domain/usecase/ — never in ViewModels or Composables
- Composables are stateless; state hoisting via ViewModel always
- Use Hilt @Inject constructor for all dependencies — no manual instantiation
- Room entities use @PrimaryKey with UUID.randomUUID().toString()
- All money amounts are Double with 2-decimal rounding; sum of splits MUST equal total
- UPI URIs follow NPCI format: upi://pay?pa=VPA&pn=NAME&am=AMOUNT&cu=INR&tn=DESC

## Critical Rules
- NO network calls anywhere — app is 100% offline
- NO READ_CONTACTS permission — use ACTION_PICK intent only
- NO cloud backend, no analytics, no Firebase
- NEVER use LiveData, only StateFlow/Flow
- NEVER put split calculation logic inside a Composable or ViewModel — it belongs in CalculateSplitUseCase
- Prefer named arguments in Kotlin function calls for readability
- Write unit tests for every use case before wiring to UI

## Testing
- JUnit 5 + Google Truth for assertions
- Test the paisa invariant: sum of all splits == totalAmount (zero leftover)
- Test UPI URI format: must be scannable by GPay, PhonePe, Paytm
- Run tests after every phase: `./gradlew testDebugUnitTest`

## UPI Reference
```
upi://pay?pa={VPA}&pn={URL_ENCODED_NAME}&am={AMOUNT_2DP}&cu=INR&tn={URL_ENCODED_DESC}
```
Amount must be formatted to exactly 2 decimal places. Name and description must be URL-encoded.

## Phase Workflow
Build in phases using subagents. See .claude/agents/ for phase-specific agents.
Current build order: scaffold → domain → data → entry-screen → crew-screen → settle-screen → polish

## TODO / Known Issues
1. **BUG — Screen 2 (Unequal split input)**: When "Unequal" split mode is selected on the Friends/Crew screen, the per-participant amount TextFields are not editable. Users cannot enter individual amounts. The TextField must be properly enabled with `KeyboardType.Decimal` and wired to the `setManualAmount()` action.
2. **FEATURE — "Share" split mode**: Add a third split option (Equal | Unequal | Share). In Share mode, users enter integer share ratios (e.g., 2:1:1) instead of fixed amounts. The total is divided proportionally. Requires updates to: `SplitMode` enum, `CalculateSplitUseCase` (new `shareSplit` method), `ParticipantEntry` (new `shareRatio` field), split mode toggle UI (3 options), and unit tests.