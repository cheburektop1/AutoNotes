package com.example.autonotes.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import com.example.autonotes.MainActivity
import com.example.autonotes.data.RecordingRepository
import com.example.autonotes.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject lateinit var repository: RecordingRepository

    // @field: нужен, чтобы Hilt увидел квалификатор на поле
    @Inject @field:ApplicationScope lateinit var appScope: CoroutineScope

    private var recorder: AudioRecorder? = null
    private var currentFile: File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_PAUSE -> setPaused(true)
            ACTION_RESUME -> setPaused(false)
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        if (recorder != null) return

        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        val dir = File(filesDir, "recordings").apply { mkdirs() }
        val file = File(dir, "rec_${System.currentTimeMillis()}.wav")
        val newRecorder = AudioRecorder()
        try {
            newRecorder.start(file)
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось начать запись", e)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        recorder = newRecorder
        currentFile = file
        RecordingSession.recorder = newRecorder
        RecordingSession.update { it.copy(isRecording = true, isPaused = false) }
    }

    private fun setPaused(paused: Boolean) {
        val rec = recorder ?: return
        if (paused) rec.pause() else rec.resume()
        RecordingSession.update { it.copy(isPaused = paused) }
    }

    private fun stopRecording() {
        finishAndSave()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Останавливает запись и сохраняет её в базу. Безопасно вызывать повторно. */
    private fun finishAndSave() {
        val rec = recorder ?: return
        val duration = rec.stop()
        val file = currentFile
        recorder = null
        currentFile = null
        RecordingSession.recorder = null
        RecordingSession.update { RecordingSession.State() }

        if (file == null) return
        // appScope переживает сервис, поэтому запись сохранится, даже если сервис уже уничтожен
        appScope.launch {
            if (duration < MIN_DURATION_MS) {
                file.delete() // случайное нажатие: слишком короткую запись не храним
            } else {
                repository.saveFinished(file.absolutePath, duration)
            }
        }
    }

    override fun onDestroy() {
        finishAndSave() // если сервис прервала система, запись всё равно сохранится
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Запись лекции", NotificationManager.IMPORTANCE_LOW)
        )
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Auto Notes")
            .setContentText("Идёт запись лекции")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "RecordingService"
        private const val CHANNEL_ID = "recording"
        private const val NOTIFICATION_ID = 1
        private const val MIN_DURATION_MS = 1_000L
        private const val ACTION_START = "autonotes.action.START"
        private const val ACTION_PAUSE = "autonotes.action.PAUSE"
        private const val ACTION_RESUME = "autonotes.action.RESUME"
        private const val ACTION_STOP = "autonotes.action.STOP"

        fun start(context: Context) { context.startForegroundService(intent(context, ACTION_START)) }
        fun pause(context: Context) { context.startService(intent(context, ACTION_PAUSE)) }
        fun resume(context: Context) { context.startService(intent(context, ACTION_RESUME)) }
        fun stop(context: Context) { context.startService(intent(context, ACTION_STOP)) }

        private fun intent(context: Context, action: String) =
            Intent(context, RecordingService::class.java).setAction(action)
    }
}