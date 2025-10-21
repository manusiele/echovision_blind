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
    private lateinit var tts: TextToSpeech
    private var voiceCommand by mutableStateOf("")
    private var messageStatus by mutableStateOf("Ready to send message")
    private var extractedNumber by mutableStateOf("")
    private var messageText by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        voiceCommand = intent.getStringExtra(MainActivity.EXTRA_COMMAND) ?: ""
        parseMessageCommand(voiceCommand)
        initializeTTS()

        setContent {
            ZiraTheme {
                MessageScreen()
            }
        }
    }

    private fun parseMessageCommand(command: String) {
        val digits = command.filter { it.isDigit() }
        if (digits.length >= 7) {
            extractedNumber = digits
        }
        messageText = command
        messageStatus = if (extractedNumber.isNotEmpty()) {
            "Message ready to send to ${formatPhoneNumber(extractedNumber)}"
        } else {
            "No recipient found"
        }
    }

    private fun formatPhoneNumber(number: String): String {
        return when {
            number.length == 10 -> "(${number.substring(0, 3)}) ${number.substring(3, 6)}-${number.substring(6)}"
            number.length == 11 -> "+${number[0]} (${number.substring(1, 4)}) ${number.substring(4, 7)}-${number.substring(7)}"
            else -> number
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                tts.speak(messageStatus, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    private fun sendMessage() {
        if (extractedNumber.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$extractedNumber"))
            startActivity(intent)
        }
    }

    @Composable
    fun MessageScreen() {
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

                        if (extractedNumber.isNotEmpty()) {
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

                if (extractedNumber.isNotEmpty()) {
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
        tts.shutdown()
        super.onDestroy()
    }
}