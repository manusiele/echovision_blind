package com.example.zira

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.text.SimpleDateFormat
import java.util.*

class DateActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toast.makeText(this, "Getting today's date...", Toast.LENGTH_SHORT).show()
        initializeTTS()
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
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
        val dateString = dateFormat.format(calendar.time)

        tts.speak("Today is $dateString", TextToSpeech.QUEUE_FLUSH, null, null)

        // Finish activity after speaking
        val duration = 4000L // 4 seconds
        Thread {
            Thread.sleep(duration)
            finish()
        }.start()
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}