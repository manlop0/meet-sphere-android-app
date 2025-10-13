package com.example.meetsphere.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.meetsphere.R
import com.example.meetsphere.ui.activities.ActivitiesScreen
import com.example.meetsphere.ui.chatList.ChatListScreen
import com.example.meetsphere.ui.map.MapScreen
import com.example.meetsphere.ui.navigation.Screen

@Composable
fun MainScreen(navController: NavController) {
    val bottomBarNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by bottomBarNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val bottomBarItems =
                    listOf(
                        Screen.Map,
                        Screen.ActivityList,
                        Screen.ChatList,
                    )

                bottomBarItems.forEach { screen ->
                    NavigationBarItem(
                        label = { Text(text = screen.route.replace("_screen", "").replace("_", " ")) },
                        icon = { Icon(painterResource(id = getIconForScreen(screen)), contentDescription = null) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            bottomBarNavController.navigate(screen.route) {
                                popUpTo(bottomBarNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = bottomBarNavController,
            startDestination = Screen.Map.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Map.route) { MapScreen(navController = navController, bottomBarNavController = bottomBarNavController) }
            composable(Screen.ActivityList.route) { ActivitiesScreen() }
            composable(Screen.ChatList.route) { ChatListScreen(navController) }
        }
    }
}

@Composable
private fun getIconForScreen(screen: Screen): Int =
    when (screen) {
        Screen.Map -> R.drawable.ic_map
        Screen.ActivityList -> R.drawable.ic_activities
        Screen.ChatList -> R.drawable.ic_chats
        else -> R.drawable.ic_map
    }
