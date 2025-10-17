package com.example.zira

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zira.ui.theme.ZiraTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceTrainingActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer

    private val trainingPhrases = listOf(
        "Zira, open my messages",
        "What time is it?",
        "Read my emails",
        "Call Mom",
        "What's in front of me?"
    )

    private var currentStep = 0
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                speak("Try again!")
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000)
                    startTrainingStep()
                }
            }
            override fun onResults(results: Bundle?) {
                nextStep()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        setContent {
            ZiraTheme {
                VoiceTrainingScreen()
            }
        }
    }

    @Composable
    fun VoiceTrainingScreen() {
        val currentStepState = remember { mutableStateOf(0) }
        val isListeningState = remember { mutableStateOf(false) }

        LaunchedEffect(currentStep) { currentStepState.value = currentStep }
        LaunchedEffect(isListening) { isListeningState.value = isListening }

        LaunchedEffect(Unit) {
            speak("Voice training. Repeat after me to help me understand you better.")
            delay(1500)
            startTrainingStep()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Step ${currentStepState.value + 1} of 5",
                    color = Color.Gray,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Voice Training",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Mic Button (Pulsing with Unicode)
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isListeningState.value) Color(0xFF2196F3) else Color.Gray
                    )
                ) {
                    Text(
                        text = "🎤",  // Unicode microphone symbol (U+1F3A4)
                        fontSize = 40.sp,
                        color = Color.White,
                        modifier = Modifier
                            .size(80.dp)
                            .padding(16.dp)
                            .wrapContentSize(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = trainingPhrases[currentStepState.value],
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isListeningState.value) "🎤 Listening..." else "Speak now!",
                    fontSize = 18.sp,
                    color = Color(0xFF2196F3)
                )

                Spacer(modifier = Modifier.weight(1f))

                LinearProgressIndicator(
                    progress = { (currentStepState.value + 1f) / 5f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF2196F3)
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    private fun startTrainingStep() {
        speak("Repeat: ${trainingPhrases[currentStep]}")
        isListening = true
        speechRecognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
        )
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun nextStep() {
        currentStep++
        isListening = false
        if (currentStep >= 5) {
            completeTraining()
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                startTrainingStep()
            }
        }
    }

    private fun completeTraining() {
        speak("Perfect! Voice training complete.")
        val prefs = getSharedPreferences("ZiraPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("voice_trained", true).apply()

        val intent = Intent(this, EmergencyContactActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        speechRecognizer.destroy()
        tts.shutdown()
        super.onDestroy()
    }
}