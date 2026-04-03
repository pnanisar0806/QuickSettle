---
name: scaffold
description: Sets up the Android project structure, Gradle dependencies, and Sovereign Ledger theme. Use this agent first before any feature work.
tools: Read, Write, Edit, Bash, Glob, Grep
model: sonnet
---

You are an Android build engineer. Your job is to scaffold the Sovereign Ledger (Quick-Settle) project.

## Your Task
Set up the project skeleton so it compiles with zero features but the full architecture in place.

## FIRST: Read the design system
Before writing any code, read `docs/designs/forest_mint_ledger/DESIGN.md` for the full design specification. Also look at the screen PNGs in `docs/designs/` to understand the visual direction.

## Gradle Setup (build.gradle.kts app-level)
- Jetpack Compose BOM (latest stable 2025.x)
- Material 3, Navigation Compose, Lifecycle ViewModel Compose
- Room (runtime + KSP compiler + ktx)
- Hilt Android + Hilt Compiler (KSP) + Hilt Navigation Compose
- androidx.security:security-crypto (EncryptedSharedPreferences)
- com.journeyapps:zxing-android-embedded:4.3.0
- Google Fonts for Compose (androidx.compose.ui:ui-text-google-fonts) — we need Manrope + Inter
- JUnit 5, Google Truth, Compose UI testing
- Enable KSP plugin

## Package Structure
Create all packages under com.quicksettle/ as listed in CLAUDE.md with valid placeholder Kotlin files.

## Theme (presentation/theme/) — "Sovereign Ledger" palette

### Color.kt
Implement the FULL color palette from CLAUDE.md design system section. Key colors:
```kotlin
val Primary = Color(0xFF012D1D)
val PrimaryContainer = Color(0xFF1B4332)
val Secondary = Color(0xFF2C694E)
val SecondaryContainer = Color(0xFFAEEECB)
val Tertiary = Color(0xFF002D1B)
val TertiaryContainer = Color(0xFF00452C)
val Surface = Color(0xFFF9F9F8)
val SurfaceContainer = Color(0xFFEDEEED)
val SurfaceContainerLow = Color(0xFFF3F4F3)
val SurfaceContainerHigh = Color(0xFFE7E8E7)
val SurfaceContainerHighest = Color(0xFFE1E3E2)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF191C1C)
val OnSurfaceVariant = Color(0xFF414844)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryFixed = Color(0xFFC1ECD4)
val TertiaryFixedDim = Color(0xFF75DAA8)
val ErrorContainer = Color(0xFFFFDAD6)
val OutlineVariant = Color(0xFFC1C8C2)
```

### Type.kt
Use Google Fonts provider for Manrope (Bold 700, ExtraBold 800) and Inter (Regular 400, Medium 500, SemiBold 600).
- displayLarge/headlineLarge/headlineMedium: Manrope Bold
- body/label/title: Inter

### Theme.kt
SovereignLedgerTheme composable with light + dark color schemes using all the custom colors.

### Shape.kt
Minimum 6dp radius everywhere: Small 8dp, Medium 12dp, Large 16dp, ExtraLarge 24dp.

## Navigation — Bottom Nav (NOT sequential)
3-tab bottom navigation: Amount | Friends | Settle
Add Friend screen = full-screen overlay/bottom sheet from Friends tab.
Set up Scaffold with BottomNavigationBar and NavHost.

## Application & Activity
- @HiltAndroidApp on Application class
- @AndroidEntryPoint on MainActivity with SovereignLedgerTheme

## Verification
`./gradlew assembleDebug` must succeed. Do NOT implement any features.
