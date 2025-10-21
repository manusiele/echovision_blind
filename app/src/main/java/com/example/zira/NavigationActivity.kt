package com.example.zira

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.zira.ui.theme.ZiraTheme
import java.util.*

class NavigationActivity : ComponentActivity() {
    private lateinit var command: String
    private lateinit var tts: TextToSpeech
    private var currentLocation by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        command = intent.getStringExtra(MainActivity.EXTRA_COMMAND) ?: ""

        initializeTTS()
        checkLocationPermissions()
        getCurrentLocation()

        setContent {
            ZiraTheme {
                NavigationScreen()
            }
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                speak("Navigation mode activated")
            }
        }
    }

    private fun checkLocationPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 102)
        }
    }

    private fun getCurrentLocation() {
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                if (location != null) {
                    currentLocation = "${location.latitude}, ${location.longitude}"
                    speak("Current location: ${location.latitude.format()}, ${location.longitude.format()}")
                }
            }
        } catch (e: Exception) {
            speak("Could not get location")
        }
    }

    @Composable
    fun NavigationScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { finish() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Navigation",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Command Info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "You said: \"$command\"",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Current Location
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = "Location",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Current Location",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Text(
                        text = currentLocation ?: "Location not available",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            // Navigation Options
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(getNavigationOptions()) { option ->
                    NavigationOptionCard(option) {
                        handleNavigation(option)
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    @Composable
    fun NavigationOptionCard(option: NavigationOption, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "${option.emoji} ${option.title}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = option.description,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    private fun getNavigationOptions(): List<NavigationOption> {
        return listOf(
            NavigationOption("🗺️ Open Maps", "View full map", "maps"),
            NavigationOption("🧭 Get Directions", "Navigate to destination", "directions"),
            NavigationOption("📍 Share Location", "Share your location", "share"),
            NavigationOption("🏠 Home", "Navigate home", "home"),
            NavigationOption("💼 Work", "Navigate to work", "work")
        )
    }

    private fun handleNavigation(option: NavigationOption) {
        when (option.action) {
            "maps" -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=nearby"))
                    startActivity(intent)
                    speak("Opening maps")
                } catch (e: Exception) {
                    speak("Maps app not available")
                }
            }
            "directions" -> {
                speak("Say the destination address")
            }
            "share" -> {
                try {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "My location: $currentLocation")
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share Location"))
                    speak("Sharing your location")
                } catch (e: Exception) {
                    speak("Could not share location")
                }
            }
            "home", "work" -> {
                speak("${option.title} navigation not configured")
            }
        }
    }

    private fun Double.format(): String = String.format("%.4f", this)

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    data class NavigationOption(
        val title: String,
        val description: String,
        val action: String,
        val emoji: String = ""
    ) {
        constructor(fullTitle: String, description: String, action: String) : this(
            title = fullTitle.substringAfter(" "),
            description = description,
            action = action,
            emoji = fullTitle.substringBefore(" ")
        )
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}