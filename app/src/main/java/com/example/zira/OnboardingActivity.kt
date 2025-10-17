package com.example.zira

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

class OnboardingActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnboardingScreen()
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(72.dp) // Larger for accessibility
            .width(200.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF2196F3), Color(0xFF1976D2)), // ZIRA Blue
                        start = Offset(0f, 0f),
                        end = Offset(100f, 100f)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OnboardingScreen() {
    val context = LocalContext.current

    // **START AUDIO FEEDBACK**
    LaunchedEffect(Unit) @androidx.annotation.RequiresPermission(android.Manifest.permission.VIBRATE) {
        speakWelcome(context)
        vibrateWelcome(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // ZIRA Black background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // **MAIN LOGO** (Your existing layout)
            Image(
                painter = painterResource(id = R.drawable.logo2),
                contentDescription = "ZIRA Logo",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 220.dp)
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.FillWidth
            )

            // **APP NAME ROW**
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "ZIRA Icon",
                    modifier = Modifier.width(60.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Zira",
                    color = Color.White,
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // **TAGLINE**
            Text(
                text = "Smarter Assistance for a Better Tomorrow!",
                color = Color.White,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // **MAIN DESCRIPTION** (Updated for voice assistant)
            Text(
                text = "Welcome! I'm your voice assistant for complete phone independence.\n\n" +
                        "Long press Volume Down (2 seconds) anytime to activate me. I'll help you:\n\n" +
                        "📞 Make calls\n" +
                        "💬 Read & send messages\n" +
                        "👁️ Read signs & identify objects\n" +
                        "📱 Navigate hands-free",
                color = Color.Gray,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                lineHeight = 22.sp
            )

            // **GET STARTED BUTTON**
            GradientButton(
                text = "START SETUP",
                onClick = {
                    speak("Starting setup...", context)
                    val intent = Intent(context, PermissionsActivity::class.java)
                    context.startActivity(intent)
                    (context as? ComponentActivity)?.finish()
                },
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // **VOICE ACTIVATION HINT**
            Text(
                text = "Or say 'Start Setup' now",
                color = Color(0xFFBBDEFB),
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(bottom = 150.dp)
                    .clickable {
                        speak("Starting setup...", context)
                        val intent = Intent(context, PermissionsActivity::class.java)
                        context.startActivity(intent)
                        (context as? ComponentActivity)?.finish()
                    }
            )
        }
    }
}

// **ACCESSIBILITY HELPERS**
private fun speakWelcome(context: Context) {
    // TODO: TTS.speak("Welcome to Zira, your personal voice assistant. I'll help you navigate your phone, read messages, identify objects, and much more. To begin setup, tap Get Started or say Start Setup.", ...)
}

@RequiresApi(Build.VERSION_CODES.O)
@RequiresPermission(Manifest.permission.VIBRATE)
private fun vibrateWelcome(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(100, 50, 100), -1))
}

private fun speak(text: String, context: Context) {
    // TODO: Implement TTS
}