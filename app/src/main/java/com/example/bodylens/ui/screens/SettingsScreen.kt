package com.example.bodylens.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bodylens.BodyLensApp
import com.example.bodylens.data.model.PhotoGroup
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val app = remember { context.applicationContext as BodyLensApp }
    val scope = rememberCoroutineScope()
    
    var showResetDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var changePinState by remember { mutableStateOf(ChangePinState.EnterOld) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val isPinEnabled by app.userPreferences.isPinEnabled.collectAsState(initial = false)
    val enabledPhotoGroups by app.userPreferences.enabledPhotoGroups.collectAsState(initial = emptySet())
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Security",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                SettingItem(
                    icon = Icons.Default.Lock,
                    title = if (isPinEnabled) "Change PIN" else "Enable PIN",
                    subtitle = if (isPinEnabled) "Update your security PIN" else "Set up PIN protection",
                    onClick = { 
                        showChangePinDialog = true
                        changePinState = if (isPinEnabled) ChangePinState.EnterOld else ChangePinState.EnterNew
                        oldPin = ""
                        newPin = ""
                        confirmPin = ""
                        errorMessage = null
                    }
                )
            }
            
            item {
                SettingItem(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Lock",
                    subtitle = "Use fingerprint to unlock (Coming soon)",
                    onClick = { }
                )
            }
            
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            item {
                Text(
                    "Photo Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                Text(
                    "Choose which photo angles to capture",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Photo group toggles
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PhotoGroup.entries.filter { it != PhotoGroup.CUSTOM }.forEach { group ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = when (group) {
                                            PhotoGroup.FACE -> "Front camera selfie for facial progress"
                                            PhotoGroup.FRONT -> "Full body front view"
                                            PhotoGroup.BACK -> "Full body back view"
                                            PhotoGroup.SIDE_LEFT -> "Left side profile"
                                            PhotoGroup.SIDE_RIGHT -> "Right side profile"
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = group.name in enabledPhotoGroups,
                                    onCheckedChange = { enabled ->
                                        // Prevent disabling all photo groups
                                        if (enabled || enabledPhotoGroups.size > 1) {
                                            scope.launch {
                                                app.userPreferences.togglePhotoGroup(group.name, enabled)
                                            }
                                        }
                                    }
                                )
                            }
                            if (group != PhotoGroup.SIDE_RIGHT) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
            
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            item {
                Text(
                    "Data & Storage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                SettingItem(
                    icon = Icons.Default.CloudUpload,
                    title = "Backup & Export",
                    subtitle = "Export your progress data (Coming soon)",
                    onClick = { }
                )
            }
            
            item {
                SettingItem(
                    icon = Icons.Default.CloudDownload,
                    title = "Import Data",
                    subtitle = "Import previously exported data (Coming soon)",
                    onClick = { }
                )
            }
            
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            item {
                Text(
                    "AI Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                SettingItem(
                    icon = Icons.Default.Lightbulb,
                    title = "AI Analysis",
                    subtitle = "Configure AI-powered insights (Coming soon)",
                    onClick = { }
                )
            }
            
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            item {
                Text(
                    "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                SettingItem(
                    icon = Icons.Default.Info,
                    title = "App Version",
                    subtitle = "1.0.0",
                    onClick = { }
                )
            }
            
            item {
                SettingItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "View privacy information",
                    onClick = { }
                )
            }
            
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            item {
                Card(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column {
                            Text(
                                "Reset All Data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Permanently delete all progress data",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Reset All Data?") },
            text = { 
                Text("This will permanently delete all your progress photos, measurements, and settings. This action cannot be undone.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onLogout()
                    }
                ) {
                    Text("Delete Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showChangePinDialog) {
        ChangePinDialog(
            state = changePinState,
            currentPin = when (changePinState) {
                ChangePinState.EnterOld -> oldPin
                ChangePinState.EnterNew -> newPin
                ChangePinState.Confirm -> confirmPin
            },
            errorMessage = errorMessage,
            isPinEnabled = isPinEnabled,
            onPinChange = { digit ->
                when (changePinState) {
                    ChangePinState.EnterOld -> {
                        if (oldPin.length < 4) {
                            oldPin += digit
                            if (oldPin.length == 4) {
                                // Verify old PIN
                                scope.launch {
                                    val isValid = app.userPreferences.verifyPin(oldPin)
                                    if (isValid) {
                                        changePinState = ChangePinState.EnterNew
                                        errorMessage = null
                                    } else {
                                        errorMessage = "Incorrect PIN"
                                        oldPin = ""
                                    }
                                }
                            }
                        }
                    }
                    ChangePinState.EnterNew -> {
                        if (newPin.length < 4) {
                            newPin += digit
                            if (newPin.length == 4) {
                                changePinState = ChangePinState.Confirm
                                errorMessage = null
                            }
                        }
                    }
                    ChangePinState.Confirm -> {
                        if (confirmPin.length < 4) {
                            confirmPin += digit
                            if (confirmPin.length == 4) {
                                if (confirmPin == newPin) {
                                    // Save new PIN
                                    scope.launch {
                                        app.userPreferences.setPin(newPin)
                                        showChangePinDialog = false
                                        errorMessage = null
                                    }
                                } else {
                                    errorMessage = "PINs don't match"
                                    confirmPin = ""
                                    changePinState = ChangePinState.EnterNew
                                    newPin = ""
                                }
                            }
                        }
                    }
                }
            },
            onBackspace = {
                when (changePinState) {
                    ChangePinState.EnterOld -> {
                        if (oldPin.isNotEmpty()) oldPin = oldPin.dropLast(1)
                    }
                    ChangePinState.EnterNew -> {
                        if (newPin.isNotEmpty()) newPin = newPin.dropLast(1)
                    }
                    ChangePinState.Confirm -> {
                        if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                    }
                }
            },
            onDismiss = {
                showChangePinDialog = false
                oldPin = ""
                newPin = ""
                confirmPin = ""
                errorMessage = null
                changePinState = ChangePinState.EnterOld
            }
        )
    }
}

enum class ChangePinState {
    EnterOld,
    EnterNew,
    Confirm
}

@Composable
fun ChangePinDialog(
    state: ChangePinState,
    currentPin: String,
    errorMessage: String?,
    isPinEnabled: Boolean,
    onPinChange: (String) -> Unit,
    onBackspace: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (state) {
                    ChangePinState.EnterOld -> "Enter Current PIN"
                    ChangePinState.EnterNew -> if (isPinEnabled) "Enter New PIN" else "Set Your PIN"
                    ChangePinState.Confirm -> "Confirm New PIN"
                }
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                // PIN dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index < currentPin.length)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Number pad
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (row in 0..2) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 1..3) {
                                val number = row * 3 + col
                                FilledTonalButton(
                                    onClick = { onPinChange(number.toString()) },
                                    modifier = Modifier.size(60.dp),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = number.toString(),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                        }
                    }
                    
                    // Bottom row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Spacer(modifier = Modifier.size(60.dp))
                        
                        FilledTonalButton(
                            onClick = { onPinChange("0") },
                            modifier = Modifier.size(60.dp),
                            shape = CircleShape
                        ) {
                            Text("0", style = MaterialTheme.typography.titleLarge)
                        }
                        
                        IconButton(
                            onClick = onBackspace,
                            modifier = Modifier.size(60.dp)
                        ) {
                            Icon(Icons.Default.KeyboardBackspace, "Backspace")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}




