package com.example.autonotes.ui.recordings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.autonotes.data.db.MarkerType
import com.example.autonotes.data.db.RecordingEntity
import com.example.autonotes.recording.RecordingSession
import com.example.autonotes.recording.RecordingSession.PendingEvent
import com.example.autonotes.ui.RecordGlyph
import com.example.autonotes.ui.StopGlyph
import com.example.autonotes.ui.color
import com.example.autonotes.ui.formatDuration
import com.example.autonotes.ui.label
import kotlinx.coroutines.delay
import com.example.autonotes.ui.RenameDialog

@Composable
fun RecordingsScreen(
    onOpenRecording: (Long) -> Unit,
    onOpenCamera: () -> Unit,
    onOpenAccount: () -> Unit,
    viewModel: RecordingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val recState by viewModel.recordingState.collectAsStateWithLifecycle()

    var positionMs by remember { mutableLongStateOf(0L) }
    var level by remember { mutableFloatStateOf(0f) }
    var toDelete by remember { mutableStateOf<RecordingEntity?>(null) }
    var toRename by remember { mutableStateOf<RecordingEntity?>(null) }
    var noteText by rememberSaveable { mutableStateOf("") }
    var noteStartMs by rememberSaveable { mutableStateOf<Long?>(null) }

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
        level = 0f
        if (!recState.isRecording) {
            noteText = ""
            noteStartMs = null
        }
        while (recState.isRecording) {
            positionMs = RecordingSession.positionMs()
            level = RecordingSession.level()
            delay(100)
        }
    }

    fun submitNote() {
        val text = noteText.trim()
        if (text.isNotEmpty()) {
            // Время заметки: момент, когда начали печатать
            viewModel.addNote(text, noteStartMs ?: RecordingSession.positionMs())
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        noteText = ""
        noteStartMs = null
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Auto Notes",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Запись лекций с отметками",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onOpenAccount) { Text("Аккаунт") }
                }
            }

            item {
                if (!recState.isRecording) {
                    IdleCard(onStart = {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) viewModel.startRecording() else permissionLauncher.launch(permissions)
                    })
                } else {
                    ActiveCard(
                        isPaused = recState.isPaused,
                        positionMs = positionMs,
                        level = level,
                        events = recState.events,
                        noteText = noteText,
                        onNoteChange = { new ->
                            if (noteText.isEmpty() && new.isNotEmpty()) {
                                noteStartMs = RecordingSession.positionMs()
                            }
                            noteText = new
                        },
                        onSendNote = ::submitNote,
                        onMarker = { type ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.addMarker(type)
                        },
                        onPhoto = onOpenCamera,
                        onTogglePause = viewModel::togglePause,
                        onStop = {
                            submitNote() // недописанная заметка не пропадёт
                            viewModel.stopRecording()
                        }
                    )
                }
            }

            item {
                Text(
                    "Записи",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (recordings.isEmpty()) {
                item {
                    Text(
                        "Записей пока нет. Первая появится здесь после остановки записи.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recordings, key = { it.id }) { rec ->
                    RecordingItem(
                        recording = rec,
                        onClick = { onOpenRecording(rec.id) },
                        onDelete = { toDelete = rec },
                        onRename = { toRename = rec }
                    )
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
    toRename?.let { rec ->
        RenameDialog(
            currentTitle = rec.title,
            onConfirm = { viewModel.rename(rec, it); toRename = null },
            onDismiss = { toRename = null }
        )
    }
}

@Composable
private fun IdleCard(onStart: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Новая запись", style = MaterialTheme.typography.titleMedium)
            Text(
                "Метки, фото и заметки привяжутся к секунде записи, а потом вы найдёте нужный момент за пару нажатий.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                RecordGlyph(scheme.onPrimary)
                Spacer(Modifier.width(10.dp))
                Text("Начать запись")
            }
        }
    }
}

@Composable
private fun ActiveCard(
    isPaused: Boolean,
    positionMs: Long,
    level: Float,
    events: List<PendingEvent>,
    noteText: String,
    onNoteChange: (String) -> Unit,
    onSendNote: () -> Unit,
    onMarker: (MarkerType) -> Unit,
    onPhoto: () -> Unit,
    onTogglePause: () -> Unit,
    onStop: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isPaused) scheme.outline else scheme.error)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isPaused) "Пауза" else "Идёт запись",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Отметок: ${events.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant
                )
            }

            Text(
                formatDuration(positionMs),
                style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            LinearProgressIndicator(
                progress = { if (isPaused) 0f else level },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .semantics { contentDescription = "Уровень сигнала микрофона" },
                trackColor = scheme.surfaceVariant
            )

            Text(
                "Отметить момент",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MarkerButton(MarkerType.IMPORTANT, Modifier.weight(1f)) { onMarker(MarkerType.IMPORTANT) }
                MarkerButton(MarkerType.DEFINITION, Modifier.weight(1f)) { onMarker(MarkerType.DEFINITION) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MarkerButton(MarkerType.EXAMPLE, Modifier.weight(1f)) { onMarker(MarkerType.EXAMPLE) }
                MarkerButton(MarkerType.HOMEWORK, Modifier.weight(1f)) { onMarker(MarkerType.HOMEWORK) }
            }

            // Кнопка съёмки доски: открывает экран камеры, запись при этом продолжается
            FilledTonalButton(
                onClick = onPhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7B1FA2))
                )
                Spacer(Modifier.width(8.dp))
                Text("Фото доски")
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Заметка") },
                supportingText = { Text("Время заметки — момент, когда вы начали печатать") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendNote() }),
                trailingIcon = {
                    TextButton(onClick = onSendNote, enabled = noteText.isNotBlank()) {
                        Text("Добавить")
                    }
                }
            )

            if (events.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    events.takeLast(3).asReversed().forEach { event ->
                        Text(
                            "${formatDuration(event.timeMs)} · ${pendingTitle(event)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                ) { Text(if (isPaused) "Продолжить" else "Пауза") }

                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.error,
                        contentColor = scheme.onError
                    )
                ) {
                    StopGlyph(scheme.onError)
                    Spacer(Modifier.width(8.dp))
                    Text("Стоп")
                }
            }
        }
    }
}

@Composable
private fun MarkerButton(type: MarkerType, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(type.color())
        )
        Spacer(Modifier.width(8.dp))
        Text(type.label(), textAlign = TextAlign.Center)
    }
}

private fun pendingTitle(event: PendingEvent): String = when (event) {
    is PendingEvent.Marker -> event.type.label()
    is PendingEvent.Note -> "Заметка: ${event.text}"
    is PendingEvent.Photo -> "Фото доски"
}

@Composable
private fun RecordingItem(
    recording: RecordingEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Открыть запись", onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp)) {
            Text(recording.title, style = MaterialTheme.typography.titleMedium)
            Text(
                formatDuration(recording.durationMs),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            Row {
                TextButton(onClick = onRename) { Text("Изменить") }
                TextButton(onClick = onDelete) { Text("Удалить", color = scheme.error) }
            }
        }
    }
}
