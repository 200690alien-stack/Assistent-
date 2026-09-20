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
                Toast.makeText(
                    this,
                    "Нет разрешения на микрофон",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val screenCaptureLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                    ?: return@registerForActivityResult

                val intent = Intent(
                    this,
                    ScreenCaptureService::class.java
                ).apply {
                    putExtra(
                        ScreenCaptureService.EXTRA_RESULT_CODE,
                        result.resultCode
                    )
                    putExtra(ScreenCaptureService.EXTRA_DATA, data)
                }

                ContextCompat.startForegroundService(this, intent)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }

        layout.addView(Button(this).apply {
            text = "Микрофон"
            setOnClickListener {
                if (
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startListener()
                } else {
                    requestMicrophone.launch(
                        Manifest.permission.RECORD_AUDIO
                    )
                }
            }
        })

        layout.addView(Button(this).apply {
            text = "Экран"
            setOnClickListener {
                requestScreenCapture()
            }
        })

        layout.addView(Button(this).apply {
            text = "Остановить"
            setOnClickListener {
                stopService(
                    Intent(
                        this@MainActivity,
                        WakeWordService::class.java
                    )
                )
                stopService(
                    Intent(
                        this@MainActivity,
                        ScreenCaptureService::class.java
                    )
                )
            }
        })

        setContentView(layout)
    }

    private fun startListener() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, WakeWordService::class.java)
        )
    }

    private fun requestScreenCapture() {
        val manager = getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        screenCaptureLauncher.launch(
            manager.createScreenCaptureIntent()
        )
    }
}                        putExtra(
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
