package com.progresstracker.bodylens.session

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.progresstracker.bodylens.data.entity.BodyPart
import kotlinx.coroutines.delay
import java.io.ByteArrayInputStream
import java.util.concurrent.Executor

/**
 * Decode image bytes and apply EXIF rotation
 */
private fun decodeAndRotateBitmap(photoBytes: ByteArray): Bitmap {
    // Decode the bitmap
    val bitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)

    // Read EXIF orientation
    val exif = ExifInterface(ByteArrayInputStream(photoBytes))
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )

    // Calculate rotation angle
    val rotationAngle = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    // If no rotation needed, return original bitmap
    if (rotationAngle == 0f) {
        return bitmap
    }

    // Apply rotation
    val matrix = Matrix()
    matrix.postRotate(rotationAngle)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

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
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    // Timer state
    var timerDuration by remember { mutableIntStateOf(0) } // 0 = off, 3, 5, 10 seconds
    var showTimerMenu by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var isCountingDown by remember { mutableStateOf(false) }

    // Photo viewer state (for viewing already captured photos)
    var viewingPhotoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showPhotoViewer by remember { mutableStateOf(false) }

    // Store captured photos by body part ID
    var capturedPhotosData by remember { mutableStateOf<Map<Long, ByteArray>>(emptyMap()) }

    // Request focus when screen appears
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Auto-scroll to current body part
    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem(currentIndex)
    }

    // Timer countdown effect
    LaunchedEffect(countdown, isCountingDown) {
        if (isCountingDown && countdown > 0) {
            delay(1000)
            countdown -= 1
        } else if (isCountingDown && countdown == 0) {
            isCountingDown = false
            // Trigger photo capture
            if (!isCapturing && imageCapture != null && currentIndex < bodyParts.size) {
                val currentBodyPart = bodyParts[currentIndex]
                isCapturing = true
                capturePhoto(
                    imageCapture = imageCapture!!,
                    executor = executor,
                    onPhotoCaptured = { bytes ->
                        isCapturing = false
                        // Auto-save immediately
                        capturedPhotosData = capturedPhotosData + (currentBodyPart.id to bytes)
                        onPhotoCaptured(bytes)
                    },
                    onError = {
                        isCapturing = false
                    }
                )
            }
        }
    }

    // Function to handle photo capture trigger
    fun triggerCapture() {
        if (!isCapturing && !isCountingDown && imageCapture != null &&
            sessionState !is SessionState.Saving && currentIndex < bodyParts.size) {
            if (timerDuration > 0) {
                // Start countdown
                countdown = timerDuration
                isCountingDown = true
            } else {
                // Immediate capture
                val currentBodyPart = bodyParts[currentIndex]
                isCapturing = true
                capturePhoto(
                    imageCapture = imageCapture!!,
                    executor = executor,
                    onPhotoCaptured = { bytes ->
                        isCapturing = false
                        // Auto-save immediately
                        capturedPhotosData = capturedPhotosData + (currentBodyPart.id to bytes)
                        onPhotoCaptured(bytes)
                    },
                    onError = {
                        isCapturing = false
                    }
                )
            }
        }
    }

    // Store preview view reference
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Rebind camera when lens facing changes
    LaunchedEffect(lensFacing, previewView) {
        if (previewView == null) return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView!!.surfaceProvider
            }

            // Set proper rotation for image capture
            val rotation = previewView!!.display.rotation
            val imageCaptureBuilder = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetRotation(rotation)
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
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusTarget()
            .onKeyEvent { keyEvent ->
                // Handle volume keys
                if (keyEvent.key == Key.VolumeDown || keyEvent.key == Key.VolumeUp) {
                    triggerCapture()
                    true // Consume the event
                } else {
                    false
                }
            }
    ) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView = it }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Countdown overlay (center of screen)
        if (isCountingDown && countdown > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = countdown.toString(),
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

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
                        onClick = {
                            // If already captured, show photo viewer
                            if (isCaptured && capturedPhotosData.containsKey(bodyPart.id)) {
                                viewingPhotoBytes = capturedPhotosData[bodyPart.id]
                                showPhotoViewer = true
                            } else {
                                // Otherwise, just switch to this body part
                                onBodyPartSelected(index)
                            }
                        }
                    )
                }
            }

            // Camera controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Flip camera button
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

                // Center: Capture button
                IconButton(
                    onClick = { triggerCapture() },
                    modifier = Modifier.size(80.dp),
                    enabled = !isCapturing && !isCountingDown && sessionState !is SessionState.Saving
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = when {
                            isCapturing || isCountingDown || sessionState is SessionState.Saving -> Color.Gray
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

                // Right side: Timer button with dropdown
                Box {
                    IconButton(onClick = { showTimerMenu = !showTimerMenu }) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer",
                            tint = if (timerDuration > 0) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Timer dropdown menu
                    DropdownMenu(
                        expanded = showTimerMenu,
                        onDismissRequest = { showTimerMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Off") },
                            onClick = {
                                timerDuration = 0
                                showTimerMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("3 seconds") },
                            onClick = {
                                timerDuration = 3
                                showTimerMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("5 seconds") },
                            onClick = {
                                timerDuration = 5
                                showTimerMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("10 seconds") },
                            onClick = {
                                timerDuration = 10
                                showTimerMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Photo Viewer Overlay (for viewing already captured photos with zoom)
        if (showPhotoViewer && viewingPhotoBytes != null) {
            ZoomablePhotoViewer(
                photoBytes = viewingPhotoBytes!!,
                bodyPartName = "Photo",
                onClose = {
                    showPhotoViewer = false
                    viewingPhotoBytes = null
                },
                onRetake = {
                    // Close viewer and camera will be ready for retaking
                    showPhotoViewer = false
                    viewingPhotoBytes = null
                    // Photo will be replaced when user captures a new one
                }
            )
        }
    }
}

@Composable
private fun ZoomablePhotoViewer(
    photoBytes: ByteArray,
    bodyPartName: String,
    onClose: () -> Unit,
    onRetake: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Display photo with zoom and pan (with EXIF rotation applied)
        val bitmap = remember(photoBytes) {
            decodeAndRotateBitmap(photoBytes)
        }

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Captured photo",
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)

                        // Only allow panning when zoomed in
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit
        )

        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            Text(
                text = bodyPartName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )

            // Zoom indicator
            if (scale > 1f) {
                Text(
                    text = "${(scale * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        // Bottom action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Reset Zoom button (only show when zoomed)
            if (scale > 1f) {
                Button(
                    onClick = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset Zoom", color = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            // Retake button
            Button(
                onClick = onRetake,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = "Retake",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retake", color = Color.White)
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
        onClick = onClick,
        modifier = Modifier.width(100.dp),
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
