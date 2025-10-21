package com.example.zira

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
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
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
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

class WifiActivity : ComponentActivity() {
    private lateinit var command: String
    private lateinit var tts: TextToSpeech
    private var wifiManager: WifiManager? = null
    private var isWifiEnabled by mutableStateOf(false)
    private var connectedNetwork by mutableStateOf<String?>(null)
    private var signalStrength by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        command = intent.getStringExtra(MainActivity.EXTRA_COMMAND) ?: ""

        initializeTTS()
        checkWifiPermissions()
        initializeWifiManager()

        setContent {
            ZiraTheme {
                WifiScreen()
            }
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                speak("WiFi settings opened")
            }
        }
    }

    private fun checkWifiPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_WIFI_STATE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.CHANGE_WIFI_STATE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 105)
        }
    }

    private fun initializeWifiManager() {
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                isWifiEnabled = wifiManager?.isWifiEnabled ?: false

                // Get connected network info
                val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = connectivityManager.activeNetwork
                if (network != null) {
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                        @Suppress("DEPRECATION")
                        val connectionInfo = wifiManager?.connectionInfo
                        connectedNetwork = connectionInfo?.ssid?.replace("\"", "")
                        signalStrength = WifiManager.calculateSignalLevel(
                            connectionInfo?.rssi ?: -127,
                            5
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Composable
    fun WifiScreen() {
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
                    text = "WiFi Settings",
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

            // WiFi Status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isWifiEnabled) Color(0xFF0D47A1) else Color(0xFF3E2723)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            if (isWifiEnabled) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                            contentDescription = "WiFi",
                            tint = if (isWifiEnabled) Color(0xFF2196F3) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "WiFi Status",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWifiEnabled) Color(0xFF2196F3) else Color.Gray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Text(
                        text = if (isWifiEnabled) "WiFi is ON" else "WiFi is OFF",
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    if (isWifiEnabled && connectedNetwork != null) {
                        Text(
                            text = "Connected to: $connectedNetwork",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "Signal Strength: ${signalStrength}/5",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // WiFi Options
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(getWifiOptions()) { option ->
                    WifiOptionCard(option) {
                        handleWifiAction(option)
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    @Composable
    fun WifiOptionCard(option: WifiOption, onClick: () -> Unit) {
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

    private fun getWifiOptions(): List<WifiOption> {
        return listOf(
            WifiOption("📡 Toggle WiFi", "Turn WiFi on/off", "toggle"),
            WifiOption("🔍 Scan Networks", "Search for available networks", "scan"),
            WifiOption("🔗 Connect to Network", "Connect to a WiFi network", "connect"),
            WifiOption("⚙️ WiFi Settings", "Open WiFi settings", "settings"),
            WifiOption("🔒 Forget Network", "Disconnect from current network", "forget"),
            WifiOption("📊 Connection Details", "View network details", "details")
        )
    }

    private fun handleWifiAction(option: WifiOption) {
        when (option.action) {
            "toggle" -> {
                try {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CHANGE_WIFI_STATE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        @Suppress("DEPRECATION")
                        wifiManager?.isWifiEnabled = !isWifiEnabled
                        isWifiEnabled = !isWifiEnabled
                        speak(if (isWifiEnabled) "Turning WiFi on" else "Turning WiFi off")
                    }
                } catch (e: Exception) {
                    speak("Could not toggle WiFi")
                }
            }
            "scan" -> {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                    startActivity(intent)
                    speak("Opening WiFi network scanner")
                } catch (e: Exception) {
                    speak("Could not open WiFi settings")
                }
            }
            "connect" -> {
                speak("Network connection requires manual selection in WiFi settings")
            }
            "settings" -> {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                    startActivity(intent)
                    speak("Opening WiFi settings")
                } catch (e: Exception) {
                    speak("Could not open WiFi settings")
                }
            }
            "forget" -> {
                speak("Forgetting network requires manual confirmation in WiFi settings")
            }
            "details" -> {
                if (isWifiEnabled && connectedNetwork != null) {
                    speak("Connected to $connectedNetwork with signal strength $signalStrength out of 5")
                } else {
                    speak("Not connected to any WiFi network")
                }
            }
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    data class WifiOption(
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