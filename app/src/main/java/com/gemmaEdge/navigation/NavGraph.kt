package com.gemmaEdge.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.chat.navigation.chatGraph
import com.settings.navigation.settingsGraph
import com.ui.navigation.Screen


@Composable
fun RootNavGraph(navController: NavHostController, startDestination: Screen) {
    NavHost(
            navController = navController,
            startDestination = startDestination.route
    ) {
        chatGraph(navController = navController)
        settingsGraph(navController = navController)
    }
}