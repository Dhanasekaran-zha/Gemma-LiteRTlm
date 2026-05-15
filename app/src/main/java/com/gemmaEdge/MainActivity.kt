package com.gemmaEdge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.gemmaEdge.navigation.RootNavGraph
import com.ui.navigation.Screen
import com.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            val themeMode = viewModel.themeMode

            AppTheme(themeMode = themeMode) {
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