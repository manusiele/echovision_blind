package com.example.zira

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class ListeningService : Service() {
    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListening = false
    private var ttsInitialized = false

    companion object {
        const val ACTION_START_LISTENING = "com.example.zira.ACTION_START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.example.zira.ACTION_STOP_LISTENING"
    }

    override fun onCreate() {
        super.onCreate()
        initializeTTS()
        initializeSpeechRecognizer()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status: Int ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.e("ListeningService", "TTS language not supported")
                } else {
                    ttsInitialized = true
                    Log.d("ListeningService", "TTS initialized successfully")
                }
            } else {
                Log.e("ListeningService", "TTS initialization failed")
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                Log.d("ListeningService", "Ready to listen")
            }

            override fun onBeginningOfSpeech() {
                Log.d("ListeningService", "Speech detected")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level monitoring (optional)
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Buffer received (optional)
            }

            override fun onEndOfSpeech() {
                isListening = false
                Log.d("ListeningService", "Speech ended")
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMessage = getErrorMessage(error)
                Log.e("ListeningService", "Speech recognition error: $errorMessage")

                // Don't restart on certain errors
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // Do not restart listening automatically
                    }

                    SpeechRecognizer.ERROR_CLIENT,
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        Log.e("ListeningService", "Critical error, stopping service")
                        stopSelf()
                    }

                    else -> {
                        speak("Error listening. Retrying...")
                        // Do not restart listening automatically
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                matches?.firstOrNull()?.let { command ->
                    Log.d("ListeningService", "Recognized command: $command")
                    processCommand(command)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )
                matches?.firstOrNull()?.let { partial ->
                    Log.d("ListeningService", "Partial result: $partial")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d("ListeningService", "Event received: $eventType")
            }
        })
    }

    private fun processCommand(command: String) {
        val lowercaseCommand = command.lowercase(Locale.getDefault())

        when {
            lowercaseCommand.contains("call mom") -> {
                speak("Calling Mom...")
                // TODO: Implement call functionality
            }

            lowercaseCommand.contains("open messages") -> {
                speak("Opening messages...")
                // TODO: Implement open messages functionality
            }

            lowercaseCommand.contains("emergency") -> {
                speak("Initiating emergency call...")
                // TODO: Implement emergency call
            }

            lowercaseCommand.contains("help") -> {
                speak("Available commands: Call Mom, Open Messages, Emergency")
            }

            else -> {
                speak("Command not recognized. Say 'help' for available commands.")
            }
        }
    }

    fun startListening() {
        if (!isListening && ::speechRecognizer.isInitialized) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                        3000L
                    )
                }
                speechRecognizer.startListening(intent)
                Log.d("ListeningService", "Started listening")
            } catch (e: Exception) {
                Log.e("ListeningService", "Error starting speech recognition", e)
                isListening = false
            }
        }
    }

    private fun speak(text: String) {
        if (::tts.isInitialized && ttsInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            Log.d("ListeningService", "Speaking: $text")
        } else {
            Log.e("ListeningService", "TTS not initialized, cannot speak")
        }
    }

    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error: $error"
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ListeningService", "Service started")
        intent?.action?.let { action ->
            when (action) {
                ACTION_START_LISTENING -> startListening()
                ACTION_STOP_LISTENING -> stopListening()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("ListeningService", "Service destroyed")

        if (::speechRecognizer.isInitialized) {
            speechRecognizer.stopListening()
            speechRecognizer.destroy()
        }

        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // No binding needed for this service
    }

    fun stopListening() {
        if (isListening && ::speechRecognizer.isInitialized) {
            speechRecognizer.stopListening()
            isListening = false
            Log.d("ListeningService", "Stopped listening")
        }
    }
}