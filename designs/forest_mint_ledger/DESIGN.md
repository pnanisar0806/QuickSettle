# Design System Specification: Sovereign Ledger

## 1. Overview & Creative North Star
**Creative North Star: The Sovereign Ledger**
This design system moves away from the "disposable" feel of modern fintech. Instead, it adopts the persona of a high-end, digital-first ledger. It combines the weight and authority of traditional Indian banking with the frictionless speed of UPI. We achieve an "Editorial High-End" feel by prioritizing aggressive white space, bold geometric typography, and a tactile sense of depth.

The "No-Login, Offline-First" nature of the application is reflected in its rock-solid stability. The UI doesn't "load"; it "exists." We break the standard Material 3 template by using intentional asymmetry in card layouts and a sophisticated "Money Green" tonal palette that communicates wealth, growth, and high trust.

---

## 2. Colors: Tonal Depth over Borders
The palette is rooted in deep botanical greens (`primary`) and high-energy mints (`tertiary`). This creates a high-contrast environment that feels both premium and modern.

### The "No-Line" Rule
**Strict Mandate:** Designers are prohibited from using 1px solid borders to define sections. Layout boundaries must be established solely through background color shifts or tonal transitions. 
*   Use `surface` (#f9f9f8) as the base canvas. 
*   Use `surface_container_low` (#f3f4f3) for secondary content areas. 
*   Use `surface_container_lowest` (#ffffff) for the most interactive, "raised" elements.

### Surface Hierarchy & Nesting
Think of the UI as layers of fine architectural paper. 
*   **Base:** `surface`
*   **Sectioning:** `surface_container`
*   **Hero Interactive:** `surface_container_highest` (#e1e3e2)
This nesting creates a natural visual hierarchy without the clutter of lines.

### Glass & Gradient Rule
To prevent the UI from feeling flat, floating elements (like a "Settle Now" sticky bar) must use **Glassmorphism**. Apply a semi-transparent `surface_container_lowest` with a 20px backdrop blur. 
**Signature Texture:** Use a subtle linear gradient on primary action buttons, transitioning from `primary` (#012d1d) to `primary_container` (#1b4332) at a 45-degree angle. This provides a "soul" to the interactive elements that flat hex codes cannot achieve.

---

## 3. Typography: Editorial Authority
We use a dual-typeface system to balance character with extreme legibility.

*   **Headlines (Manrope):** Chosen for its modern, geometric construction. Use `display-lg` and `headline-lg` for currency amounts and names. These should be set to **Bold (700)** to feel authoritative and "unchangeable."
*   **Body (Inter):** The workhorse for transactional data. Inter’s tall x-height ensures that even at `body-sm`, bill details and UPI IDs are unmistakably clear.
*   **Tonal Contrast:** Always pair a `headline-md` in `on_surface` (#191c1c) with a `label-md` in `on_surface_variant` (#414844) to create a sophisticated, multi-layered information hierarchy.

---

## 4. Elevation & Depth: Tonal Layering
Traditional drop shadows are largely replaced by **Tonal Layering**.

*   **The Layering Principle:** A card containing a bill split should be `surface_container_lowest` (#ffffff) sitting on a `surface_container` (#edeeed) background. This creates a "soft lift."
*   **Ambient Shadows:** For the "Custom Number Pad" or "Floating Action Buttons," use an extra-diffused shadow: `Y: 12px, Blur: 24px, Spread: -4px`. The shadow color must be a 6% opacity version of `on_primary_fixed` (#002114), creating a natural, mossy depth rather than a grey smudge.
*   **The "Ghost Border" Fallback:** If a high-contrast environment requires more definition, use a "Ghost Border": `outline_variant` (#c1c8c2) at **15% opacity**. 100% opaque borders are strictly forbidden.

---

## 5. Components

### Custom Number Pad (The Hero)
The number pad is the primary touchpoint. 
*   **Keys:** Large, `surface_container_high` (#e7e8e7) rectangles with `xl` (0.75rem) roundedness.
*   **Typography:** Numbers in `headline-lg` (Manrope).
*   **CTA Key:** The "Checkmark/Confirm" key should use the `tertiary` (#002d1b) background with `on_tertiary` (#ffffff) icon to signal the completion of a transaction.

### Buttons
*   **Primary:** Gradient fill (`primary` to `primary_container`), `xl` roundedness. No shadow.
*   **Secondary:** `secondary_container` (#aeeecb) fill with `on_secondary_container` (#316e52) text.
*   **Tertiary:** Text-only in `primary`, but with a `surface_variant` (#e1e3e2) hover/press state.

### Cards & Lists
*   **Forbid Dividers:** Do not use lines between list items. Use 12px (`spacing.3`) of vertical white space and a 4px (`spacing.1`) vertical pill of `primary_fixed` (#c1ecd4) on the left side of an active list item to indicate selection.
*   **Contextual Chips:** Use `tertiary_fixed_dim` (#75daa8) for "Paid" statuses and `error_container` (#ffdad6) for "Owed" statuses.

### Input Fields
Inputs should not be "boxes." They should be `surface_container_low` (#f3f4f3) with a thick 3px bottom indicator in `primary` only when focused. This maintains the "Ledger" aesthetic.

---

## 6. Do’s and Don’ts

### Do
*   **Do** use `primary_fixed` (#c1ecd4) for subtle background highlights behind important text.
*   **Do** lean into `display-lg` typography for the total "Amount to Settle"—make the money the star.
*   **Do** use `spacing.8` (2rem) and `spacing.10` (2.5rem) to create generous breathing room between different groups of people in a split.

### Don't
*   **Don't** use standard Material "Outlined" buttons; they feel too "generic app."
*   **Don't** use pure black (#000000) for text. Always use `on_surface` (#191c1c) to maintain the forest-green tonal depth.
*   **Don't** use sharp corners. Every element should have a minimum of `md` (0.375rem) roundedness to feel approachable and "high-trust."
*   **Don't** use 1px dividers. If content needs separation, use a `surface_variant` (#e1e3e2) full-width horizontal band of 8px height instead.