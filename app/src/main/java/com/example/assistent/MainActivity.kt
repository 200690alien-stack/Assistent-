package com.example.assistent

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val requestMicrophone =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startListener()
                requestScreenCapture()
            }
        }

    private val screenCaptureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                val data = result.data ?: return@registerForActivityResult

                val serviceIntent =
                    Intent(this, ScreenCaptureService::class.java).apply {
                        putExtra(
                            ScreenCaptureService.EXTRA_RESULT_CODE,
                            result.resultCode
                        )
                        putExtra(
                            ScreenCaptureService.EXTRA_DATA,
                            data
                        )
                    }

                ContextCompat.startForegroundService(
                    this,
                    serviceIntent
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startListener()
            requestScreenCapture()
        } else {
            requestMicrophone.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    private fun startListener() {
        val intent =
            Intent(this, WakeWordService::class.java)

        ContextCompat.startForegroundService(
            this,
            intent
        )
    }

    private fun requestScreenCapture() {

        val projectionManager =
            getSystemService(
                Context.MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        screenCaptureLauncher.launch(
            projectionManager.createScreenCaptureIntent()
        )
    }
}