package com.quicksettle.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.quicksettle.presentation.screens.crew.AddFriendScreen
import com.quicksettle.presentation.screens.crew.CrewScreen
import com.quicksettle.presentation.screens.crew.FriendsViewModel
import com.quicksettle.presentation.screens.crew.computeSplit
import com.quicksettle.presentation.screens.entry.EntryScreen
import com.quicksettle.presentation.screens.entry.EntryViewModel
import com.quicksettle.presentation.screens.profile.ProfileScreen
import com.quicksettle.presentation.screens.settle.SettleScreen
import com.quicksettle.presentation.screens.settle.SettleViewModel
import com.quicksettle.presentation.screens.settle.SplitEntry

private const val ROOT_ROUTE = "root"

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Entry.route,
        route = ROOT_ROUTE,
        modifier = modifier,
    ) {
        composable(route = Screen.Entry.route) {
            // Scope to nav graph root so all tabs share the same instance
            val rootEntry = remember(navController) {
                navController.getBackStackEntry(ROOT_ROUTE)
            }
            val entryViewModel: EntryViewModel = hiltViewModel(rootEntry)

            EntryScreen(
                onNavigateToFriends = {
                    navController.navigate(Screen.Crew.route) {
                        popUpTo(Screen.Entry.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                viewModel = entryViewModel,
            )
        }
        composable(route = Screen.Crew.route) {
            val rootEntry = remember(navController) {
                navController.getBackStackEntry(ROOT_ROUTE)
            }
            val entryViewModel: EntryViewModel = hiltViewModel(rootEntry)
            val friendsViewModel: FriendsViewModel = hiltViewModel(rootEntry)
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
            val rootEntry = remember(navController) {
                navController.getBackStackEntry(ROOT_ROUTE)
            }
            val entryViewModel: EntryViewModel = hiltViewModel(rootEntry)
            val friendsViewModel: FriendsViewModel = hiltViewModel(rootEntry)
            val settleViewModel: SettleViewModel = hiltViewModel()

            val entryState by entryViewModel.uiState.collectAsStateWithLifecycle()
            val friendsState by friendsViewModel.uiState.collectAsStateWithLifecycle()

            // Re-read profile every time the Settle tab enters composition
            // (covers the case where the user updated their VPA on the Profile screen).
            LaunchedEffect(Unit) { settleViewModel.refreshProfile() }

            // Forward split data whenever friends state changes
            LaunchedEffect(friendsState.selectedFriends, friendsState.splitMode, friendsState.includeSelfInSplit) {
                val (amounts, _) = friendsState.computeSplit(
                    com.quicksettle.domain.usecase.CalculateSplitUseCase(),
                )
                val description = entryState.description.ifBlank { "Bill Split" }
                val splits = friendsState.selectedFriends
                    .filter { sf -> (amounts[sf.friend.id] ?: 0.0) > 0.0 }
                    .map { sf ->
                        SplitEntry(
                            name = sf.friend.name,
                            amount = amounts[sf.friend.id] ?: 0.0,
                            upiId = sf.friend.upiId,
                        )
                    }
                settleViewModel.setSettlementData(description = description, splits = splits)
            }

            SettleScreen(viewModel = settleViewModel)
        }
        composable(route = Screen.AddFriend.route) {
            val rootEntry = remember(navController) {
                navController.getBackStackEntry(ROOT_ROUTE)
            }
            val friendsViewModel: FriendsViewModel = hiltViewModel(rootEntry)

            AddFriendScreen(
                onDismiss = { navController.popBackStack() },
                viewModel = friendsViewModel,
            )
        }
        composable(route = Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
