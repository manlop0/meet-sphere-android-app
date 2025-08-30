package com.example.meetsphere.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.meetsphere.ui.auth.AuthScreen
import com.example.meetsphere.ui.main.MainScreen
import com.example.meetsphere.ui.splash.SplashScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }

        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Main.route) {
            MainScreen()
        }

        composable(
            route = Screen.ActivityDetails.route,
        ) { backStackEntry ->
            // val activityId = backStackEntry.arguments?.getString("activityId")
            // ActivityDetailsScreen(activityId = activityId, navController = navController)
        }

        composable(route = Screen.Chat.route) {
            // ChatScreen()
        }
    }
}
