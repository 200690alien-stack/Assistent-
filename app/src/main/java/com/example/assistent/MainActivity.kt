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

    private val requestMicrophone =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startListener()
            } else {
                showMessage("Нет разрешения на микрофон")
            }
        }

    private val screenCaptureLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data

                if (data != null) {
                    val serviceIntent =
                        Intent(
                            this,
                            ScreenCaptureService::class.java
                        ).apply {
                            putExtra(
                                ScreenCaptureService.EXTRA_RESULT_CODE,
                                result.resultCode
                            )
                            putExtra(
                                ScreenCaptureService.EXTRA_RESULT_DATA,
                                data
                            )
                        }

                    ContextCompat.startForegroundService(
                        this,
                        serviceIntent
                    )
                }
            } else {
                showMessage("Захват экрана отменён")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val padding =
            (20 *
