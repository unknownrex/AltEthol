package com.unknownrex.altethol.feature.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.unknownrex.altethol.feature.auth.login.LoginRoot
import com.unknownrex.altethol.feature.auth.sessioncheck.SessionCheckRoot
import com.unknownrex.altethol.feature.home.HomeRoot

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

fun NavGraphBuilder.homeGraph() {
    composable<HomeRoute> {
        HomeRoot()
    }
}
