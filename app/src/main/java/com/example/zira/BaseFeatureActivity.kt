package com.example.zira

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zira.ui.theme.ZiraTheme
import java.util.*

abstract class BaseFeatureActivity : ComponentActivity() {
    protected lateinit var tts: TextToSpeech
    protected var command: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        command = intent.getStringExtra(MainActivity.EXTRA_COMMAND) ?: ""

        initializeTTS()

        setContent {
            ZiraTheme {
                FeatureScreen()
            }
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                onTTSReady()
            }
        }
    }

    protected fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    abstract fun onTTSReady()

    @Composable
    abstract fun FeatureContent()

    @Composable
    private fun FeatureScreen() {
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
                FeatureContent()

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