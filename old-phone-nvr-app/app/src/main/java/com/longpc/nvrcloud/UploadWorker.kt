package com.longpc.nvrcloud

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File

class UploadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dir = File(applicationContext.filesDir, "segments")
        if (!dir.exists()) return Result.success()

        val segmentFiles = dir.listFiles { f -> f.extension.equals("mp4", true) }?.sortedBy { it.lastModified() }
            ?: return Result.success()

        for (file in segmentFiles) {
            val ok = uploadToGoogleDrive(file)
            if (!ok) return Result.retry()
        }
        return Result.success()
    }

    private fun uploadToGoogleDrive(file: File): Boolean {
        // TODO: Implement Google Drive upload via Drive REST API OAuth token.
        // Quick MVP strategy:
        // 1) Store user token in encrypted prefs.
        // 2) POST multipart upload to Drive v3 files endpoint.
        // 3) On success, delete local file.
        return false
    }
}
