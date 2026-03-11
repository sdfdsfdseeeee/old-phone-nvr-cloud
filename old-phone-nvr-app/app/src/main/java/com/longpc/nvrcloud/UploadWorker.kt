package com.longpc.nvrcloud

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UploadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val accountEmail = applicationContext
                .getSharedPreferences("nvr_prefs", Context.MODE_PRIVATE)
                .getString("drive_account_email", null)
                ?: return@withContext Result.retry()

            val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext,
                listOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccountName = accountEmail

            val drive = Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("OldPhoneNvrCloud")
                .build()

            val base = applicationContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: applicationContext.filesDir
            val dir = File(base, "segments")
            if (!dir.exists()) return@withContext Result.success()

            val files = dir.listFiles { f -> f.extension.equals("mp4", true) }
                ?.sortedBy { it.lastModified() }
                ?: return@withContext Result.success()

            for (file in files) {
                val ok = uploadToGoogleDrive(drive, file)
                if (!ok) return@withContext Result.retry()
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("UploadWorker", "Upload failed", e)
            Result.retry()
        }
    }

    private fun uploadToGoogleDrive(drive: Drive, file: File): Boolean {
        return try {
            val metadata = com.google.api.services.drive.model.File().apply {
                name = file.name
                parents = listOf("root")
            }
            val media = FileContent("video/mp4", file)

            drive.files()
                .create(metadata, media)
                .setFields("id,name")
                .execute()

            file.delete()
            true
        } catch (e: Exception) {
            Log.e("UploadWorker", "Single file upload failed: ${file.name}", e)
            false
        }
    }
}
