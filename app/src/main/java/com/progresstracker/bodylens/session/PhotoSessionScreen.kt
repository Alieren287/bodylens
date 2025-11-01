package com.progresstracker.bodylens.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.progresstracker.bodylens.camera.CameraScreen
import com.progresstracker.bodylens.data.entity.BodyPart

/**
 * Photo session flow screen
 * Guides user through taking photos of each body part
 */
@Composable
fun PhotoSessionScreen(
    onComplete: (Long) -> Unit,
    onCancel: () -> Unit,
    viewModel: PhotoSessionViewModel = viewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val activeBodyParts by viewModel.activeBodyParts.collectAsState()
    val currentIndex by viewModel.currentBodyPartIndex.collectAsState()
    val capturedPhotos by viewModel.capturedPhotos.collectAsState()

    var showCamera by remember { mutableStateOf(false) }

    // Start session on first composition
    LaunchedEffect(Unit) {
        if (sessionState is SessionState.Idle) {
            viewModel.startSession()
        }
    }

    // Handle session completion
    LaunchedEffect(sessionState) {
        if (sessionState is SessionState.Complete) {
            val sessionId = (sessionState as SessionState.Complete).sessionId
            onComplete(sessionId)
        } else if (sessionState is SessionState.Cancelled) {
            onCancel()
        }
    }

    when {
        showCamera -> {
            val currentBodyPart = viewModel.getCurrentBodyPart()
            if (currentBodyPart != null) {
                CameraScreen(
                    bodyPartName = currentBodyPart.name,
                    onPhotoCaptured = { photoData ->
                        viewModel.capturePhoto(photoData)
                        showCamera = false
                    },
                    onClose = {
                        showCamera = false
                    }
                )
            }
        }

        else -> {
            SessionGuidanceScreen(
                sessionState = sessionState,
                activeBodyParts = activeBodyParts,
                currentIndex = currentIndex,
                capturedPhotos = capturedPhotos,
                onTakePhoto = { showCamera = true },
                onSkip = { viewModel.skipCurrent() },
                onPrevious = { viewModel.moveToPrevious() },
                onCancel = { viewModel.cancelSession() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionGuidanceScreen(
    sessionState: SessionState,
    activeBodyParts: List<BodyPart>,
    currentIndex: Int,
    capturedPhotos: Map<Long, Boolean>,
    onTakePhoto: () -> Unit,
    onSkip: () -> Unit,
    onPrevious: () -> Unit,
    onCancel: () -> Unit
) {
    val currentBodyPart = activeBodyParts.getOrNull(currentIndex)
    val progress = Pair(currentIndex + 1, activeBodyParts.size)
    val hasCaptured = currentBodyPart?.let { capturedPhotos[it.id] == true } ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Photo Session")
                        Text(
                            text = "Step ${progress.first} of ${progress.second}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { progress.first.toFloat() / progress.second.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Current body part info
            if (currentBodyPart != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = if (hasCaptured) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Text(
                        text = currentBodyPart.name,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (hasCaptured) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Photo captured",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Text(
                            text = "Position yourself to capture this angle",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Show error if any
                    if (sessionState is SessionState.Error) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = sessionState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Take/Retake photo button
                Button(
                    onClick = onTakePhoto,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = sessionState !is SessionState.Saving
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (hasCaptured) "Retake Photo" else "Take Photo")
                }

                // Skip button
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = sessionState !is SessionState.Saving
                ) {
                    Text("Skip This Part")
                }

                // Previous button (if not first)
                if (currentIndex > 0) {
                    TextButton(
                        onClick = onPrevious,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous")
                    }
                }
            }
        }
    }
}
