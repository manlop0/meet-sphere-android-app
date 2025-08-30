package com.example.meetsphere.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.meetsphere.ui.navigation.Screen

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val splashState by viewModel.splashState.collectAsState()

    LaunchedEffect(splashState) {
        when (splashState) {
            is SplashState.UserLoggedIn -> {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            is SplashState.UserNotLoggedIn -> {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            is SplashState.Loading -> {
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
