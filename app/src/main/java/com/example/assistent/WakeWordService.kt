package com.example.assistent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.core.app.NotificationCompat

class WakeWordService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var finished = false

    companion object {
        private const val CHANNEL_ID = "wake_word_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            createNotification("Слушаю одну фразу...")
        )

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            finishListening("Распознавание речи недоступно")
            return
        }

        try {
            setupSpeechRecognizer()
        } catch (error: Exception) {
            finishListening(
                "Не удалось включить микрофон: " +
                    (error.message ?: "неизвестная ошибка")
            )
        }
    }

    private fun setupSpeechRecognizer() {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer = recognizer

        recognizer.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {}

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (finished) return

                    val message = when (error) {
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            "Речь не услышана. Нажми включение снова."

                        SpeechRecognizer.ERROR_NO_MATCH ->
                            "Фраза не распознана. Нажми включение снова."

                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            "Нет разрешения на микрофон"

                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                            "Распознавание занято. Код ошибки: $error"

                        else ->
                            "Ошибка распознавания: $error"
                    }

                    finishListening(message)
                }

                override fun onResults(results: Bundle?) {
                    if (finished) return

                    val phrase = results
                        ?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )
                        ?.firstOrNull()

                    finishListening(
                        if (phrase.isNullOrBlank()) {
                            "Фраза не распознана"
                        } else {
                            "Распознано: $phrase"
                        }
                    )
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {}

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {}
            }
        )

        val recognizerIntent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer.startListening(recognizerIntent)
    }

    private fun finishListening(message: String) {
        if (finished) return
        finished = true

        Toast.makeText(
            applicationContext,
            message,
            Toast.LENGTH_LONG
        ).show()

        stopSelf()
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Assistent")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice listener",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        finished = true

        val recognizer = speechRecognizer
        speechRecognizer = null

        try {
            recognizer?.cancel()
        } finally {
            recognizer?.destroy()
            stopForeground(STOP_FOREGROUND_REMOVE)
            super.onDestroy()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
