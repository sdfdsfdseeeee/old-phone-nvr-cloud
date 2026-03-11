package com.longpc.nvrcloud

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class MainActivity : ComponentActivity() {

    private lateinit var statusText: TextView
    private lateinit var accountText: TextView

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val email = account.email ?: "unknown"
            getSharedPreferences("nvr_prefs", MODE_PRIVATE)
                .edit()
                .putString("drive_account_email", email)
                .apply()
            accountText.text = "Drive account: $email"
            Toast.makeText(this, "Drive connected", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        accountText = findViewById(R.id.accountText)
        val signInBtn: Button = findViewById(R.id.signInBtn)
        val startBtn: Button = findViewById(R.id.startBtn)
        val stopBtn: Button = findViewById(R.id.stopBtn)

        val savedEmail = getSharedPreferences("nvr_prefs", MODE_PRIVATE)
            .getString("drive_account_email", null)
        if (savedEmail != null) accountText.text = "Drive account: $savedEmail"

        requestRuntimePermissions()

        signInBtn.setOnClickListener { signInGoogleDrive() }

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

    private fun signInGoogleDrive() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        signInLauncher.launch(client.signInIntent)
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
