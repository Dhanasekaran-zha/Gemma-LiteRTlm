package com.settings.navigation


import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.settings.ui.SettingsScreen
import com.ui.navigation.Screen

fun NavGraphBuilder.settingsGraph(navController: NavHostController) {
    composable(route = Screen.Settings.route) {
        SettingsScreen()
    }
}