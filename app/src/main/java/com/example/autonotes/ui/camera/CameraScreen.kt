package com.example.autonotes.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.autonotes.recording.RecordingSession
import com.example.autonotes.ui.formatDuration
import kotlinx.coroutines.delay

@Composable
fun CameraScreen(
    onDone: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(hasPermission, lifecycleOwner) {
        if (hasPermission) viewModel.bindToCamera(lifecycleOwner)
    }

    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()
    val photoCount by viewModel.photoCount.collectAsStateWithLifecycle()
    val capturing by viewModel.capturing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val recState by RecordingSession.state.collectAsStateWithLifecycle()
    var positionMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(recState.isRecording) {
        while (recState.isRecording) {
            positionMs = RecordingSession.positionMs()
            delay(200)
        }
    }

    Scaffold(containerColor = Color.Black, modifier = Modifier.fillMaxSize()) { padding ->
        Box(Modifier.fillMaxSize()) {
            if (hasPermission) {
                surfaceRequest?.let { request ->
                    CameraXViewfinder(
                        surfaceRequest = request,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                PermissionPrompt(
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Верхняя плашка: идёт ли запись и сколько снимков сделано
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(padding)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (recState.isRecording)
                        "Запись ${formatDuration(positionMs)} · снимков: $photoCount"
                    else
                        "Запись не идёт",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                message?.let {
                    Text(
                        it,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                }
                ShutterButton(
                    enabled = hasPermission && recState.isRecording && !capturing,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.takePhoto()
                    }
                )
                Button(onClick = onDone, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Готово")
                }
            }
        }
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White.copy(alpha = alpha), CircleShape)
            .clickable(
                enabled = enabled,
                onClickLabel = "Сделать снимок",
                role = Role.Button,
                onClick = onClick
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color.White.copy(alpha = alpha))
        )
    }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Для съёмки доски нужен доступ к камере.",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = onRequest) { Text("Разрешить") }
    }
}