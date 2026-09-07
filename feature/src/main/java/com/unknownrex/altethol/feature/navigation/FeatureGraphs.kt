package com.unknownrex.altethol.feature.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.unknownrex.altethol.feature.auth.login.LoginRoot
import com.unknownrex.altethol.feature.auth.sessioncheck.SessionCheckRoot
import com.unknownrex.altethol.feature.history.HistoryRoot
import com.unknownrex.altethol.feature.home.HomeRoot
import com.unknownrex.altethol.feature.settings.SettingsRoot

fun NavGraphBuilder.authGraph(
    navController: NavController,
) {
    composable<SessionCheckRoute> {
        SessionCheckRoot(
            onLoginRequired = {
                navController.navigate(LoginRoute) {
                    popUpTo(SessionCheckRoute) { inclusive = true }
                }
            },
            onAuthenticated = {
                navController.navigate(HomeRoute) {
                    popUpTo(SessionCheckRoute) { inclusive = true }
                }
            },
        )
    }
    composable<LoginRoute> {
        LoginRoot(
            onLoginSuccess = {
                navController.navigate(HomeRoute) {
                    popUpTo(LoginRoute) { inclusive = true }
                }
            },
        )
    }
}

fun NavGraphBuilder.homeGraph(navController: NavController) {
    composable<HomeRoute> {
        HomeRoot(
            onOpenHistory = { navController.navigate(HistoryRoute) },
            onOpenSettings = { navController.navigate(SettingsRoute) },
        )
    }
}

fun NavGraphBuilder.historyGraph(navController: NavController) {
    composable<HistoryRoute> {
        HistoryRoot(
            onBack = { navController.popBackStack() },
        )
    }
}

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    composable<SettingsRoute> {
        SettingsRoot(
            onBack = { navController.popBackStack() },
            onLoggedOut = {
                navController.navigate(LoginRoute) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            },
        )
    }
}
