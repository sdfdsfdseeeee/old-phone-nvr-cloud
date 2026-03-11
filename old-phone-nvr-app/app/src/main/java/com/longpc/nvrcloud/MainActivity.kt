package com.longpc.nvrcloud

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val startBtn: Button = findViewById(R.id.startBtn)
        val stopBtn: Button = findViewById(R.id.stopBtn)

        requestRuntimePermissions()

        startBtn.setOnClickListener {
            val i = Intent(this, NvrService::class.java).apply { action = NvrService.ACTION_START }
            ContextCompat.startForegroundService(this, i)
            statusText.text = "NVR running"
        }

        stopBtn.setOnClickListener {
            val i = Intent(this, NvrService::class.java).apply { action = NvrService.ACTION_STOP }
            startService(i)
            statusText.text = "NVR stopped"
        }
    }

    private fun requestRuntimePermissions() {
        val perms = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS

        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1001)
        }
    }
}
