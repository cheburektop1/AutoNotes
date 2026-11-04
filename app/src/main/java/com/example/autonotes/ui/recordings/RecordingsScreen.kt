package com.example.autonotes.ui.recordings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.autonotes.data.db.RecordingEntity
import com.example.autonotes.recording.RecordingSession
import com.example.autonotes.ui.formatDuration
import kotlinx.coroutines.delay

@Composable
fun RecordingsScreen(
    onOpenRecording: (Long) -> Unit,
    viewModel: RecordingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val recState by viewModel.recordingState.collectAsStateWithLifecycle()
    var positionMs by remember { mutableLongStateOf(0L) }
    var toDelete by remember { mutableStateOf<RecordingEntity?>(null) }

    val permissions = remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.RECORD_AUDIO] == true) viewModel.startRecording()
    }

    LaunchedEffect(recState.isRecording) {
        while (recState.isRecording) {
            positionMs = RecordingSession.positionMs()
            delay(200)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Auto Notes",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            RecordPanel(
                isRecording = recState.isRecording,
                isPaused = recState.isPaused,
                positionMs = if (recState.isRecording) positionMs else 0L,
                onStart = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) viewModel.startRecording() else permissionLauncher.launch(permissions)
                },
                onTogglePause = viewModel::togglePause,
                onStop = viewModel::stopRecording
            )

            Spacer(Modifier.height(16.dp))

            if (recordings.isEmpty()) {
                Text("Записей пока нет. Нажмите «Начать запись».")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recordings, key = { it.id }) { rec ->
                        RecordingItem(
                            recording = rec,
                            onClick = { onOpenRecording(rec.id) },
                            onDelete = { toDelete = rec }
                        )
                    }
                }
            }
        }
    }

    toDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Удалить запись?") },
            text = { Text("«${rec.title}» и все её данные будут удалены без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(rec); toDelete = null }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun RecordPanel(
    isRecording: Boolean,
    isPaused: Boolean,
    positionMs: Long,
    onStart: () -> Unit,
    onTogglePause: () -> Unit,
    onStop: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(formatDuration(positionMs), style = MaterialTheme.typography.displayMedium)
            if (!isRecording) {
                Button(onClick = onStart) { Text("Начать запись") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onTogglePause) { Text(if (isPaused) "Продолжить" else "Пауза") }
                    Button(onClick = onStop) { Text("Стоп") }
                }
            }
        }
    }
}

@Composable
private fun RecordingItem(
    recording: RecordingEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(recording.title, style = MaterialTheme.typography.titleMedium)
                Text(formatDuration(recording.durationMs), style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onDelete) { Text("Удалить") }
        }
    }
}