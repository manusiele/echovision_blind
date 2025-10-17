package com.example.zira

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.delay
import java.util.*

class ActivationTestActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech
    private lateinit var vibrator: Vibrator
    private lateinit var audioManager: AudioManager
    private var originalVolume = 0

    // State management for key press
    private var isLongPressing = mutableStateOf(false)
    private var pressStartTime = mutableStateOf(0L)
    private var progress = mutableStateOf(0f)
    private var testComplete = mutableStateOf(false)
    private var hasSpokenOneSecond = false
    private var hasSpokenAlmost = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
        }
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        setContent {
            ZiraTheme {
                ActivationTestScreen()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (event?.repeatCount == 0) {
                // First press down
                pressStartTime.value = System.currentTimeMillis()
                isLongPressing.value = true
                hasSpokenOneSecond = false
                hasSpokenAlmost = false
            }
            // Prevent volume change
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (isLongPressing.value) {
                val elapsed = System.currentTimeMillis() - pressStartTime.value
                if (elapsed < 2000) {
                    // Released too early
                    isLongPressing.value = false
                    progress.value = 0f
                }
            }
            // Restore original volume
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun playActivationBeep() {
        tts.playSilentUtterance(100, TextToSpeech.QUEUE_FLUSH, null)
    }

    private fun vibrateShort() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun vibrateSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    private fun completeOnboardingDelayed() {
        val prefs = getSharedPreferences("ZiraPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()

        speak("Setup complete! Zira is ready. Long press Volume Up to activate me anytime. Moving to the main screen in 5 seconds.")
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            startService(Intent(this, ListeningService::class.java))
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 5000)
    }

    @Composable
    fun ActivationTestScreen() {
        val isPressingState = isLongPressing.value
        val progressState = progress.value
        val testCompleteState = testComplete.value

        LaunchedEffect(Unit) {
            speak("Welcome to the activation test, the final step of Zira setup. This app helps blind users with voice commands. On this page, you'll learn to activate Zira by long pressing the Volume Up button for 2 seconds. You'll hear a countdown, a beep, and feel a vibration when successful. The screen shows 'Step 5 of 5', 'Activation Test' in large white text, a Volume Up icon with 'HOLD 2s', and a progress bar that fills as you hold. Press and hold the Volume Up button on your phone's side now.")
        }

        LaunchedEffect(isPressingState) {
            while (isPressingState) {
                val elapsed = System.currentTimeMillis() - pressStartTime.value
                progress.value = (elapsed.toFloat() / 2000).coerceIn(0f, 1f)

                when {
                    elapsed >= 1000 && elapsed < 1500 && !hasSpokenOneSecond -> {
                        speak("1 second left...")
                        vibrateShort()
                        hasSpokenOneSecond = true
                    }
                    elapsed >= 1500 && elapsed < 2000 && !hasSpokenAlmost -> {
                        speak("Almost there...")
                        vibrateShort()
                        hasSpokenAlmost = true
                    }
                    elapsed >= 2000 -> {
                        speak("Activated!")
                        vibrateSuccess()
                        testComplete.value = true
                        isLongPressing.value = false
                        playActivationBeep()
                        onVolumeLongPress()
                        break
                    }
                }
                delay(50)
            }
            if (!isPressingState && !testCompleteState) {
                progress.value = 0f
            }
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
                    text = "Step 5 of 5",
                    color = Color.Gray,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Activation Test",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(48.dp))

                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPressingState) Color(0xFF2196F3) else Color.Gray
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume Up button",
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "HOLD 2s",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "🎯 Long press Volume Up button",
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "You'll hear: Countdown, BEEP! + Vibration\nThen say any command!",
                    fontSize = 18.sp,
                    color = Color(0xFF2196F3),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { progressState },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF2196F3)
                )

                Spacer(modifier = Modifier.weight(1f))

                if (testCompleteState) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "✓ PERFECT!",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    private fun onVolumeLongPress() {
        speak("Perfect! You've activated Zira. This is how you'll wake me up anytime. You'll hear a beep and feel a vibration. Now Zira is listening. Try saying 'Call Mom' or 'What time is it?' Or say 'Done' to finish setup.")
        completeOnboardingDelayed()
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}