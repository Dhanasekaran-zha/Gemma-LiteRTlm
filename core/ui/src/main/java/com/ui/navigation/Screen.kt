package com.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object EmployeeDummy : Screen("employee_dummy")
    data object Clients : Screen("clients")
    data object User : Screen("user")
    data object Chat : Screen("chat")
    data object Settings : Screen("settings")
}