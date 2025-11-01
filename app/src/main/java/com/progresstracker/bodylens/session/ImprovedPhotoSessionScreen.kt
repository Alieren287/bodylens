package com.progresstracker.bodylens.session

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.progresstracker.bodylens.data.entity.BodyPart
import java.util.concurrent.Executor

/**
 * Improved photo session with instant camera and body part slider
 */
@Composable
fun ImprovedPhotoSessionScreen(
    onComplete: (Long) -> Unit,
    onCancel: () -> Unit,
    viewModel: PhotoSessionViewModel = viewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val activeBodyParts by viewModel.activeBodyParts.collectAsState()
    val currentIndex by viewModel.currentBodyPartIndex.collectAsState()
    val capturedPhotos by viewModel.capturedPhotos.collectAsState()

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Camera permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Start session on first composition
    LaunchedEffect(Unit) {
        if (sessionState is SessionState.Idle) {
            viewModel.startSession()
        }
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
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

    if (!hasCameraPermission) {
        // Permission screen
        PermissionScreen(
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onCancel = onCancel
        )
    } else if (activeBodyParts.isEmpty()) {
        // No body parts configured
        NoBodyPartsScreen(onCancel = onCancel)
    } else {
        // Main camera screen with body part slider
        CameraWithBodyPartSlider(
            bodyParts = activeBodyParts,
            currentIndex = currentIndex,
            capturedPhotos = capturedPhotos,
            sessionState = sessionState,
            onBodyPartSelected = { index ->
                // Update current index without starting new session
                viewModel.moveToIndex(index)
            },
            onPhotoCaptured = { photoData ->
                viewModel.capturePhoto(photoData)
            },
            onComplete = {
                viewModel.completeSession()
            },
            onCancel = {
                viewModel.cancelSession()
            }
        )
    }
}

@Composable
private fun PermissionScreen(
    onRequestPermission: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Camera Permission Required",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "BodyLens needs camera access to take progress photos",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Permission")
            }
            TextButton(onClick = onCancel) {
                Text("Cancel", color = Color.White)
            }
        }
    }
}

@Composable
private fun NoBodyPartsScreen(onCancel: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "No Body Parts Configured",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Please configure at least one body part in settings",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Button(onClick = onCancel) {
                Text("Go Back")
            }
        }
    }
}

@Composable
private fun CameraWithBodyPartSlider(
    bodyParts: List<BodyPart>,
    currentIndex: Int,
    capturedPhotos: Map<Long, Boolean>,
    sessionState: SessionState,
    onBodyPartSelected: (Int) -> Unit,
    onPhotoCaptured: (ByteArray) -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to current body part
    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem(currentIndex)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val imageCaptureBuilder = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    imageCapture = imageCaptureBuilder.build()
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            Text(
                text = "${capturedPhotos.size} / ${bodyParts.size} captured",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onComplete,
                enabled = capturedPhotos.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Complete",
                    tint = if (capturedPhotos.isNotEmpty()) Color.White else Color.Gray
                )
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Body parts horizontal slider
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(bodyParts.size) { index ->
                    val bodyPart = bodyParts[index]
                    val isSelected = index == currentIndex
                    val isCaptured = capturedPhotos[bodyPart.id] == true

                    BodyPartChip(
                        bodyPart = bodyPart,
                        isSelected = isSelected,
                        isCaptured = isCaptured,
                        onClick = { onBodyPartSelected(index) }
                    )
                }
            }

            // Camera controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flip camera
                IconButton(onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip camera",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Capture button
                IconButton(
                    onClick = {
                        if (!isCapturing && imageCapture != null) {
                            isCapturing = true
                            capturePhoto(
                                imageCapture = imageCapture!!,
                                executor = executor,
                                onPhotoCaptured = { bytes ->
                                    isCapturing = false
                                    onPhotoCaptured(bytes)
                                },
                                onError = {
                                    isCapturing = false
                                }
                            )
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    enabled = !isCapturing && sessionState !is SessionState.Saving
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = when {
                            isCapturing || sessionState is SessionState.Saving -> Color.Gray
                            else -> Color.White
                        }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isCapturing || sessionState is SessionState.Saving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = Color.Black
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Camera,
                                    contentDescription = "Take photo",
                                    tint = Color.Black,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                }

                // Placeholder for symmetry
                Spacer(modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun BodyPartChip(
    bodyPart: BodyPart,
    isSelected: Boolean,
    isCaptured: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isCaptured -> MaterialTheme.colorScheme.primaryContainer
            else -> Color.White.copy(alpha = 0.3f)
        }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = when {
                        isSelected -> Color.White
                        isCaptured -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> Color.White
                    }
                )
                if (isCaptured) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Captured",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                    )
                }
            }
            Text(
                text = bodyPart.name,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.White
                    isCaptured -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> Color.White
                },
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

private fun capturePhoto(
    imageCapture: ImageCapture,
    executor: Executor,
    onPhotoCaptured: (ByteArray) -> Unit,
    onError: () -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                onPhotoCaptured(bytes)
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                onError()
            }
        }
    )
}
