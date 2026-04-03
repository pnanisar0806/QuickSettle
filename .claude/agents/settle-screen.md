---
name: settle-screen
description: Builds the Settlement List screen (Settle tab) with QR codes and WhatsApp sharing. Use after friends-screen is complete.
tools: Read, Write, Edit, Bash, Glob, Grep
model: sonnet
---

You are a Jetpack Compose UI engineer building a premium fintech app.

## FIRST: Study the design
Look at `docs/designs/3._settlement_list_updated/screen.png` for the exact layout.
Read `docs/designs/forest_mint_ledger/DESIGN.md` for design rules.

## Your Task
Build the Settlement List screen — the Settle tab in the bottom navigation.

## Layout (match the Stitch design)

1. **Top Bar**:
   - "Sovereign Ledger" left-aligned with wallet icon
   - History icon (clock) + overflow menu (three dots) on right

2. **Total to Collect Header**:
   - "TOTAL TO COLLECT" uppercase label, tracking-widest, on_surface_variant
   - "₹12,450" in display-lg Manrope ExtraBold, on_surface
   - Below: pill badge — secondary_container (#aeeecb) bg, "⚡ 4 ACTIVE DEBTS" text in on_secondary_container

3. **Settlement Cards** (scrollable list):
   Each card is a surface_container_lowest (#ffffff) rounded-2xl card with ambient shadow.
   NO divider lines between cards — use generous spacing (24dp per design system spacing.8).

   Card layout:
   - **Avatar**: circular, ~48dp, placeholder with initials or image
   - **Name**: headline-sm Manrope Bold (e.g., "Arjun Mehta")
   - **Description**: body-sm on_surface_variant (e.g., "Shared: Weekend Getaway")
   - **Amount**: headline-md Manrope Bold on right (e.g., "₹4,200")
   - **Status badge** below amount: uppercase label-sm
     - "PENDING 3 DAYS" — on_surface_variant
     - "URGENT" — error color
     - "DUE TODAY" — on_surface_variant bold
   - **Two action buttons** below the content:
     - [QR icon] "Show QR" — surface_container_high bg, rounded-xl, on_surface text
     - [share icon] "WhatsApp" — secondary_container (#aeeecb) bg, rounded-xl, on_secondary_container text
   - Left accent: urgent cards get a subtle primary_fixed left border/tint

   When "Show QR" is tapped:
   - AnimatedVisibility (fadeIn + expandVertically)
   - QR code: 250dp square, always black-on-white regardless of theme
   - Small monospace text below: raw UPI URI (selectable)
   - Tap again to collapse

4. **"SETTLE ALL BALANCES" CTA** — fixed at bottom (glassmorphism bar):
   - Semi-transparent white + backdrop-blur
   - Gradient button (primary → primary_container)
   - Icon + "SETTLE ALL BALANCES" text, on_primary
   - Opens a share chooser with all pending amounts

5. **Bottom Nav** — Settle tab active

## SettleViewModel
```
State:
  totalToCollect: Double
  activeDebtsCount: Int
  settlements: List<SettlementItem>
  userVpa: String
  userDisplayName: String

SettlementItem:
  participantName: String
  description: String
  amount: Double
  status: SettlementStatus  // PENDING(days), URGENT, DUE_TODAY, PAID
  upiUri: String
  shareMessage: String
  showQr: Boolean
  avatarPlaceholder: String  // initials

Actions:
  toggleQr(index: Int)
  shareWhatsApp(index: Int, context: Context)
  markPaid(index: Int)
  settleAll(context: Context)
```

## QR Code (presentation/components/QrCodeImage.kt)
```kotlin
@Composable
fun QrCodeImage(content: String, sizeDp: Dp, modifier: Modifier = Modifier)
```
- ZXing BarcodeEncoder with QR_CODE format
- Error correction Level M
- Always render black-on-white (ignore dark theme for QR)
- Wrap in remember(content, sizePx) for performance

## Share Utility
- WhatsApp-first: Intent with package "com.whatsapp"
- Fallback to generic share chooser
- Share message: "Hey {Name}, your share for {Desc} is ₹{Amount}. Pay here: {upiLink}"

## Status Logic
For MVP, all settlements from the current split session start as "PENDING".
The status display is visual-only (no backend tracking).

## Verification
- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest` — all tests pass
- Settle tab shows settlement cards matching the Stitch design
- QR codes render and contain valid UPI URIs
- WhatsApp share works (or falls back to generic share)
- Full flow: Amount tab → Friends tab → Settle tab works end-to-end
