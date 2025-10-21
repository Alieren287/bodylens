package com.example.bodylens.ui.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object Camera : Screen("camera/{entryId}") {
        fun createRoute(entryId: Long) = "camera/$entryId"
    }
    data object Progress : Screen("progress/{entryId}") {
        fun createRoute(entryId: Long) = "progress/$entryId"
    }
    data object Gallery : Screen("gallery")
    data object AIInsights : Screen("ai_insights")
    data object Settings : Screen("settings")
}



