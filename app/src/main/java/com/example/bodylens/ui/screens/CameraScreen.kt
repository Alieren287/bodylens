package com.example.bodylens.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.bodylens.data.model.PhotoGroup
import com.example.bodylens.ui.viewmodel.CameraUiState
import com.example.bodylens.ui.viewmodel.CameraViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    entryId: Long,
    viewModel: CameraViewModel,
    onNavigateBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    
    val uiState by viewModel.uiState.collectAsState()
    val selectedPhotoGroup by viewModel.selectedPhotoGroup.collectAsState()
    val capturedPhotos by viewModel.capturedPhotos.collectAsState()
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()
    val enabledPhotoGroups by viewModel.enabledPhotoGroups.collectAsState()
    
    var showTipsDialog by remember { mutableStateOf(false) }
    var isCameraReady by remember { mutableStateOf(false) }
    
    // Create and initialize camera controller immediately
    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }
    
    // Bind camera to lifecycle immediately when permission is granted
    DisposableEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            try {
                cameraController.bindToLifecycle(lifecycleOwner)
                isCameraReady = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        onDispose {
            // Unbind camera when leaving screen
            cameraController.unbind()
        }
    }
    
    // Update camera selector when flip is toggled
    LaunchedEffect(isFrontCamera) {
        cameraController.cameraSelector = if (isFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    
    // Request permission immediately on screen entry
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    LaunchedEffect(uiState) {
        if (uiState is CameraUiState.PhotoSaved) {
            kotlinx.coroutines.delay(1000)
            viewModel.resetState()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture Progress Photo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showTipsDialog = true }) {
                        Icon(Icons.Default.Help, "Photo Tips")
                    }
                    IconButton(onClick = { viewModel.toggleCamera() }) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Camera"
                        )
                    }
                    // Show Done button after at least one photo is captured
                    if (viewModel.hasAnyPhotoCaptured()) {
                        TextButton(onClick = onComplete) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (cameraPermissionState.status.isGranted) {
            CameraContent(
                modifier = Modifier.padding(padding),
                cameraController = cameraController,
                lifecycleOwner = lifecycleOwner,
                selectedPhotoGroup = selectedPhotoGroup,
                capturedPhotos = capturedPhotos,
                enabledPhotoGroups = enabledPhotoGroups,
                uiState = uiState,
                isFrontCamera = isFrontCamera,
                onPhotoGroupSelected = { viewModel.selectPhotoGroup(it) },
                onCapturePhoto = { bitmap ->
                    viewModel.capturePhoto(entryId, bitmap)
                }
            )
        } else {
            CameraPermissionContent(
                modifier = Modifier.padding(padding),
                permissionState = cameraPermissionState
            )
        }
        
        // Photo tips dialog
        if (showTipsDialog) {
            PhotoTipsDialog(onDismiss = { showTipsDialog = false })
        }
    }
}

