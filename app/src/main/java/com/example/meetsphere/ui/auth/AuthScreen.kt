package com.example.meetsphere.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthSuccess: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSignInMode by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthSuccess()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val textFieldColors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                errorTextColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor = MaterialTheme.colorScheme.surface,
            )

        if (!isSignInMode) {
            OutlinedTextField(
                value = uiState.username,
                onValueChange = { viewModel.onUsernameChange(it) },
                label = { Text("Username") },
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = uiState.email,
            onValueChange = { viewModel.onEmailChange(it) },
            label = { Text("Email") },
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.password,
            onValueChange = { viewModel.onPasswordChange(it) },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isSignInMode) {
            Button(
                onClick = { viewModel.signIn() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign In")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Don't have an account? Sign Up",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { isSignInMode = false },
            )
        } else {
            Button(
                onClick = { viewModel.signUp() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign Up")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Already have an account? Sign In",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { isSignInMode = true },
            )
        }

        uiState.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
        }
    }
}
