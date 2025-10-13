package com.example.meetsphere.ui.activities

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.meetsphere.domain.model.Activity
import com.example.meetsphere.ui.activityDetails.ActivityDetailsUiState
import com.example.meetsphere.ui.activityDetails.ActivityDetailsViewModel
import com.example.meetsphere.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailsScreen(
    activityId: String,
    navController: NavController,
    viewModel: ActivityDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(activityId) {
        viewModel.loadActivityDetails(activityId)
    }

    val isCreatingChat by viewModel.isCreatingChat.collectAsState()
    val chatError by viewModel.chatError.collectAsState()

    val navigateToChat by viewModel.navigateToChat.collectAsState(initial = null)
    LaunchedEffect(navigateToChat) {
        navigateToChat?.let { chatId ->
            navController.navigate(Screen.Chat.createRoute(chatId))
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(chatError) {
        chatError?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "OK",
                duration = SnackbarDuration.Short,
            )
            viewModel.clearChatError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Activity details") })
        },
        bottomBar = {
            BottomButtonPanel(
                onBackClick = { navController.popBackStack() },
                onMessageClick = {
                    val creatorId = (uiState as? ActivityDetailsUiState.Success)?.activity?.creatorId ?: ""
                    viewModel.onMessageClick(creatorId)
                },
                isCreatingChat = isCreatingChat,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { paddingValues ->
        when (uiState) {
            is ActivityDetailsUiState.Loading -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            is ActivityDetailsUiState.Success -> {
                val activity = (uiState as ActivityDetailsUiState.Success).activity
                ActivityDetailsContent(
                    activity = activity,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                )
            }
            is ActivityDetailsUiState.Error -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Error: ${(uiState as ActivityDetailsUiState.Error).message}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityDetailsContent(
    activity: Activity,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CreatorSection(
            creatorName = activity.creatorName,
            createdAt = activity.createdAt,
        )

        Spacer(modifier = Modifier.height(16.dp))

        DescriptionSection(
            fullDescription = activity.fullDescription,
            shortDescription = activity.shortDescription,
        )
    }
}

@Composable
private fun SectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.padding(horizontal = 12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun CreatorSection(
    creatorName: String,
    createdAt: java.util.Date?,
) {
    SectionCard(icon = Icons.Default.AccountCircle, title = "Creator") {
        Text(
            text = creatorName,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (createdAt != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Created at: $createdAt",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DescriptionSection(
    fullDescription: String,
    shortDescription: String,
) {
    SectionCard(icon = Icons.Default.Description, title = "Description") {
        Text(
            text = fullDescription,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.animateContentSize(),
        )
        if (fullDescription != shortDescription) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Briefly: $shortDescription",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BottomButtonPanel(
    onBackClick: () -> Unit,
    onMessageClick: () -> Unit,
    isCreatingChat: Boolean,
) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.weight(1f),
            ) {
                Text("Back")
            }
            Button(
                onClick = onMessageClick,
                modifier = Modifier.weight(1f),
                enabled = !isCreatingChat,
            ) {
                if (isCreatingChat) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creating chat...")
                    }
                } else {
                    Text("Message")
                }
            }
        }
    }
}
