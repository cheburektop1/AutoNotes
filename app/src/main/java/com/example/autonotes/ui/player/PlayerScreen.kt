package com.example.autonotes.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.autonotes.ui.PlayPauseGlyph
import com.example.autonotes.ui.color
import com.example.autonotes.ui.formatDuration
import com.example.autonotes.ui.label
import com.example.autonotes.ui.shortLabel
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.autonotes.export.ExportFormat
import com.example.autonotes.ui.export.ExportViewModel
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.autonotes.util.BitmapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.autonotes.ui.RenameDialog

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
    exportViewModel: ExportViewModel = hiltViewModel()
) {
    val recording by viewModel.recording.collectAsStateWithLifecycle()
    val position by viewModel.positionMs.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val errorText by viewModel.error.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val exporting by exportViewModel.exporting.collectAsStateWithLifecycle()
    val exportError by exportViewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        exportViewModel.shareRequests.collect { request ->
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", request.file
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = request.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, request.subject)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Поделиться отчётом"))
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.pause() }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        val rec = recording
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            TextButton(onClick = onBack) { Text("Назад") }

            errorText?.let {
                Text(it, color = scheme.error, modifier = Modifier.padding(vertical = 8.dp))
            }

            if (rec == null) {
                if (errorText == null) Text("Загрузка…")
                return@Column
            }

            var showRename by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    rec.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showRename = true }) { Text("Изменить") }
            }
            if (showRename) {
                RenameDialog(
                    currentTitle = rec.title,
                    onConfirm = { viewModel.rename(it); showRename = false },
                    onDismiss = { showRename = false }
                )
            }
            Spacer(Modifier.height(16.dp))

            val duration = rec.durationMs.coerceAtLeast(1L)
            var dragging by remember { mutableStateOf(false) }
            var dragValue by remember { mutableFloatStateOf(0f) }
            val fraction = if (dragging) dragValue else (position.toFloat() / duration).coerceIn(0f, 1f)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Slider(
                        value = fraction,
                        onValueChange = { dragging = true; dragValue = it },
                        onValueChangeFinished = {
                            viewModel.seekTo((dragValue * duration).toLong())
                            dragging = false
                        },
                        modifier = Modifier.semantics { contentDescription = "Позиция записи" }
                    )

                    EventsStrip(events = events, durationMs = duration, onSeek = viewModel::seekTo)

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            formatDuration((fraction * duration).toLong()),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            formatDuration(rec.durationMs),
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.seekBy(-10_000) },
                            modifier = Modifier.semantics { contentDescription = "Назад на 10 секунд" }
                        ) { Text("−10 с") }

                        Spacer(Modifier.width(16.dp))
                        PlayButton(isPlaying = isPlaying, onClick = viewModel::togglePlay)
                        Spacer(Modifier.width(16.dp))

                        TextButton(
                            onClick = { viewModel.seekBy(10_000) },
                            modifier = Modifier.semantics { contentDescription = "Вперёд на 10 секунд" }
                        ) { Text("+10 с") }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { exportViewModel.export(ExportFormat.PDF) },
                    enabled = !exporting,
                    modifier = Modifier.weight(1f)
                ) { Text("Отчёт PDF") }
                OutlinedButton(
                    onClick = { exportViewModel.export(ExportFormat.MARKDOWN) },
                    enabled = !exporting,
                    modifier = Modifier.weight(1f)
                ) { Text("Markdown") }
            }
            if (exporting) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            exportError?.let {
                Text(it, color = scheme.error, modifier = Modifier.padding(top = 8.dp))
            }

            Text("Отметки (${events.size})", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (events.isEmpty()) {
                Text(
                    "В этой записи нет отметок.",
                    color = scheme.onSurfaceVariant
                )
            } else {
                val activeIndex = events.indexOfLast { it.timeMs <= position }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(events) { index, event ->
                        val active = index == activeIndex
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    onClickLabel = "Перейти к ${formatDuration(event.timeMs)}"
                                ) { viewModel.seekTo(event.timeMs) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (active) scheme.primaryContainer
                                else scheme.surfaceContainerLow
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EventBadge(event, 32.dp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(eventTitle(event), style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        formatDuration(event.timeMs),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (active) scheme.onPrimaryContainer
                                        else scheme.onSurfaceVariant
                                    )
                                    if (event is TimelineEvent.Photo) {
                                        Spacer(Modifier.height(8.dp))
                                        PhotoThumbnail(event.filePath)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(
                onClickLabel = if (isPlaying) "Пауза" else "Воспроизвести",
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        PlayPauseGlyph(isPlaying, MaterialTheme.colorScheme.onPrimary, Modifier.size(28.dp))
    }
}

private fun eventTitle(event: TimelineEvent): String = when (event) {
    is TimelineEvent.Marker -> event.type.label()
    is TimelineEvent.Note -> "Заметка: ${event.text}"
    is TimelineEvent.Photo -> "Фото доски"
}

private fun badge(event: TimelineEvent): Pair<String, Color> = when (event) {
    is TimelineEvent.Marker -> event.type.shortLabel() to event.type.color()
    is TimelineEvent.Note -> "З" to Color(0xFF616161)
    is TimelineEvent.Photo -> "Ф" to Color(0xFF7B1FA2)
}

@Composable
private fun EventBadge(event: TimelineEvent, size: Dp) {
    val (text, color) = badge(event)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 11.sp)
    }
}

/** Полоса с иконками под слайдером. Нажатие на иконку перематывает запись. */
@Composable
private fun EventsStrip(
    events: List<TimelineEvent>,
    durationMs: Long,
    onSeek: (Long) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 10.dp) // совпадает с отступом дорожки Slider под ползунок
    ) {
        val trackWidth = maxWidth
        events.forEach { event ->
            val fraction = (event.timeMs.toFloat() / durationMs).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .offset(x = trackWidth * fraction - 20.dp)
                    .size(40.dp) // область нажатия не меньше 40 dp
                    .clickable(onClickLabel = "Перейти к ${formatDuration(event.timeMs)}") {
                        onSeek(event.timeMs)
                    }
                    .semantics { contentDescription = "${eventTitle(event)}, ${formatDuration(event.timeMs)}" },
                contentAlignment = Alignment.Center
            ) {
                EventBadge(event, 24.dp)
            }
        }
    }
}
@Composable
private fun PhotoThumbnail(path: String) {
    val bitmap by produceState<Bitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) { BitmapLoader.load(path, 720, 480) }
    }
    bitmap?.let { bmp ->
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Фото доски",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    }
}