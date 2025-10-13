package com.example.meetsphere.ui.activities

import android.R.attr.onClick
import android.graphics.drawable.Icon
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.meetsphere.domain.model.MapMarker
import com.example.meetsphere.ui.navigation.Screen

@Composable
fun ActivitiesScreen(
    navController: NavController,
    viewModel: ActivitiesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is ActivitiesNavigationEvent.ToActivityDetails -> {
                    navController.navigate(Screen.ActivityDetails.createRoute(event.activityId))
                }
                is ActivitiesNavigationEvent.ToChat -> {
                    navController.navigate(Screen.Chat.createRoute(event.chatId))
                }
                is ActivitiesNavigationEvent.ToCreateActivity -> {
                    navController.navigate(Screen.CreateActivity.createRoute(event.location))
                }
            }
        }
    }

    LaunchedEffect(state.chatError) {
        state.chatError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearChatError()
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            stickyHeader { SectionHeader(title = "My activity") }
            item {
                MyActivityBlock(
                    marker = state.myActivity,
                    loading = state.loading,
                    onCreateActivity = viewModel::onCreateActivity,
                    onOpenDetails = viewModel::onOpenDetails,
                    onClose = viewModel::onCloseMyActivity,
                )
            }

            stickyHeader { SectionHeader(title = "Other activities") }
            if (state.othersActivities.isEmpty() && !state.loading) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No activities nearby",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                items(state.othersActivities, key = { it.id }) { marker ->
                    OtherActivityItem(
                        marker = marker,
                        isCreatingChat = state.isCreatingChat,
                        onOpenDetails = { viewModel.onOpenDetails(marker.id) },
                        onOpenChat = { viewModel.onOpenChat(marker.creatorId) },
                    )
                    Divider()
                }
            }
        }

        if (state.loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun MyActivityBlock(
    marker: MapMarker?,
    loading: Boolean,
    onCreateActivity: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onClose: () -> Unit,
) {
    var showCloseDialog by remember { mutableStateOf(false) }

    when {
        loading -> {
            Card(Modifier.padding(16.dp).fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Loading your activity...")
                }
            }
        }
        marker == null -> {
            Card(Modifier.padding(16.dp).fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("You have no active activity", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Create an activity so people nearby can find you")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onCreateActivity, modifier = Modifier.fillMaxWidth()) {
                        Text("Create activity")
                    }
                }
            }
        }
        else -> {
            Card(Modifier.padding(16.dp).fillMaxWidth()) {
                Box(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .padding(16.dp)
                            .padding(end = 40.dp),
                    ) {
                        Text("Your activity", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(marker.shortDescription, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { onOpenDetails(marker.id) }) {
                                Text("More details")
                            }
                        }
                    }

                    IconButton(
                        onClick = { showCloseDialog = true },
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Close activity",
                            tint = Color.Red,
                        )
                    }
                }
            }

            if (showCloseDialog) {
                AlertDialog(
                    onDismissRequest = { showCloseDialog = false },
                    title = { Text("Close Activity?") },
                    text = { Text("Are you sure you want to close your activity? This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showCloseDialog = false
                                onClose()
                            },
                        ) {
                            Text("Close activity", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCloseDialog = false }) {
                            Text("Cancel")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun OtherActivityItem(
    marker: MapMarker,
    isCreatingChat: Boolean,
    onOpenDetails: () -> Unit,
    onOpenChat: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp),
        ) {
            Text(
                text = marker.creatorName,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = marker.shortDescription,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onOpenDetails) {
                    Text("More details")
                }
                OutlinedButton(
                    onClick = onOpenChat,
                    enabled = !isCreatingChat,
                ) {
                    if (isCreatingChat) {
                        CircularProgressIndicator(Modifier.size(16.dp))
                    } else {
                        Text("Chat")
                    }
                }
            }
        }
    }
}
