package com.example.zira

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zira.ui.theme.ZiraTheme
import java.util.*

class PermissionsActivity : ComponentActivity() {

    private lateinit var tts: TextToSpeech
    private var ttsReady = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            speak("All permissions granted. Thank you!")
            navigateToNext()
        } else {
            speak("Some permissions were denied. You can still continue, but some features may be limited.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                tts.language = Locale.US
            }
        }

        setContent {
            ZiraTheme {
                PermissionsScreen(
                    onRequestPermissions = { requestAllPermissions() },
                    onSkip = { navigateToNext() },
                    onEnableScreenReader = { openAccessibilitySettings() }
                )
            }
        }
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            // Audio & Camera
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,

            // Contacts & Phone
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,

            // SMS
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,

            // Calendar
            Manifest.permission.READ_CALENDAR,

            // Location
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Android 13+ (API 33+) - Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        }

        // Storage permissions (for older Android versions)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        // Bluetooth permissions for Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            // Older Bluetooth permissions for Android 11 and below
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        speak("Requesting permissions. Please allow access to help you better.")
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun speak(text: String) {
        if (ttsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun navigateToNext() {
        val sharedPreferences = getSharedPreferences("ZiraPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("permissions_requested", true).apply()

        val intent = Intent(this, ActivationTestActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}

@Composable
fun PermissionsScreen(
    onRequestPermissions: () -> Unit,
    onSkip: () -> Unit,
    onEnableScreenReader: () -> Unit
) {
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "permission_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Permissions",
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale),
                tint = Color(0xFF2196F3)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Permissions Required",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "To provide the best assistance, Zira needs access to certain features",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            PermissionItem(
                icon = Icons.Default.Phone,
                title = "Microphone",
                description = "To listen to your voice commands and help you navigate"
            )

            PermissionItem(
                icon = Icons.Default.Info,
                title = "Camera",
                description = "To read text, identify objects, and describe your surroundings"
            )

            PermissionItem(
                icon = Icons.Default.Phone,
                title = "Phone & Calls",
                description = "To make calls and manage your contacts by voice"
            )

            PermissionItem(
                icon = Icons.Default.Email,
                title = "Messages",
                description = "To read and send messages for you"
            )

            PermissionItem(
                icon = Icons.Default.DateRange,
                title = "Calendar",
                description = "To manage your schedule and reminders"
            )

            PermissionItem(
                icon = Icons.Default.LocationOn,
                title = "Location",
                description = "To help you navigate and find nearby places"
            )

            PermissionItem(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                description = "To alert you about important events and messages"
            )

            PermissionItem(
                icon = Icons.Default.Star,
                title = "Bluetooth",
                description = "To connect with nearby devices and accessories"
            )

            PermissionItem(
                icon = Icons.Default.Build,
                title = "Storage",
                description = "To access and manage your files and media"
            )

            PermissionItem(
                icon = Icons.Filled.Accessibility,
                title = "Screen Reader",
                description = "To read the screen content and provide voice feedback"
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onEnableScreenReader,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Accessibility,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Enable Screen Reader",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRequestPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Grant Permissions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Skip for Now",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}