package com.progresstracker.bodylens.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import java.io.File

/**
 * Full-screen photo viewer with zoom and pan capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    sessionId: Long,
    photoIndex: Int,
    viewModel: SessionDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val photosWithBodyParts by viewModel.photosWithBodyParts.collectAsState()
    var currentIndex by remember { mutableStateOf(photoIndex) }

    // Transform state for zoom and pan
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)

        // Only allow panning when zoomed in
        if (scale > 1f) {
            val maxX = (scale - 1f) * 500f
            val maxY = (scale - 1f) * 800f
            offset = Offset(
                x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                y = (offset.y + panChange.y).coerceIn(-maxY, maxY)
            )
        } else {
            offset = Offset.Zero
        }
    }

    // Reset zoom when changing photos
    LaunchedEffect(currentIndex) {
        scale = 1f
        offset = Offset.Zero
    }

    if (photosWithBodyParts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val currentPhoto = photosWithBodyParts.getOrNull(currentIndex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Photo with zoom and pan
        currentPhoto?.let { photoWithBodyPart ->
            Image(
                painter = rememberAsyncImagePainter(File(photoWithBodyPart.photo.filePath)),
                contentDescription = photoWithBodyPart.bodyPart?.name,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = transformState),
                contentScale = ContentScale.Fit
            )
        }

        // Top bar with back button and info
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = currentPhoto?.bodyPart?.name ?: "Photo",
                        color = Color.White
                    )
                    Text(
                        text = "${currentIndex + 1} of ${photosWithBodyParts.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.5f)
            )
        )

        // Navigation controls (Previous/Next)
        if (photosWithBodyParts.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Previous button
                if (currentIndex > 0) {
                    FloatingActionButton(
                        onClick = { currentIndex-- },
                        containerColor = Color.White.copy(alpha = 0.9f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Previous photo",
                            tint = Color.Black
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(56.dp))
                }

                // Zoom indicator
                Surface(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Pinch to zoom: ${String.format("%.1f", scale)}x",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black
                    )
                }

                // Next button
                if (currentIndex < photosWithBodyParts.size - 1) {
                    FloatingActionButton(
                        onClick = { currentIndex++ },
                        containerColor = Color.White.copy(alpha = 0.9f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = "Next photo",
                            tint = Color.Black
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(56.dp))
                }
            }
        }
    }
}
