package com.example.zira

import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.util.*

class VolumeActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech
    private lateinit var audioManager: AudioManager
    private var voiceCommand by mutableStateOf("")
    private var currentVolume by mutableStateOf(0)
    private var maxVolume by mutableStateOf(15)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        voiceCommand = intent.getStringExtra(MainActivity.EXTRA_COMMAND) ?: ""
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        processVolumeCommand(voiceCommand)
        initializeTTS()

        setContent {
            ZiraTheme {
                VolumeScreen()
            }
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                announceVolume()
            }
        }
    }

    private fun processVolumeCommand(command: String) {
        when {
            command.contains("up", ignoreCase = true) -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            command.contains("down", ignoreCase = true) -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            command.contains("mute", ignoreCase = true) -> {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            }
            command.contains("max", ignoreCase = true) -> {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
            }
        }
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    private fun announceVolume() {
        val percentage = (currentVolume * 100) / maxVolume
        tts.speak("Volume is at $percentage percent", TextToSpeech.QUEUE_FLUSH, null, null)
    }

    @Composable
    fun VolumeScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "🔊",
                    fontSize = 80.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "Volume Level",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "${(currentVolume * 100) / maxVolume}%",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        LinearProgressIndicator(
                            progress = { currentVolume.toFloat() / maxVolume },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = Color(0xFF2196F3),
                            trackColor = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "Command heard:",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        Text(
                            text = "\"$voiceCommand\"",
                            fontSize = 16.sp,
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { finish() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Back", fontSize = 16.sp)
                }
            }
        }
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}