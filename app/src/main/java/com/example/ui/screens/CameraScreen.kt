package com.example.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.util.Rational
import android.view.ViewGroup
import android.view.Surface as AndroidSurface
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Observer
import com.example.data.MediaType
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class CameraMode {
    PHOTO,
    VIDEO
}

enum class FlashState {
    OFF,
    ON,
    AUTO
}

private enum class PhotoAspectRatio(
    val label: String,
    val strategy: AspectRatioStrategy,
    val cropRatio: Rational
) {
    STANDARD("4:3", AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY, Rational(4, 3)),
    WIDE("16:9", AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY, Rational(16, 9))
}

private enum class VideoResolution(val label: String, val quality: Quality?) {
    AUTO("Auto", null),
    UHD("4K", Quality.UHD),
    FHD("1080p", Quality.FHD),
    HD("720p", Quality.HD),
    SD("480p", Quality.SD)
}

@Composable
fun CameraScreen(
    onClose: () -> Unit,
    onMediaCaptured: (Uri, MediaType, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Handle back button to exit camera mode
    BackHandler {
        onClose()
    }

    // Permission tracking
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true || hasCameraPermission
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] == true || hasAudioPermission
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionPrompt(
            onRequestPermission = {
                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            },
            onClose = onClose,
            modifier = modifier
        )
        return
    }

    var cameraMode by remember { mutableStateOf(CameraMode.PHOTO) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashState by remember { mutableStateOf(FlashState.OFF) }
    var photoAspectRatio by remember { mutableStateOf(PhotoAspectRatio.STANDARD) }
    var videoResolution by remember { mutableStateOf(VideoResolution.AUTO) }
    var selectedVideoFpsRange by remember { mutableStateOf<Range<Int>?>(null) }
    var supportedVideoResolutions by remember { mutableStateOf(listOf(VideoResolution.AUTO)) }
    var supportedVideoFpsRanges by remember { mutableStateOf(emptyList<Range<Int>>()) }

    // Video Recording state
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var recordingStartTime by remember { mutableStateOf(0L) }

    // Shutter animation
    val shutterFlashAlpha = remember { Animatable(0f) }

    // CameraX references
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var hasBackCamera by remember { mutableStateOf(false) }
    var hasFrontCamera by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var linearZoom by remember { mutableFloatStateOf(0f) }
    var minZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(1f) }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    val imageCapture = remember(photoAspectRatio) {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(100)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(photoAspectRatio.strategy)
                    .build()
            )
            .build()
    }
    val videoCapture = remember(videoResolution) {
        val requestedQuality = videoResolution.quality ?: Quality.HIGHEST
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(requestedQuality))
            .build()
        VideoCapture.Builder(recorder).build()
    }

    // Query camera provider and check capabilities once
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                val backAvailable = try {
                    provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                } catch (e: Exception) {
                    false
                }
                val frontAvailable = try {
                    provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                } catch (e: Exception) {
                    false
                }
                hasBackCamera = backAvailable
                hasFrontCamera = frontAvailable

                if (!backAvailable && frontAvailable) {
                    lensFacing = CameraSelector.LENS_FACING_FRONT
                } else if (backAvailable) {
                    lensFacing = CameraSelector.LENS_FACING_BACK
                }
            } catch (e: Exception) {
                Log.w("CameraScreen", "Could not obtain CameraProvider: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Bind camera safely when provider, lens facing, mode, or lifecycle changes
    LaunchedEffect(
        cameraProvider,
        lensFacing,
        cameraMode,
        lifecycleOwner,
        imageCapture,
        videoCapture,
        selectedVideoFpsRange
    ) {
        val provider = cameraProvider ?: return@LaunchedEffect
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val isSupported = try {
            provider.hasCamera(cameraSelector)
        } catch (e: Exception) {
            false
        }

        if (!isSupported) {
            Log.w("CameraScreen", "Selected lens facing $lensFacing is not available on this device")
            return@LaunchedEffect
        }

        try {
            provider.unbindAll()
            val targetRotation = previewView.display?.rotation ?: AndroidSurface.ROTATION_0
            imageCapture.targetRotation = targetRotation
            val previewAspectRatio = if (cameraMode == CameraMode.VIDEO) {
                AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
            } else {
                photoAspectRatio.strategy
            }
            val preview = Preview.Builder()
                .setTargetRotation(targetRotation)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(previewAspectRatio)
                        .build()
                )
                .build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            boundCamera = if (cameraMode == CameraMode.PHOTO) {
                val photoGroup = UseCaseGroup.Builder()
                    .setViewPort(
                        ViewPort.Builder(photoAspectRatio.cropRatio, targetRotation)
                            .setScaleType(ViewPort.FILL_CENTER)
                            .build()
                    )
                    .addUseCase(preview)
                    .addUseCase(imageCapture)
                    .build()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    photoGroup
                )
            } else {
                val cameraInfo = provider.getCameraInfo(cameraSelector)
                val supportedQualities = Recorder.getVideoCapabilities(cameraInfo)
                    .getSupportedQualities(DynamicRange.SDR)
                supportedVideoResolutions = listOf(VideoResolution.AUTO) +
                    VideoResolution.entries.drop(1).filter { it.quality in supportedQualities }
                if (videoResolution !in supportedVideoResolutions) {
                    videoResolution = VideoResolution.AUTO
                    return@LaunchedEffect
                }

                val videoViewPort = ViewPort.Builder(Rational(16, 9), targetRotation)
                    .setScaleType(ViewPort.FILL_CENTER)
                    .build()
                val baseSession = SessionConfig.Builder(preview, videoCapture)
                    .setViewPort(videoViewPort)
                    .build()
                val compatibleFpsRanges = cameraInfo.getSupportedFrameRateRanges(baseSession)
                    .filter { it.upper in 24..240 }
                    .groupBy { it.upper }
                    .map { (_, ranges) -> ranges.minBy { it.upper - it.lower } }
                    .sortedBy { it.upper }
                supportedVideoFpsRanges = compatibleFpsRanges

                if (selectedVideoFpsRange != null && selectedVideoFpsRange !in compatibleFpsRanges) {
                    selectedVideoFpsRange = null
                    Toast.makeText(context, "That FPS is not available at this resolution. Using Auto.", Toast.LENGTH_SHORT).show()
                    return@LaunchedEffect
                }

                val session = SessionConfig.Builder(preview, videoCapture).apply {
                    setViewPort(videoViewPort)
                    selectedVideoFpsRange?.let(::setFrameRateRange)
                }.build()
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, session)
            }
            boundCamera?.cameraControl?.setZoomRatio(1f)
        } catch (exc: Exception) {
            Log.w("CameraScreen", "Camera binding failed: ${exc.message}")
            if (
                cameraMode == CameraMode.VIDEO &&
                (videoResolution != VideoResolution.AUTO || selectedVideoFpsRange != null)
            ) {
                Toast.makeText(
                    context,
                    "That resolution and FPS combination is unavailable. Using Auto.",
                    Toast.LENGTH_SHORT
                ).show()
                videoResolution = VideoResolution.AUTO
                selectedVideoFpsRange = null
            }
        }
    }

    // Keep the controls synchronized with the physical camera's supported zoom range.
    DisposableEffect(boundCamera, lifecycleOwner) {
        val zoomState = boundCamera?.cameraInfo?.zoomState
        val observer = Observer<ZoomState> { state ->
            zoomRatio = state.zoomRatio
            linearZoom = state.linearZoom
            minZoomRatio = state.minZoomRatio
            maxZoomRatio = state.maxZoomRatio
        }
        zoomState?.observe(lifecycleOwner, observer)
        onDispose { zoomState?.removeObserver(observer) }
    }

    // Timer for video recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isActive) {
                delay(1000)
                recordingSeconds++
            }
        } else {
            recordingSeconds = 0
        }
    }

    // Clean up active recording & camera bindings when leaving
    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                activeRecording?.stop()
                activeRecording = null
                cameraProvider?.unbindAll()
            } catch (e: Exception) {
                Log.w("CameraScreen", "Error on dispose: ${e.message}")
            }
        }
    }

    // Update ImageCapture flash mode
    LaunchedEffect(flashState) {
        imageCapture.flashMode = when (flashState) {
            FlashState.OFF -> ImageCapture.FLASH_MODE_OFF
            FlashState.ON -> ImageCapture.FLASH_MODE_ON
            FlashState.AUTO -> ImageCapture.FLASH_MODE_AUTO
        }
        try {
            boundCamera?.cameraControl?.enableTorch(flashState == FlashState.ON && cameraMode == CameraMode.VIDEO)
        } catch (e: Exception) {
            Log.w("CameraScreen", "Could not toggle torch", e)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_screen")
    ) {
        // Camera Preview View
        AndroidView(
            factory = { previewView },
            update = { view ->
                // A PreviewView can be composed before Android attaches it to a
                // display. Some devices return null here during that short window.
                view.display?.rotation?.let { rotation ->
                    imageCapture.targetRotation = rotation
                    videoCapture.targetRotation = rotation
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(boundCamera, minZoomRatio, maxZoomRatio) {
                    awaitEachGesture {
                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.size >= 2) {
                                val currentZoom = boundCamera?.cameraInfo?.zoomState?.value?.zoomRatio
                                    ?: zoomRatio
                                val requestedZoom = (currentZoom * event.calculateZoom())
                                    .coerceIn(minZoomRatio, maxZoomRatio)
                                boundCamera?.cameraControl?.setZoomRatio(requestedZoom)
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(boundCamera) {
                    detectTapGestures { offset ->
                        val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(point)
                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                            .build()
                        boundCamera?.cameraControl?.startFocusAndMetering(action)
                    }
                }
        )

        // Shutter flash effect
        if (shutterFlashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = shutterFlashAlpha.value))
            )
        }

        // Top Controls Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close Button
                IconButton(
                    onClick = {
                        if (isRecording) {
                            activeRecording?.stop()
                            activeRecording = null
                        }
                        onClose()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .testTag("camera_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Camera",
                        tint = Color.White
                    )
                }

                // Center Recording Timer Indicator (when recording video)
                if (isRecording) {
                    Row(
                        modifier = Modifier
                            .background(Color.Red.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val minutes = recordingSeconds / 60
                        val seconds = recordingSeconds % 60
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Flash Toggle Button
                IconButton(
                    onClick = {
                        flashState = when (flashState) {
                            FlashState.OFF -> FlashState.ON
                            FlashState.ON -> FlashState.AUTO
                            FlashState.AUTO -> FlashState.OFF
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .testTag("camera_flash_button")
                ) {
                    Icon(
                        imageVector = when (flashState) {
                            FlashState.OFF -> Icons.Default.FlashOff
                            FlashState.ON -> Icons.Default.FlashOn
                            FlashState.AUTO -> Icons.Default.FlashAuto
                        },
                        contentDescription = "Flash: ${flashState.name}",
                        tint = if (flashState != FlashState.OFF) Color(0xFFFFD54F) else Color.White
                    )
                }
            }

            if (!isRecording) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (cameraMode == CameraMode.PHOTO) {
                        CameraOptionMenu(
                            label = "ASPECT",
                            selectedLabel = photoAspectRatio.label,
                            options = PhotoAspectRatio.entries.map { it.label to it },
                            onSelect = { photoAspectRatio = it }
                        )
                    } else {
                        CameraOptionMenu(
                            label = "RESOLUTION",
                            selectedLabel = videoResolution.label,
                            options = supportedVideoResolutions.map { it.label to it },
                            onSelect = { videoResolution = it }
                        )
                        CameraOptionMenu(
                            label = "FPS",
                            selectedLabel = selectedVideoFpsRange?.fpsLabel() ?: "Auto",
                            options = listOf("Auto" to null) + supportedVideoFpsRanges.map { it.fpsLabel() to it },
                            onSelect = { selectedVideoFpsRange = it }
                        )
                    }
                }
            }
        }

        // Bottom Controls Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (!isRecording && maxZoomRatio > minZoomRatio) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1fx", zoomRatio),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.width(44.dp)
                    )
                    Slider(
                        value = linearZoom,
                        onValueChange = { value ->
                            boundCamera?.cameraControl?.setLinearZoom(value.coerceIn(0f, 1f))
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("camera_zoom_slider")
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1fx", maxZoomRatio),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(44.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Mode Selector (PHOTO / VIDEO)
            if (!isRecording) {
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo Mode Tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (cameraMode == CameraMode.PHOTO) Color.White.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                cameraMode = CameraMode.PHOTO
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("camera_mode_photo")
                    ) {
                        Text(
                            text = "PHOTO",
                            color = if (cameraMode == CameraMode.PHOTO) Color(0xFFFFE082) else Color.White.copy(alpha = 0.7f),
                            fontWeight = if (cameraMode == CameraMode.PHOTO) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Video Mode Tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (cameraMode == CameraMode.VIDEO) Color.White.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                cameraMode = CameraMode.VIDEO
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("camera_mode_video")
                    ) {
                        Text(
                            text = "VIDEO",
                            color = if (cameraMode == CameraMode.VIDEO) Color(0xFFFFE082) else Color.White.copy(alpha = 0.7f),
                            fontWeight = if (cameraMode == CameraMode.VIDEO) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Spacer(modifier = Modifier.height(30.dp))
            }

            // Bottom Shutter & Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Spacer to center the shutter
                Spacer(modifier = Modifier.size(52.dp))

                // Center Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(
                            width = 4.dp,
                            color = if (cameraMode == CameraMode.VIDEO && isRecording) Color.Red else Color.White,
                            shape = CircleShape
                        )
                        .padding(6.dp)
                        .testTag("camera_shutter_button")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (cameraMode == CameraMode.PHOTO) {
                                // Take Photo
                                takePhoto(
                                    context = context,
                                    imageCapture = imageCapture,
                                    onCaptured = { uri ->
                                        scope.launch {
                                            shutterFlashAlpha.snapTo(0.85f)
                                            shutterFlashAlpha.animateTo(0f, animationSpec = tween(250))
                                        }
                                        scope.launch {
                                            val actualDetails = withContext(Dispatchers.IO) {
                                                readPhotoDetails(context, uri)
                                            } ?: "${photoAspectRatio.label} photo"
                                            Toast.makeText(context, "Saved $actualDetails", Toast.LENGTH_SHORT).show()
                                            onMediaCaptured(uri, MediaType.PHOTO, 0, actualDetails)
                                        }
                                    }
                                )
                            } else {
                                // Toggle Video Recording
                                if (isRecording) {
                                    activeRecording?.stop()
                                    activeRecording = null
                                } else {
                                    val recording = startVideoRecording(
                                        context = context,
                                        videoCapture = videoCapture,
                                        hasAudioPermission = hasAudioPermission,
                                        onRecordingStarted = {
                                            isRecording = true
                                            recordingStartTime = System.currentTimeMillis()
                                        },
                                        onRecordingFinalized = { uri, durationSec ->
                                            isRecording = false
                                            activeRecording = null
                                            scope.launch {
                                                val actualDetails = withContext(Dispatchers.IO) {
                                                    readVideoDetails(context, uri)
                                                } ?: buildString {
                                                    append(videoResolution.label)
                                                    selectedVideoFpsRange?.let { append(" • ${it.fpsLabel()} fps") }
                                                }
                                                Toast.makeText(context, "Saved $actualDetails", Toast.LENGTH_LONG).show()
                                                onMediaCaptured(uri, MediaType.VIDEO, durationSec, actualDetails)
                                            }
                                        }
                                    )
                                    activeRecording = recording
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        cameraMode == CameraMode.PHOTO -> {
                            // Inner solid white circle
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                        cameraMode == CameraMode.VIDEO && isRecording -> {
                            // Red square inside circle when recording
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Red)
                            )
                        }
                        else -> {
                            // Red circle for video idle
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935))
                            )
                        }
                    }
                }

                // Right: Flip Camera Button
                IconButton(
                    onClick = {
                        if (!isRecording) {
                            val nextFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                            val nextSelector = CameraSelector.Builder().requireLensFacing(nextFacing).build()
                            val canSwitch = try {
                                cameraProvider?.hasCamera(nextSelector) == true
                            } catch (e: Exception) {
                                false
                            }

                            if (canSwitch) {
                                lensFacing = nextFacing
                            } else {
                                Toast.makeText(
                                    context,
                                    if (nextFacing == CameraSelector.LENS_FACING_FRONT)
                                        "Front camera is not available on this device"
                                    else
                                        "Rear camera is not available on this device",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = !isRecording,
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .testTag("camera_flip_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = if (!isRecording) Color.White else Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> CameraOptionMenu(
    label: String,
    selectedLabel: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            color = Color.White.copy(alpha = 0.16f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { expanded = true }
                .testTag("camera_${label.lowercase(Locale.ROOT)}_menu")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(label, color = Color.White.copy(alpha = 0.65f), fontSize = 9.sp)
                Text(selectedLabel, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (optionLabel, option) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

private fun Range<Int>.fpsLabel(): String {
    return if (lower == upper) "$upper" else "$lower–$upper"
}

private fun readPhotoDetails(context: Context, uri: Uri): String? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    } ?: return null
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    val longSide = max(options.outWidth, options.outHeight)
    val shortSide = min(options.outWidth, options.outHeight)
    return "$longSide×$shortSide"
}

private fun readVideoDetails(context: Context, uri: Uri): String? {
    return MediaMetadataRetriever().run {
        try {
            setDataSource(context, uri)
            val width = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val height = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            if (width == null || height == null) return@run null
            val dimensions = "${max(width, height)}×${min(width, height)}"
            val fps = extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                ?.toFloatOrNull()
                ?.takeIf { it > 0f }
                ?.roundToInt()
            if (fps != null) "$dimensions • $fps fps" else dimensions
        } finally {
            release()
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onCaptured: (Uri) -> Unit
) {
    val displayName = "IMG_${System.currentTimeMillis()}.jpg"
    val contentValues = createPublicMediaValues(
        displayName = displayName,
        mimeType = "image/jpeg",
        mediaType = MediaType.PHOTO
    )
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.let(onCaptured)
                    ?: Toast.makeText(context, "Photo saved, but its MediaStore URI was unavailable", Toast.LENGTH_SHORT).show()
            }

            override fun onError(exception: ImageCaptureException) {
                Log.w("CameraScreen", "Photo capture failed: ${exception.message}", exception)
                Toast.makeText(context, "Failed to capture photo", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

private fun startVideoRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    hasAudioPermission: Boolean,
    onRecordingStarted: () -> Unit,
    onRecordingFinalized: (Uri, Int) -> Unit
): Recording {
    val displayName = "VID_${System.currentTimeMillis()}.mp4"
    val contentValues = createPublicMediaValues(
        displayName = displayName,
        mimeType = "video/mp4",
        mediaType = MediaType.VIDEO
    )
    val outputOptions = MediaStoreOutputOptions.Builder(
        context.contentResolver,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ).setContentValues(contentValues).build()

    val pendingRecording = videoCapture.output.prepareRecording(context, outputOptions)

    if (hasAudioPermission && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
        pendingRecording.withAudioEnabled()
    }

    val startTime = System.currentTimeMillis()

    return pendingRecording.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
        when (recordEvent) {
            is VideoRecordEvent.Start -> {
                onRecordingStarted()
            }
            is VideoRecordEvent.Finalize -> {
                if (!recordEvent.hasError()) {
                    val durationSec = ((System.currentTimeMillis() - startTime) / 1000).toInt().coerceAtLeast(1)
                    val savedUri = recordEvent.outputResults.outputUri
                    if (savedUri != Uri.EMPTY) {
                        onRecordingFinalized(savedUri, durationSec)
                    } else {
                        Toast.makeText(context, "Video saved, but its MediaStore URI was unavailable", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w("CameraScreen", "Video recording error code: ${recordEvent.error}")
                    Toast.makeText(context, "Video recording ended with error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun createPublicMediaValues(
    displayName: String,
    mimeType: String,
    mediaType: MediaType
): ContentValues {
    return ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/Gallery")
        } else {
            val publicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "Gallery"
            ).apply { mkdirs() }
            put(MediaStore.MediaColumns.DATA, File(publicDir, displayName).absolutePath)
        }
        if (mediaType == MediaType.PHOTO) {
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }
    }
}

@Composable
private fun CameraPermissionPrompt(
    onRequestPermission: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("camera_permission_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Camera Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Allow access to your camera and microphone to capture photos and record videos directly into your gallery.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("camera_permission_grant_button")
                ) {
                    Text("Grant Permissions")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("camera_permission_cancel_button")
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
