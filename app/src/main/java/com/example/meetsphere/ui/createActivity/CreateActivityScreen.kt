package com.example.meetsphere.ui.createActivity

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.meetsphere.util.Constants.MAX_ACTIVITY_RADIUS_METERS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateActivityScreen(
    navController: NavController,
    viewModel: CreateActivityViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.createSuccess) {
        if (uiState.createSuccess) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Activity creation") })
        },
        bottomBar = {
            BottomButtonPanel(
                isCreating = uiState.isCreating,
                isCreateButtonEnabled = uiState.description.isNotBlank(),
                onCreateClick = { viewModel.onCreateActivity() },
                onCancelClick = { navController.popBackStack() },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
        ) {
            DescriptionSection(
                description = uiState.description,
                onDescriptionChange = viewModel::onDescriptionChange,
            )
            Spacer(modifier = Modifier.height(24.dp))

            LocationSettingsSection(
                showLocation = uiState.showLocation,
                onShowLocationToggle = viewModel::onShowLocationToggle,
                radius = uiState.radius,
                onRadiusChange = viewModel::onRadiusChange,
            )
        }
    }
}

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun DescriptionSection(
    description: String,
    onDescriptionChange: (String) -> Unit,
) {
    SectionCard(icon = Icons.AutoMirrored.Filled.Notes, title = "Description") {
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth().height(120.dp),
            placeholder = { Text("What would you like to do?") },
            label = { Text("Describe your activity") },
        )
    }
}

@Composable
private fun LocationSettingsSection(
    showLocation: Boolean,
    onShowLocationToggle: (Boolean) -> Unit,
    radius: Float,
    onRadiusChange: (Float) -> Unit,
) {
    SectionCard(icon = Icons.Default.Radar, title = "Visibility settings") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Show on the map", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = showLocation, onCheckedChange = onShowLocationToggle)
        }
        Column(modifier = Modifier.animateContentSize()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Radius of visibility: ${formatRadius(radius)}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Slider(
                value = radius,
                onValueChange = onRadiusChange,
                valueRange = 100f..MAX_ACTIVITY_RADIUS_METERS.toFloat(),
                steps = ((MAX_ACTIVITY_RADIUS_METERS - 100) / 50).toInt() - 1,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BottomButtonPanel(
    isCreating: Boolean,
    isCreateButtonEnabled: Boolean,
    onCreateClick: () -> Unit,
    onCancelClick: () -> Unit,
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
                onClick = onCancelClick,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onCreateClick,
                enabled = isCreateButtonEnabled && !isCreating,
                modifier = Modifier.weight(1f),
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Create")
                }
            }
        }
    }
}

private fun formatRadius(radius: Float): String =
    if (radius >= 1000) {
        "%.2f км".format(radius / 1000)
    } else {
        "${radius.toInt()} м"
    }
