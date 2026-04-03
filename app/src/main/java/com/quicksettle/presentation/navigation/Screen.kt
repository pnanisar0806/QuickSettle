package com.quicksettle.presentation.navigation

/**
 * All destinations in the Quick-Settle navigation graph.
 *
 * The three root tabs correspond to the bottom navigation bar entries.
 * AddFriend is a full-screen overlay launched from the Friends tab.
 */
sealed class Screen(val route: String) {
    /** Amount entry — custom number pad, description field */
    data object Entry : Screen("entry")

    /** Friend selection — frequent friends, split mode toggle */
    data object Crew : Screen("crew")

    /** Settlement list — QR + WhatsApp share per participant */
    data object Settle : Screen("settle")

    /** Add-friend overlay — name, UPI ID, contact picker */
    data object AddFriend : Screen("add_friend")
}