@Composable
fun CameraContent(
    modifier: Modifier = Modifier,
    cameraController: LifecycleCameraController,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    selectedPhotoGroup: PhotoGroup,
    capturedPhotos: Map<PhotoGroup, Boolean>,
    enabledPhotoGroups: List<PhotoGroup>,
    uiState: CameraUiState,
    isFrontCamera: Boolean,
    onPhotoGroupSelected: (PhotoGroup) -> Unit,
    onCapturePhoto: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    
    Box(modifier = modifier.fillMaxSize()) {
        // Camera Preview - optimized for instant display
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    // Just set the controller - already bound in DisposableEffect
                    controller = cameraController
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Top instruction bar with camera indicator
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = getInstructionText(selectedPhotoGroup),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                // Camera indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isFrontCamera) Icons.Default.CameraFront else Icons.Default.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isFrontCamera) "Front Camera" else "Back Camera",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Photo group selector with auto-scroll
        val photoGroups = enabledPhotoGroups // Use enabled photo groups from preferences
        val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
        
        // Auto-scroll to center selected photo group
        LaunchedEffect(selectedPhotoGroup) {
            val index = photoGroups.indexOf(selectedPhotoGroup)
            if (index >= 0) {
                lazyListState.animateScrollToItem(
                    index = index,
                    scrollOffset = -200 // Center the item
                )
            }
        }
        
        LazyRow(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(photoGroups) { group ->
                PhotoGroupChip(
                    group = group,
                    isSelected = group == selectedPhotoGroup,
                    isCaptured = capturedPhotos[group] == true,
                    onClick = { onPhotoGroupSelected(group) }
                )
            }
        }
        
        // Capture button and status
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState is CameraUiState.Saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(72.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (uiState is CameraUiState.PhotoSaved) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Saved",
                    modifier = Modifier.size(72.dp),
                    tint = Color.Green
                )
            } else {
                FloatingActionButton(
                    onClick = {
                        capturePhoto(cameraController, context) { bitmap ->
                            onCapturePhoto(bitmap)
                        }
                    },
                    modifier = Modifier.size(72.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGroupChip(
    group: PhotoGroup,
    isSelected: Boolean,
    isCaptured: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(group.displayName) },
        leadingIcon = if (isCaptured) {
            {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Captured",
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun PhotoTipsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Photo Tips for Best Results")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TipItem(
                    icon = Icons.Default.Lightbulb,
                    title = "Good Lighting",
                    description = "Use natural light or a well-lit room. Avoid harsh shadows and backlighting."
                )
                TipItem(
                    icon = Icons.Default.Person,
                    title = "Consistent Positioning",
                    description = "Stand in the same spot each time, about 6 feet (2 meters) from the camera."
                )
                TipItem(
                    icon = Icons.Default.CheckCircle,
                    title = "Proper Posture",
                    description = "Stand naturally with arms slightly away from your body. Keep shoulders relaxed."
                )
                TipItem(
                    icon = Icons.Default.GridOn,
                    title = "Use the Frame Guide",
                    description = "Align your entire body within the on-screen guide frame for consistent photos."
                )
                TipItem(
                    icon = Icons.Default.CameraFront,
                    title = "Face Photos",
                    description = "Use front camera for face shots. Look directly at the camera with a neutral expression."
                )
                TipItem(
                    icon = Icons.Default.Camera,
                    title = "Body Photos",
                    description = "Use back camera with a mirror or timer. Wear fitted or minimal clothing for accuracy."
                )
                TipItem(
                    icon = Icons.Default.FlipCameraAndroid,
                    title = "Switch Cameras",
                    description = "Tap the flip icon to switch between front and back cameras as needed."
                )
                TipItem(
                    icon = Icons.Default.Schedule,
                    title = "Same Time of Day",
                    description = "Take photos at the same time each session for consistent lighting and body state."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It!")
            }
        }
    )
}

@Composable
fun TipItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionContent(
    modifier: Modifier = Modifier,
    permissionState: com.google.accompanist.permissions.PermissionState
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (permissionState.status.shouldShowRationale) {
                "Camera access is required to take progress photos. Please grant permission in settings."
            } else {
                "This app needs camera access to capture your progress photos."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { permissionState.launchPermissionRequest() }
        ) {
            Text("Grant Permission")
        }
    }
}

private fun getInstructionText(photoGroup: PhotoGroup): String {
    return when (photoGroup) {
        PhotoGroup.FACE -> "Center your face in the frame"
        PhotoGroup.FRONT -> "Stand facing the camera directly"
        PhotoGroup.BACK -> "Turn around and face away from the camera"
        PhotoGroup.SIDE_LEFT -> "Turn to your left side"
        PhotoGroup.SIDE_RIGHT -> "Turn to your right side"
        PhotoGroup.CUSTOM -> "Position yourself as needed"
    }
}

private fun capturePhoto(
    controller: LifecycleCameraController,
    context: android.content.Context,
    onPhotoCaptured: (Bitmap) -> Unit
) {
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                onPhotoCaptured(bitmap)
                image.close()
            }
            
            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val planeProxy = image.planes[0]
    val buffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    
    // Rotate bitmap based on image rotation
    val matrix = Matrix()
    matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
    
    return Bitmap.createBitmap(
        bitmap,
        0,
        0,
        bitmap.width,
        bitmap.height,
        matrix,
        true
    )
}



