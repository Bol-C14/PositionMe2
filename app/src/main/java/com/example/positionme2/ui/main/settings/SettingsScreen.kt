package com.example.positionme2.ui.main.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            SettingsSection(
                title = "Account",
                icon = Icons.Default.AccountCircle
            ) {
                SettingsItem(
                    title = "Profile",
                    subtitle = "Manage your profile and account details",
                    onClick = { /* TODO: Navigate to profile */ }
                )
                SettingsItem(
                    title = "Sign Out",
                    subtitle = "Sign out of your account",
                    onClick = { /* TODO: Handle sign out */ }
                )
            }
        }

        item {
            SettingsSection(
                title = "Map & Navigation",
                icon = Icons.Default.Map
            ) {
                var mapStyle by remember { mutableStateOf("Standard") }
                var units by remember { mutableStateOf("Metric") }

                SettingsDropdownItem(
                    title = "Map Style",
                    subtitle = "Choose your preferred map appearance",
                    value = mapStyle,
                    options = listOf("Standard", "Satellite", "Terrain", "Hybrid"),
                    onValueChange = { mapStyle = it }
                )

                SettingsDropdownItem(
                    title = "Units",
                    subtitle = "Distance and speed units",
                    value = units,
                    options = listOf("Metric", "Imperial"),
                    onValueChange = { units = it }
                )

                var voiceGuidance by remember { mutableStateOf(true) }
                SettingsSwitchItem(
                    title = "Voice Guidance",
                    subtitle = "Enable voice navigation instructions",
                    checked = voiceGuidance,
                    onCheckedChange = { voiceGuidance = it }
                )
            }
        }

        item {
            SettingsSection(
                title = "Recording & Data",
                icon = Icons.Default.DataUsage
            ) {
                var highAccuracy by remember { mutableStateOf(true) }
                var autoSave by remember { mutableStateOf(false) }

                SettingsSwitchItem(
                    title = "High Accuracy Mode",
                    subtitle = "Use GPS and sensors for maximum precision",
                    checked = highAccuracy,
                    onCheckedChange = { highAccuracy = it }
                )

                SettingsSwitchItem(
                    title = "Auto-save Recordings",
                    subtitle = "Automatically save completed recordings",
                    checked = autoSave,
                    onCheckedChange = { autoSave = it }
                )

                SettingsItem(
                    title = "Export Data",
                    subtitle = "Export your recorded trajectories",
                    onClick = { /* TODO: Handle export */ }
                )

                SettingsItem(
                    title = "Clear All Data",
                    subtitle = "Delete all recordings and cached data",
                    onClick = { /* TODO: Handle clear data */ }
                )
            }
        }

        item {
            SettingsSection(
                title = "Privacy & Permissions",
                icon = Icons.Default.Security
            ) {
                SettingsItem(
                    title = "Location Permissions",
                    subtitle = "Manage location access settings",
                    onClick = { /* TODO: Open permissions */ }
                )

                var shareAnalytics by remember { mutableStateOf(false) }
                SettingsSwitchItem(
                    title = "Share Analytics",
                    subtitle = "Help improve the app by sharing usage data",
                    checked = shareAnalytics,
                    onCheckedChange = { shareAnalytics = it }
                )
            }
        }

        item {
            SettingsSection(
                title = "About",
                icon = Icons.Default.Info
            ) {
                SettingsItem(
                    title = "Version",
                    subtitle = "1.0.0",
                    onClick = { }
                )

                SettingsItem(
                    title = "Help & Support",
                    subtitle = "Get help and contact support",
                    onClick = { /* TODO: Open help */ }
                )

                SettingsItem(
                    title = "Privacy Policy",
                    subtitle = "Read our privacy policy",
                    onClick = { /* TODO: Open privacy policy */ }
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun SettingsDropdownItem(
    title: String,
    subtitle: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = true },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onValueChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
