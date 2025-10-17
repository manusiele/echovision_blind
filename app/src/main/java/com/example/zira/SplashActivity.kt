package com.example.zira

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashScreen()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SplashScreen() {
    val context = LocalContext.current

    // **STARTUP FEEDBACK** (Audio + Haptic)
    LaunchedEffect(Unit) {
        playStartupChime(context) // Subtle chime sound
        vibrateStartup(context)   // Gentle pulse
        speak("Zira loading...", context)
    }

    // **NAVIGATION LOGIC** (2.5 seconds)
    LaunchedEffect(Unit) {
        delay(2500)

        val sharedPreferences = context.getSharedPreferences("ZiraPrefs", Context.MODE_PRIVATE)
        val hasCompletedOnboarding = sharedPreferences.getBoolean("onboarding_completed", false)

        val intent = if (hasCompletedOnboarding) {
            Intent(context, MainActivity::class.java)  // PAGE 3
        } else {
            Intent(context, OnboardingActivity::class.java) // PAGE 2
        }

        context.startActivity(intent)
        (context as? ComponentActivity)?.finish()
    }

    // **VISUAL DESIGN** (High Contrast)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E3A8A)), // Deep Blue
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = com.example.zira.R.drawable.logo2),
                contentDescription = "Zira Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Zira",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "See Through Echoes",
                color = Color(0xFFF59E0B), // Amber accent
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Loading...",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

// **ACCESSIBILITY HELPERS**
private fun playStartupChime(context: Context) {
    // TODO: Play R.raw.startup_chime (440Hz → 880Hz, 200ms)
}

@RequiresApi(Build.VERSION_CODES.O)
private fun vibrateStartup(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
}

private fun speak(text: String, context: Context) {
    // TODO: Use TextToSpeech (implement in next step)
    // TTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
}