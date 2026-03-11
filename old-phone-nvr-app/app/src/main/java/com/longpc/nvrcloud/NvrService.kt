package com.longpc.nvrcloud

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NvrService : LifecycleService() {

    companion object {
        const val ACTION_START = "com.longpc.nvrcloud.START"
        const val ACTION_STOP = "com.longpc.nvrcloud.STOP"
        private const val CHANNEL_ID = "nvr_channel"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var loopJob: Job? = null
    private var currentRecording: Recording? = null
    private var videoCapture: VideoCapture<Recorder>? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startNvr()
            ACTION_STOP -> stopNvr()
        }
        return START_STICKY
    }

    private fun startNvr() {
        createNotificationChannel()
        startForeground(99, buildNotification("NVR is running"))

        prepareCameraAndStartLoop()
        scheduleUploader()
    }

    private fun prepareCameraAndStartLoop() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, videoCapture)

            startSegmentLoop()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startSegmentLoop() {
        if (loopJob?.isActive == true) return

        loopJob = serviceScope.launch {
            while (isActive) {
                rotateSegmentRecording()
                delay(60_000)
            }
        }
    }

    private fun rotateSegmentRecording() {
        currentRecording?.stop()
        currentRecording = null

        val capture = videoCapture ?: return
        val segmentFile = nextSegmentFile()
        val output = FileOutputOptions.Builder(segmentFile).build()

        var pending = capture.output.prepareRecording(this, output)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            pending = pending.withAudioEnabled()
        }

        currentRecording = pending.start(ContextCompat.getMainExecutor(this)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    // no-op
                }
                is VideoRecordEvent.Finalize -> {
                    // file finalized. uploader worker will pick it up.
                }
            }
        }
    }

    private fun nextSegmentFile(): File {
        val base = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
        val dir = File(base, "segments")
        if (!dir.exists()) dir.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "segment_$stamp.mp4")
    }

    private fun stopNvr() {
        loopJob?.cancel()
        currentRecording?.stop()
        currentRecording = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun scheduleUploader() {
        val req = PeriodicWorkRequestBuilder<UploadWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "upload_segments",
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    private fun buildNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Old Phone NVR Cloud")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NVR Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}