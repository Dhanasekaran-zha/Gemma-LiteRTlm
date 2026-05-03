package com.chat.navigation


import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.chat.ui.chat.ChatScreen
import com.ui.navigation.Screen

fun NavGraphBuilder.chatGraph(navController: NavHostController) {
    composable(route = Screen.Chat.route) {
        ChatScreen()
    }
}