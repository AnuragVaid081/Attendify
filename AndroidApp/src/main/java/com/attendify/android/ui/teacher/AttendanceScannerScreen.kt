package com.attendify.android.ui.teacher

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.attendify.android.ui.theme.*
import com.attendify.shared.viewmodel.AttendanceViewModel
import com.attendify.shared.viewmodel.AuthViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.koin.androidx.compose.koinViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AttendanceScannerScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: AttendanceViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val authState by authViewModel.state.collectAsState()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    // Reset processing flag after result
    LaunchedEffect(state.successMessage, state.error) {
        isProcessing = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Attendance", color = OnDarkBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OnDarkBackground)
                    }
                },
                actions = {
                    state.activeSession?.let { session ->
                        Text(
                            "${state.records.size}/${session.totalStudents}",
                            style = MaterialTheme.typography.labelLarge,
                            color = AttendifySecondary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!cameraPermission.status.isGranted) {
                // Permission request screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = AttendifyPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Camera permission required", style = MaterialTheme.typography.titleMedium, color = OnDarkBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                        Text("Grant Permission")
                    }
                }
            } else {
                // Camera preview
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onQrDetected = { qrValue ->
                        if (!isProcessing) {
                            isProcessing = true
                            authState.user?.id?.let { userId ->
                                viewModel.markStudentAttendance(sessionId, qrValue, userId)
                            }
                        }
                    }
                )

                // QR scanning overlay
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Darkened corners
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .border(2.dp, AttendifyPrimary, MaterialTheme.shapes.large)
                                .clip(MaterialTheme.shapes.large)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Point at student QR code",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // Status snackbar area
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedVisibility(
                        visible = state.successMessage != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = ColorPresent.copy(alpha = 0.9f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                                Text(state.successMessage ?: "", color = Color.White)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = state.error != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = ColorAbsent.copy(alpha = 0.9f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = Color.White)
                                Text(state.error ?: "", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    @androidx.camera.core.ExperimentalGetImage
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage, imageProxy.imageInfo.rotationDegrees
                        )
                        barcodeScanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                                    ?.rawValue?.let { value -> onQrDetected(value) }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, imageAnalysis
                    )
                } catch (e: Exception) { /* Log */ }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}
