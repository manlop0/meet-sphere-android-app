package com.example.meetsphere.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.meetsphere.ui.activities.ActivityDetailsScreen
import com.example.meetsphere.ui.auth.AuthScreen
import com.example.meetsphere.ui.chat.ChatScreen
import com.example.meetsphere.ui.createActivity.CreateActivityScreen
import com.example.meetsphere.ui.main.MainScreen
import com.example.meetsphere.ui.permission.PermissionScreen
import com.example.meetsphere.ui.splash.SplashScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Permission.route,
    ) {
        composable(Screen.Permission.route) {
            PermissionScreen(navController)
        }

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
            MainScreen(navController)
        }

        composable(
            route = Screen.CreateActivity.route,
            arguments =
                listOf(
                    navArgument("latitude") { type = NavType.StringType },
                    navArgument("longitude") { type = NavType.StringType },
                ),
        ) {
            CreateActivityScreen(navController = navController)
        }

        composable(
            route = Screen.ActivityDetails.route,
            arguments = listOf(navArgument("activityId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getString("activityId") ?: ""
            ActivityDetailsScreen(
                activityId = activityId,
                navController = navController,
            )
        }

        composable(route = Screen.Chat.route) { backStackEntry ->
            ChatScreen(navController = navController)
        }
    }
}
