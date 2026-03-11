package com.longpc.nvrcloud

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.io.File
import java.util.concurrent.TimeUnit

class NvrService : Service() {

    companion object {
        const val ACTION_START = "com.longpc.nvrcloud.START"
        const val ACTION_STOP = "com.longpc.nvrcloud.STOP"
        private const val CHANNEL_ID = "nvr_channel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

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

        val folder = File(filesDir, "segments")
        if (!folder.exists()) folder.mkdirs()

        // TODO: attach CameraX recorder and segment files every 60s into /segments.
        scheduleUploader()
    }

    private fun stopNvr() {
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
