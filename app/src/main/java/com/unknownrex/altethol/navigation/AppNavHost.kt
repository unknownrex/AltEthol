package com.unknownrex.altethol.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.unknownrex.altethol.feature.navigation.SessionCheckRoute
import com.unknownrex.altethol.feature.navigation.authGraph
import com.unknownrex.altethol.feature.navigation.homeGraph

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SessionCheckRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
        authGraph(
            navController = navController,
        )
        homeGraph()
        }
    }
}
