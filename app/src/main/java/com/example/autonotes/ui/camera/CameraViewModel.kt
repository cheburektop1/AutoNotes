package com.example.autonotes.ui.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.example.autonotes.recording.RecordingSession
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()

    private val _photoCount = MutableStateFlow(0)
    val photoCount: StateFlow<Int> = _photoCount.asStateFlow()

    private val _capturing = MutableStateFlow(false)
    val capturing: StateFlow<Boolean> = _capturing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val preview = Preview.Builder().build().apply {
        setSurfaceProvider { request -> _surfaceRequest.value = request }
    }

    private val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    /** Привязывает камеру к жизненному циклу, работает, пока корутина не отменена. */
    suspend fun bindToCamera(lifecycleOwner: LifecycleOwner) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture
        )
        try {
            awaitCancellation()
        } finally {
            provider.unbindAll()
        }
    }

    fun takePhoto() {
        if (_capturing.value) return
        if (!RecordingSession.state.value.isRecording) {
            _message.value = "Запись не идёт: фото не к чему привязать"
            return
        }

        val timeMs = RecordingSession.positionMs()
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()

        _message.value = null
        _capturing.value = true
        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (RecordingSession.state.value.isRecording) {
                        RecordingSession.addPhoto(file.absolutePath, timeMs)
                        _photoCount.update { it + 1 }
                    } else {
                        file.delete() // запись успела закончиться: фото осталось бы без записи
                        _message.value = "Запись уже остановлена"
                    }
                    _capturing.value = false
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Не удалось сделать фото", exception)
                    file.delete()
                    _message.value = "Не удалось сделать фото"
                    _capturing.value = false
                }
            }
        )
    }

    private companion object {
        const val TAG = "AutoNotesCamera"
    }
}