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

class PhoneActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech
    private var voiceCommand = ""
    private var extractedNumber = ""
    private var initialCallStatus = ""

    companion object {
        const val EXTRA_COMMAND = "extra_command"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        voiceCommand = intent.getStringExtra(EXTRA_COMMAND) ?: ""
        extractPhoneNumber(voiceCommand)
        initializeTTS()

        setContent {
            ZiraTheme {
                PhoneScreen()
            }
        }
    }

    private fun extractPhoneNumber(command: String) {
        val digits = command.filter { it.isDigit() }
        if (digits.length >= 7) {
            extractedNumber = digits
            initialCallStatus = "Number extracted: ${formatPhoneNumber(digits)}"
        } else {
            initialCallStatus = "No valid number found in command"
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
                tts.speak(initialCallStatus, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    private fun makeCall() {
        if (extractedNumber.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$extractedNumber"))
            startActivity(intent)
        }
    }

    @Composable
    fun PhoneScreen() {
        var callStatus by remember { mutableStateOf(initialCallStatus) }

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
                    text = "📞",
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
                            text = "Phone Call",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = callStatus,
                            fontSize = 18.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        if (extractedNumber.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = formatPhoneNumber(extractedNumber),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
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
                        onClick = { makeCall() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Call Now", fontSize = 16.sp)
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
        if (::tts.isInitialized) {
            tts.shutdown()
        }
        super.onDestroy()
    }
}