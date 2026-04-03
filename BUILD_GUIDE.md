# Quick-Settle: Claude Code Build Setup

## What's in This Folder

```
quicksettle/
├── CLAUDE.md                          # Project context — loaded every session
├── docs/
│   └── designs/                       # Stitch design exports
│       ├── 1._bill_entry_final/       # Screen 1: Bill Entry (Amount tab)
│       ├── 2._select_friends_updated/ # Screen 2: Select Friends (Friends tab)
│       ├── 3._settlement_list_updated/# Screen 3: Settlement List (Settle tab)
│       ├── 4._add_friend_updated/     # Screen 4: Add Friend (overlay)
│       └── forest_mint_ledger/        # DESIGN.md — full design system spec
└── .claude/
    └── agents/
        ├── scaffold.md                # Phase 0: Project structure + Quick-Settle theme
        ├── domain-builder.md          # Phase 1: Models + use cases + tests
        ├── data-builder.md            # Phase 2: Room + Prefs + Hilt
        ├── entry-screen.md            # Phase 3: Bill Entry screen (Amount tab)
        ├── friends-screen.md          # Phase 4: Select Friends + Add Friend screens
        ├── settle-screen.md           # Phase 5: Settlement List screen (Settle tab)
        └── polish.md                  # Phase 6: Edge cases + final QA
```

## How It Works

**CLAUDE.md** is loaded automatically at the start of every Claude Code session. It contains the project's tech stack, architecture, coding conventions, and critical rules. You never need to re-explain these — Claude just knows.

**Subagents** (`.claude/agents/*.md`) are specialized agents for each build phase. Each has its own system prompt, scoped to one task. When you invoke a subagent, it runs in an isolated context window so your main session stays clean.

## Step-by-Step Build Process

### 1. Create your Android project first

Open Android Studio → New Project → Empty Compose Activity:
- Name: `QuickSettle`
- Package: `com.quicksettle`  
- Min SDK: 28

Then copy `CLAUDE.md` and `.claude/` into your project root.

### 2. Open Claude Code in your project

```bash
cd /path/to/QuickSettle
claude
```

### 3. Run phases in order

**Phase 0 — Scaffold:**
```
@scaffold Set up the project structure, all Gradle dependencies, the Quick-Settle theme (read the DESIGN.md first), bottom navigation, and placeholder files. Make sure it compiles.
```

**Phase 1 — Domain Logic:**
```
@domain-builder Implement the domain models, CalculateSplitUseCase, GenerateUpiLinkUseCase, and all unit tests. Run the tests.
```

**Phase 2 — Data Layer:**
```
@data-builder Implement Room database, FriendDao, UserProfileStore, FriendRepository, and all Hilt modules. Make sure it compiles with existing tests passing.
```

**Phase 3 — Bill Entry Screen:**
```
@entry-screen Build the Bill Entry screen (Amount tab). Study the Stitch design at docs/designs/1._bill_entry_final/screen.png first. Include the number pad, description field, amount display, and Add Friends CTA.
```

**Phase 4 — Friends Screens:**
```
@friends-screen Build the Select Friends screen (Friends tab) and the Add Friend overlay. Study docs/designs/2._select_friends_updated/screen.png and docs/designs/4._add_friend_updated/screen.png first. Include equal/unequal toggle, friend cards, contact picker, and manual UPI entry.
```

**Phase 5 — Settle Screen:**
```
@settle-screen Build the Settlement List screen (Settle tab). Study docs/designs/3._settlement_list_updated/screen.png first. Include settlement cards with Show QR and WhatsApp buttons, status badges, and the Settle All Balances CTA.
```

**Phase 6 — Polish:**
```
@polish Run the final QA pass. Fix all edge cases, add animations, verify dark theme works with the Quick-Settle palette, and check every acceptance criterion.
```

### 4. Between phases

After each phase completes:

1. **Build in Android Studio** — check for compile errors
2. **Run on device/emulator** — verify visually
3. **Run tests**: `./gradlew testDebugUnitTest`
4. **If something is broken**, paste the error back into Claude Code (in the same session or a new one — CLAUDE.md will reload the context)

## Tips

- **One agent at a time.** Don't try to run multiple phases simultaneously.
- **Use /clear between phases** to reset context and keep things fast.
- **If a phase fails mid-way**, you can re-invoke the same agent — it will read the current file state and continue.
- **For bug fixes**, you don't need a subagent. Just describe the bug in the main Claude Code session — CLAUDE.md gives it all the context it needs.
- **To add features later** (e.g., split history, recurring groups), create a new agent in `.claude/agents/` following the same pattern.

## Alternative: Run as Session Agent

Instead of `@agent` mentions, you can start a full session as a specific agent:

```bash
claude --agent scaffold
```

This makes the entire session use the scaffold agent's system prompt. Useful for focused work.
