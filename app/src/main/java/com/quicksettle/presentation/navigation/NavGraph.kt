package com.quicksettle.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.quicksettle.presentation.screens.crew.AddFriendScreen
import com.quicksettle.presentation.screens.crew.CrewScreen
import com.quicksettle.presentation.screens.crew.FriendsViewModel
import com.quicksettle.presentation.screens.entry.EntryScreen
import com.quicksettle.presentation.screens.entry.EntryViewModel
import com.quicksettle.presentation.screens.settle.SettleScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Entry.route,
        modifier = modifier,
    ) {
        composable(route = Screen.Entry.route) {
            EntryScreen(
                onNavigateToFriends = {
                    navController.navigate(Screen.Crew.route) {
                        popUpTo(Screen.Entry.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
        composable(route = Screen.Crew.route) { backStackEntry ->
            // Scope FriendsViewModel to the Crew nav back stack entry so it survives
            // config changes but is also accessible from AddFriend via the same parent.
            val friendsViewModel: FriendsViewModel = hiltViewModel(backStackEntry)

            // Pull session data from EntryViewModel (scoped to Entry's back stack entry)
            val entryBackStackEntry = navController.getBackStackEntry(Screen.Entry.route)
            val entryViewModel: EntryViewModel = hiltViewModel(entryBackStackEntry)
            val entryState by entryViewModel.uiState.collectAsStateWithLifecycle()

            // Forward amount + description whenever they change
            LaunchedEffect(entryState.rawAmount, entryState.description) {
                val amount = entryState.rawAmount.toDoubleOrNull() ?: 0.0
                friendsViewModel.setSessionData(
                    totalAmount = amount,
                    description = entryState.description,
                )
            }

            CrewScreen(
                onAddFriend = { navController.navigate(Screen.AddFriend.route) },
                onNavigateToSettle = {
                    navController.navigate(Screen.Settle.route) {
                        popUpTo(Screen.Entry.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                viewModel = friendsViewModel,
            )
        }
        composable(route = Screen.Settle.route) {
            SettleScreen()
        }
        composable(route = Screen.AddFriend.route) {
            // Reuse the FriendsViewModel scoped to Crew so selections are preserved
            val crewBackStackEntry = navController.getBackStackEntry(Screen.Crew.route)
            val friendsViewModel: FriendsViewModel = hiltViewModel(crewBackStackEntry)

            AddFriendScreen(
                onDismiss = { navController.popBackStack() },
                viewModel = friendsViewModel,
            )
        }
    }
}
