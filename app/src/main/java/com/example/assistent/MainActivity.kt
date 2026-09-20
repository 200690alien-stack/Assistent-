package com.example.assistent

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val microphone = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListener()
        else Toast.makeText(this, "Нет доступа к микрофону", Toast.LENGTH_SHORT).show()
    }

    private val screen = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                val service = Intent(this, ScreenCaptureService::class.java)
                service.putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                service.putExtra(ScreenCaptureService.EXTRA_DATA, data)
                ContextCompat.startForegroundService(this, service)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(24, 48, 24, 24)

        fun button(title: String, action: () -> Unit) {
            val button = Button(this)
            button.text = title
            button.setOnClickListener { action() }
            layout.addView(button)
        }

        button("Включить микрофон") {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startListener()
            } else {
                microphone.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        button("Выключить микрофон") {
            stopService(Intent(this, WakeWordService::class.java))
        }

        button("Включить захват экрана") {
            val manager = getSystemService(
                Context.MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager
            screen.launch(manager.createScreenCaptureIntent())
        }

        button("Выключить захват экрана") {
            stopService(Intent(this, ScreenCaptureService::class.java))
        }

        setContentView(layout)
    }

    private fun startListener() {
        ContextCompat.startForegroundService(
            this, Intent(this, WakeWordService::class.java)
        )
    }
}
