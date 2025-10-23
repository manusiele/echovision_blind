package com.example.zira

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

class DateActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech
    private var dateString by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toast.makeText(this, "Getting today's date...", Toast.LENGTH_SHORT).show()

        // Get the date
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
        dateString = dateFormat.format(calendar.time)

        initializeTTS()

        setContent {
            MaterialTheme {
                DateScreen()
            }
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                announceDate()
            }
        }
    }

    private fun announceDate() {
        tts.speak("Today is $dateString", TextToSpeech.QUEUE_FLUSH, null, null)

        // Finish activity after speaking
        val duration = 4000L // 4 seconds
        Thread {
            Thread.sleep(duration)
            runOnUiThread {
                finish()
            }
        }.start()
    }

    @Composable
    private fun DateScreen() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Today's Date",
                    fontSize = 24.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = dateString,
                    fontSize = 20.sp,
                    color = Color(0xFF2196F3)
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { finish() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Text("Back to ZIRA", fontSize = 16.sp)
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