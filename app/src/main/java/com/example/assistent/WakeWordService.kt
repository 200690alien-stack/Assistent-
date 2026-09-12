package com.example.assistent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat

class WakeWordService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent

    private val handler = Handler(Looper.getMainLooper())

    private var lastTriggerTime = 0L

    companion object {
        private const val CHANNEL_ID = "wake_word_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Слушаю..."))

        setupSpeechRecognizer()
    }

    private fun setupSpeechRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateNotification("Распознавание речи недоступно")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizerIntent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "ru-RU"
            )

            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )

            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                5
            )
        }

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    updateNotification("Слушаю...")
                }

                override fun onBeginningOfSpeech() {
                }

                override fun onRmsChanged(rmsdB: Float) {
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                }

                override fun onEndOfSpeech() {
                }

                override fun onError(error: Int) {
                    restartListening()
                }

                override fun onResults(results: Bundle?) {

                    val phrases =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    checkPhrases(phrases)

                    restartListening()
                }

                override fun onPartialResults(partialResults: Bundle?) {

                    val phrases =
                        partialResults?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    checkPhrases(phrases)
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {
                }
            }
        )

        startListening()
    }

    private fun startListening() {
        try {
            speechRecognizer?.startListening(recognizerIntent)
        } catch (_: Exception) {
            restartListening()
        }
    }

    private fun restartListening() {

        handler.removeCallbacksAndMessages(null)

        handler.postDelayed(
            {
                try {
                    speechRecognizer?.cancel()
                    speechRecognizer?.startListening(recognizerIntent)
                } catch (_: Exception) {
                }
            },
            700
        )
    }

    private fun checkPhrases(phrases: ArrayList<String>?) {

        if (phrases == null) return

        for (phrase in phrases) {

            val text = phrase
                .lowercase()
                .replace(",", "")
                .replace(".", "")
                .replace("!", "")
                .replace("?", "")
                .trim()

            when {

                containsAny(
                    text,
                    "gpt слушай",
                    "джипити слушай",
                    "gpt ха слушай",
                    "джипити ха слушай"
                ) -> {
                    triggerListen()
                    return
                }

                containsAny(
                    text,
                    "gpt смотри",
                    "джипити смотри",
                    "gpt ха смотри",
                    "джипити ха смотри"
                ) -> {
                    triggerLook()
                    return
                }

                containsAny(
                    text,
                    "эй ты здесь",
                    "эй ты тут",
                    "hey ты здесь",
                    "hey ты тут"
                ) -> {
                    triggerAreYouHere()
                    return
                }
            }
        }
    }

    private fun containsAny(
        text: String,
        vararg variants: String
    ): Boolean {

        return variants.any {
            text.contains(it)
        }
    }

    private fun canTrigger(): Boolean {

        val now = System.currentTimeMillis()

        if (now - lastTriggerTime < 1500) {
            return false
        }

        lastTriggerTime = now
        return true
    }

    private fun triggerListen() {

        if (!canTrigger()) return

        updateNotification("GPT: слушаю команду")

        // Сюда следующим шагом подключим
        // передачу твоей дальнейшей речи ассистенту.
    }

    private fun triggerLook() {

        if (!canTrigger()) return

        updateNotification("GPT: смотрю экран")

        // Сюда следующим шагом подключим
        // получение текущего кадра из ScreenCaptureService.
    }

    private fun triggerAreYouHere() {

        if (!canTrigger()) return

        updateNotification("GPT: я здесь")

        // Здесь потом сделаем голосовой ответ.
    }

    private fun updateNotification(text: String) {

        val notification =
            createNotification(text)

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.notify(
            NOTIFICATION_ID,
            notification
        )
    }

    private fun createNotification(
        text: String
    ): Notification {

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("Assistent")
            .setContentText(text)
            .setSmallIcon(
                android.R.drawable.ic_btn_speak_now
            )
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Voice listener",
                    NotificationManager.IMPORTANCE_LOW
                )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        return START_STICKY
    }

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()

        speechRecognizer = null

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null
}