package com.example.zira

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothAudio
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

class BluetoothActivity : ComponentActivity() {
    private lateinit var command: String
    private lateinit var tts: TextToSpeech
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isBluetoothEnabled by mutableStateOf(false)

    // Activity result launcher for Bluetooth enable request
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isBluetoothEnabled = bluetoothAdapter?.isEnabled ?: false
        if (isBluetoothEnabled) {
            speak("Bluetooth turned on successfully")
        } else {
            speak("Bluetooth was not enabled")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        command = intent.getStringExtra(MainActivity.EXTRA_COMMAND) ?: ""

        initializeTTS()
        checkBluetoothPermissions()
        initializeBluetoothAdapter()

        setContent {
            ZiraTheme {
                BluetoothScreen()
            }
        }

        // Handle voice command automatically
        handleVoiceCommand(command)
    }

    private fun handleVoiceCommand(command: String) {
        val lowerCommand = command.lowercase()

        when {
            lowerCommand.contains("turn on") ||
                    lowerCommand.contains("enable") ||
                    lowerCommand.contains("open bluetooth") ||
                    lowerCommand.contains("switch on") -> {
                if (!isBluetoothEnabled) {
                    turnOnBluetooth()
                } else {
                    speak("Bluetooth is already on")
                }
            }
            lowerCommand.contains("turn off") ||
                    lowerCommand.contains("disable") ||
                    lowerCommand.contains("close bluetooth") ||
                    lowerCommand.contains("switch off") -> {
                if (isBluetoothEnabled) {
                    turnOffBluetooth()
                } else {
                    speak("Bluetooth is already off")
                }
            }
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                speak("Bluetooth settings opened")
            }
        }
    }

    private fun checkBluetoothPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 104)
        }
    }

    private fun initializeBluetoothAdapter() {
        val bluetoothManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(BluetoothManager::class.java)
        } else {
            null
        }

        bluetoothAdapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothManager?.adapter
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }

        isBluetoothEnabled = bluetoothAdapter?.isEnabled ?: false
    }

    @Composable
    fun BluetoothScreen() {
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Bluetooth",
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

            // Bluetooth Status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBluetoothEnabled) Color(0xFF1B5E20) else Color(0xFF3E2723)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Filled.BluetoothAudio,
                            contentDescription = "Bluetooth",
                            tint = if (isBluetoothEnabled) Color(0xFF4CAF50) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Bluetooth Status",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBluetoothEnabled) Color(0xFF4CAF50) else Color.Gray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Text(
                        text = if (isBluetoothEnabled) "Bluetooth is ON" else "Bluetooth is OFF",
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    if (isBluetoothEnabled) {
                        if (ContextCompat.checkSelfPermission(
                                this@BluetoothActivity,
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                    Manifest.permission.BLUETOOTH_CONNECT
                                else
                                    Manifest.permission.BLUETOOTH
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            Text(
                                text = "Device Name: ${bluetoothAdapter?.name ?: "Unknown"}",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Bluetooth Options
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(getBluetoothOptions()) { option ->
                    BluetoothOptionCard(option) {
                        handleBluetoothAction(option)
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    @Composable
    fun BluetoothOptionCard(option: BluetoothOption, onClick: () -> Unit) {
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

    private fun getBluetoothOptions(): List<BluetoothOption> {
        return listOf(
            BluetoothOption("🔋 Toggle Bluetooth", "Turn Bluetooth on/off", "toggle"),
            BluetoothOption("📱 Paired Devices", "View paired devices", "paired"),
            BluetoothOption("🔍 Scan for Devices", "Search for new devices", "scan"),
            BluetoothOption("🔗 Connect Device", "Connect to a device", "connect"),
            BluetoothOption("⚙️ Bluetooth Settings", "Open Bluetooth settings", "settings")
        )
    }

    private fun handleBluetoothAction(option: BluetoothOption) {
        when (option.action) {
            "toggle" -> toggleBluetooth()
            "paired" -> showPairedDevices()
            "scan" -> openBluetoothSettings()
            "connect" -> speak("Device connection requires manual selection")
            "settings" -> openBluetoothSettings()
        }
    }

    private fun toggleBluetooth() {
        if (isBluetoothEnabled) {
            turnOffBluetooth()
        } else {
            turnOnBluetooth()
        }
    }

    private fun turnOnBluetooth() {
        try {
            if (bluetoothAdapter == null) {
                speak("Bluetooth is not available on this device")
                return
            }

            // Check permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    speak("Bluetooth permission not granted")
                    return
                }
            }

            // Turn ON - Use BluetoothAdapter.ACTION_REQUEST_ENABLE
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
            speak("Turning on Bluetooth")
        } catch (e: SecurityException) {
            speak("Security exception: Bluetooth permission denied")
            e.printStackTrace()
        } catch (e: Exception) {
            speak("Could not turn on Bluetooth")
            e.printStackTrace()
        }
    }

    private fun turnOffBluetooth() {
        try {
            if (bluetoothAdapter == null) {
                speak("Bluetooth is not available on this device")
                return
            }

            // Check permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    speak("Bluetooth permission not granted")
                    return
                }
            }

            // Turn OFF - Open system Bluetooth settings
            val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
            startActivity(intent)
            speak("Please turn off Bluetooth from settings. I cannot turn it off directly for security reasons.")
        } catch (e: SecurityException) {
            speak("Security exception: Bluetooth permission denied")
            e.printStackTrace()
        } catch (e: Exception) {
            speak("Could not open Bluetooth settings")
            e.printStackTrace()
        }
    }

    private fun showPairedDevices() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    speak("Bluetooth permission not granted")
                    return
                }
            }

            val pairedDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
            speak("You have ${pairedDevices.size} paired devices")
        } catch (e: SecurityException) {
            speak("Could not retrieve paired devices: Permission denied")
        } catch (e: Exception) {
            speak("Could not retrieve paired devices")
        }
    }

    private fun openBluetoothSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
            startActivity(intent)
            speak("Opening Bluetooth settings")
        } catch (e: Exception) {
            speak("Could not open Bluetooth settings")
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    data class BluetoothOption(
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

    override fun onResume() {
        super.onResume()
        // Update Bluetooth status when returning to the activity
        isBluetoothEnabled = bluetoothAdapter?.isEnabled ?: false
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}