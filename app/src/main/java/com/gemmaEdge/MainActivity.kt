package com.gemmaEdge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.gemmaEdge.navigation.RootNavGraph
import com.gemmaEdge.ui.theme.GemmaEdgeTheme
import com.ui.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            GemmaEdgeTheme {
                val navController = rememberNavController()
                val startRoute = Screen.Chat
                RootNavGraph(
                        navController = navController,
                        startDestination = startRoute
                )
            }
        }
    }
}