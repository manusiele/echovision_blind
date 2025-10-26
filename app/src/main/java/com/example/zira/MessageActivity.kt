package com.example.zira

import android.content.Intent
import android.net.Uri
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

class MessageActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    private var voiceCommand = ""
    private var extractedNumber = ""
    private var initialMessageStatus = ""

    companion object {
        const val EXTRA_COMMAND = "extra_command"
        private const val MIN_PHONE_DIGITS = 7
        private const val PHONE_10_DIGIT = 10
        private const val PHONE_11_DIGIT = 11
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        voiceCommand = intent.getStringExtra(EXTRA_COMMAND) ?: ""
        parseMessageCommand(voiceCommand)
        initializeTTS()

        setContent {
            ZiraTheme {
                MessageScreen()
            }
        }
    }

    private fun parseMessageCommand(command: String) {
        // Extract continuous digit sequences
        val digitSequences = command.split(Regex("\\D+")).filter { it.isNotEmpty() }

        // Find the first sequence with at least MIN_PHONE_DIGITS
        extractedNumber = digitSequences.firstOrNull { it.length >= MIN_PHONE_DIGITS } ?: ""

        initialMessageStatus = if (extractedNumber.isNotEmpty()) {
            "Message ready to send to ${formatPhoneNumber(extractedNumber)}"
        } else {
            "No valid phone number found in command"
        }
    }

    private fun formatPhoneNumber(number: String): String {
        return when (number.length) {
            PHONE_10_DIGIT -> "(${number.substring(0, 3)}) ${number.substring(3, 6)}-${number.substring(6)}"
            PHONE_11_DIGIT -> "+${number[0]} (${number.substring(1, 4)}) ${number.substring(4, 7)}-${number.substring(7)}"
            else -> number
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.apply {
                    language = Locale.US
                    speak(initialMessageStatus, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }
    }

    private fun sendMessage() {
        if (extractedNumber.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$extractedNumber")
                }
                startActivity(intent)
            } catch (e: Exception) {
                // Handle case where no SMS app is available
                e.printStackTrace()
            }
        }
    }

    @Composable
    fun MessageScreen() {
        val messageStatus by remember { mutableStateOf(initialMessageStatus) }
        val hasValidNumber = remember { extractedNumber.isNotEmpty() }

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
                    text = "💬",
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
                            text = "Send Message",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = messageStatus,
                            fontSize = 18.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        if (hasValidNumber) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "To: ${formatPhoneNumber(extractedNumber)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2196F3)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

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

                if (hasValidNumber) {
                    Button(
                        onClick = { sendMessage() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Send Message", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

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
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}