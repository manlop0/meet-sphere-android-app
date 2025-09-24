package com.example.meetsphere.ui.navigation

import org.osmdroid.util.GeoPoint

sealed class Screen(
    val route: String,
) {
    object Permission : Screen("permission_screen")

    object Splash : Screen("splash_screen")

    object Auth : Screen("auth_screen")

    object Main : Screen("main_screen")

    object Map : Screen("map_screen")

    object ActivityList : Screen("activity_list_screen")

    object ChatList : Screen("chat_list_screen")

    object CreateActivity : Screen("create_activity_screen/{latitude}/{longitude}") {
        fun createRoute(userLocation: GeoPoint) =
            "create_activity_screen/${userLocation.latitude}/${userLocation.longitude}"
    }

    object ActivityDetails : Screen("activity_details_screen/{activityId}") {
        fun createRoute(activityId: String) = "activity_details_screen/$activityId"
    }

    object Chat : Screen("chat_screen/{chatId}") {
        fun createRoute(chatId: String) = "chat_screen/$chatId"
    }
}
