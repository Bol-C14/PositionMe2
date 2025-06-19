package com.example.positionme2.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save

@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit = {}) {
    val context = LocalContext.current
    val requiredPermissions = remember {
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                remove(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
    }
    var showRationale by rememberSaveable { mutableStateOf(false) }
    var allGranted by rememberSaveable { mutableStateOf(false) }
    var missingPermissions by rememberSaveable { mutableStateOf(listOf<String>()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.isEmpty()) {
            showRationale = true
            return@rememberLauncherForActivityResult
        }
        allGranted = permissions.values.all { it }
        if (!allGranted) {
            showRationale = true
            missingPermissions = permissions.filter { !it.value }.map { it.key }
        } else {
            onPermissionsGranted()
        }
    }
    LaunchedEffect(Unit) {
        val notGranted = requiredPermissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isEmpty()) {
            allGranted = true
            onPermissionsGranted()
        } else {
            allGranted = false
            missingPermissions = notGranted
        }
    }
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Permissions Required") },
            text = {
                Text("This app needs location, camera, and storage permissions to function.\nMissing: ${missingPermissions.joinToString()}" )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    permissionLauncher.launch(requiredPermissions.toTypedArray())
                }) { Text("Grant Again") }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) { Text("Cancel") }
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 0.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Spacer(Modifier.height(32.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_dialog_map),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(120.dp)
                )
            }
            Text(
                text = "App Permissions Required",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "To provide you with the best experience, PositionMe needs access to the following features:",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            // Location Card
            PermissionCard(
                icon = Icons.Default.LocationOn,
                title = "Location",
                description = "To show your current position on the map and provide navigation features",
                granted = missingPermissions.none { it.contains("LOCATION") },
                modifier = Modifier.padding(top = 24.dp)
            )
            // Sensor Card
            PermissionCard(
                icon = Icons.Default.CompassCalibration,
                title = "Sensors",
                description = "To calibrate the compass and provide accurate orientation on the map",
                granted = missingPermissions.none { it.contains("ACTIVITY_RECOGNITION") },
                modifier = Modifier.padding(top = 12.dp)
            )
            // Storage Card
            PermissionCard(
                icon = Icons.Default.Save,
                title = "Storage",
                description = "To save map data offline and store your favorite locations",
                granted = missingPermissions.none { it.contains("READ_EXTERNAL_STORAGE") } && missingPermissions.none { it.contains("READ_MEDIA_IMAGES") },
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                permissionLauncher.launch(requiredPermissions.toTypedArray())
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text("Grant Permissions")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (granted) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
