package com.example.zira

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.zira.ui.theme.ZiraTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    // Core Components
    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var audioManager: AudioManager
    private lateinit var wifiManager: WifiManager
    private val prefs by lazy { getSharedPreferences("ZiraPrefs", Context.MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())

    // State Management
    private var isListening by mutableStateOf(false)
    private var statusMessage by mutableStateOf("Ready - Long press Volume Up to speak")
    private var lastCommand by mutableStateOf("")

    // Button Press Tracking
    private var buttonPressStartTime = 0L
    private val longPressThreshold = 500L
    private var isVolumeButtonPressed = false
    private var capturedCommand: String? = null

    // Command System
    private val commandRegistry = CommandRegistry()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check permissions
        checkPermissions()

        // Initialize services
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        initializeTTS()
        initializeSpeechRecognizer()
        registerAllCommands()

        setContent {
            ZiraTheme {
                MainScreen()
            }
        }
    }

    // ==================== INITIALIZATION ====================

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CALL_PHONE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_CONTACTS)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                speak("Zira is ready. Long press Volume Up to activate voice commands.")
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                statusMessage = "Listening... Speak now"
            }

            override fun onBeginningOfSpeech() {
                statusMessage = "Voice detected..."
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                isListening = false
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        statusMessage = "No speech detected"
                        speak("I didn't catch that. Please try again.")
                    }
                    else -> {
                        statusMessage = "Recognition error"
                        speak("Sorry, there was an error.")
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val command = matches?.firstOrNull()

                if (command != null) {
                    capturedCommand = command
                    lastCommand = "\"$command\""
                    statusMessage = lastCommand
                } else {
                    statusMessage = "No command detected"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { partial ->
                    statusMessage = "Hearing: \"$partial...\""
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    // ==================== COMMAND SYSTEM ====================

    inner class CommandRegistry {
        private val commands = mutableListOf<Command>()

        fun register(command: Command) {
            commands.add(command)
        }

        fun execute(input: String) {
            val normalized = input.lowercase().trim()

            // Find first matching command
            val command = commands.firstOrNull { it.matches(normalized) }

            if (command != null) {
                command.execute(normalized)
            } else {
                speak("I heard: $input. But I don't understand that command yet.")
                statusMessage = "Unknown command"
            }

            // Reset status after 3 seconds
            handler.postDelayed({
                statusMessage = "Ready - Long press Volume Up to speak"
            }, 3000)
        }
    }

    abstract inner class Command(
        val keywords: List<String>
    ) {
        abstract fun execute(input: String)

        fun matches(input: String): Boolean {
            return keywords.any { keyword -> input.contains(keyword) }
        }
    }

    private fun registerAllCommands() {

        // ===== TIME & DATE COMMANDS =====
        commandRegistry.register(object : Command(
            keywords = listOf("time", "what time")
        ) {
            override fun execute(input: String) {
                val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                speak("The time is $time")
                statusMessage = "Time: $time"
            }
        })

        commandRegistry.register(object : Command(
            keywords = listOf("date", "day", "today", "what day")
        ) {
            override fun execute(input: String) {
                val date = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
                speak("Today is $date")
                statusMessage = "Date: $date"
            }
        })

        // ===== BATTERY COMMANDS =====
        commandRegistry.register(object : Command(
            keywords = listOf("battery", "charge", "power")
        ) {
            override fun execute(input: String) {
                val level = getBatteryLevel()
                val charging = if (isCharging()) "and charging" else ""
                speak("Battery is at $level percent $charging")
                statusMessage = "Battery: $level% $charging"
            }
        })

        // ===== VOLUME COMMANDS =====
        commandRegistry.register(object : Command(
            keywords = listOf("volume", "sound")
        ) {
            override fun execute(input: String) {
                when {
                    input.contains("up") || input.contains("increase") || input.contains("louder") -> {
                        adjustVolume(AudioManager.ADJUST_RAISE)
                        speak("Volume increased")
                        statusMessage = "Volume up"
                    }
                    input.contains("down") || input.contains("decrease") || input.contains("lower") || input.contains("quiet") -> {
                        adjustVolume(AudioManager.ADJUST_LOWER)
                        speak("Volume decreased")
                        statusMessage = "Volume down"
                    }
                    input.contains("max") || input.contains("maximum") || input.contains("full") -> {
                        setMaxVolume()
                        speak("Volume set to maximum")
                        statusMessage = "Volume max"
                    }
                    input.contains("mute") || input.contains("silent") || input.contains("off") -> {
                        muteVolume()
                        speak("Volume muted")
                        statusMessage = "Muted"
                    }
                    else -> {
                        val percentage = getCurrentVolumePercentage()
                        speak("Volume is at $percentage percent")
                        statusMessage = "Volume: $percentage%"
                    }
                }
            }
        })

        // ===== WIFI COMMANDS =====
        commandRegistry.register(object : Command(
            keywords = listOf("wifi", "wi-fi", "internet connection")
        ) {
            override fun execute(input: String) {
                when {
                    input.contains("on") || input.contains("enable") || input.contains("turn on") -> {
                        speak("Opening WiFi settings to enable")
                        openWifiSettings()
                        statusMessage = "WiFi settings opened"
                    }
                    input.contains("off") || input.contains("disable") || input.contains("turn off") -> {
                        speak("Opening WiFi settings to disable")
                        openWifiSettings()
                        statusMessage = "WiFi settings opened"
                    }
                    else -> {
                        val status = if (wifiManager.isWifiEnabled) "enabled" else "disabled"
                        speak("WiFi is currently $status")
                        statusMessage = "WiFi: $status"
                    }
                }
            }
        })

        // ===== BLUETOOTH COMMANDS =====
        commandRegistry.register(object : Command(
            keywords = listOf("bluetooth")
        ) {
            override fun execute(input: String) {
                speak("Opening Bluetooth settings")
                openBluetoothSettings()
                statusMessage = "Bluetooth settings"
            }
        })

        // ===== BRIGHTNESS COMMANDS =====
        commandRegistry.register(object : Command(
            keywords = listOf("brightness", "screen brightness", "display")
        ) {
            override fun execute(input: String) {
                speak("Opening display settings for brightness control")
                openDisplaySettings()
                statusMessage = "Display settings"
            }
        })

        // ===== PHONE CALL COMMANDS =====
        commandRegistry.register(object : Command(
            keywords = listOf("call", "dial", "phone")
        ) {
            override fun execute(input: String) {
                val contact = input.replace(Regex("(call|dial|phone)"), "").trim()
                if (contact.isNotEmpty() && contact.length > 2) {
                    // Check if input contains a phone number
                    val phoneNumber = extractPhoneNumber(contact)
                    if (phoneNumber != null) {
                        speak("Calling $phoneNumber")
                        makeCall(phoneNumber)
                        statusMessage = "Calling: $phoneNumber"
                    } else {
                        // Try to find contact by name
                        speak("Calling $contact")
                        val number = findContactNumber(contact)
                        if (number != null) {
                            makeCall(number)
                            statusMessage = "Calling: $contact"
                        } else {
                            speak("Contact $contact not found. Opening dialer.")
                            openDialer()
                            statusMessage = "Contact not found"
                        }
                    }
                } else {
                    speak("Opening phone dialer")
                    openDialer()
                    statusMessage = "Dialer"
                }
            }
        })

        // ===== MESSAGE COMMANDS =====
        commandRegistry.register(object : Command(
            keywords = listOf("message", "text", "sms")
        ) {
            override fun execute(input: String) {
                when {
                    input.contains("read") -> speak("Opening messages to read")
                    input.contains("send") -> speak("Opening messages to send")
                    else -> speak("Opening messages")
                }
                openMessaging()
                statusMessage = "Messages opened"
            }
        })

        // ===== CAMERA/VISION COMMANDS =====
        commandRegistry.register(object : Command(
            keywords = listOf("camera", "photo", "picture", "read this", "what is this", "identify", "scan")
        ) {
            override fun execute(input: String) {
                when {
                    input.contains("read") || input.contains("scan") ->
                        speak("Opening camera to read text")
                    input.contains("what") || input.contains("identify") ->
                        speak("Opening camera to identify objects")
                    else ->
                        speak("Opening camera")
                }
                openCamera()
                statusMessage = "Camera opened"
            }
        })

        // ===== NAVIGATION COMMANDS =====
        commandRegistry.register(object : Command(
            keywords = listOf("navigate", "directions", "where am i", "location", "maps")
        ) {
            override fun execute(input: String) {
                when {
                    input.contains("where am i") || input.contains("my location") -> {
                        speak("Getting your current location")
                        statusMessage = "Location services"
                    }
                    else -> {
                        speak("Opening navigation")
                        statusMessage = "Navigation"
                    }
                }
            }
        })

        // ===== APP OPENING COMMANDS =====
        commandRegistry.register(object : Command(
            keywords = listOf("open", "launch", "start")
        ) {
            override fun execute(input: String) {
                val appName = input.replace(Regex("(open|launch|start)"), "").trim()

                when {
                    appName.contains("settings") || appName.contains("setting") -> {
                        speak("Opening settings")
                        openSettings()
                        statusMessage = "Settings"
                    }
                    appName.contains("contact") -> {
                        speak("Opening contacts")
                        openContacts()
                        statusMessage = "Contacts"
                    }
                    appName.isNotEmpty() && appName.length > 2 -> {
                        speak("Opening $appName")
                        statusMessage = "Opening: $appName"
                    }
                    else -> {
                        speak("Which app would you like to open?")
                    }
                }
            }
        })

        // ===== CALENDAR COMMANDS =====
        commandRegistry.register(object : Command(
            keywords = listOf("calendar", "schedule", "appointment", "meeting", "event")
        ) {
            override fun execute(input: String) {
                speak("Opening calendar")
                openCalendar()
                statusMessage = "Calendar"
            }
        })

        // ===== ALARM COMMANDS =====
        commandRegistry.register(object : Command(
            keywords = listOf("alarm", "wake me", "set alarm", "timer")
        ) {
            override fun execute(input: String) {
                speak("Opening alarm settings")
                openAlarmSettings()
                statusMessage = "Alarms"
            }
        })

        // ===== EMERGENCY COMMANDS =====
        commandRegistry.register(object : Command(
            keywords = listOf("emergency", "help", "sos", "danger")
        ) {
            override fun execute(input: String) {
                handleEmergency()
            }
        })

        // ===== SYSTEM INFO COMMANDS =====
        commandRegistry.register(object : Command(
            keywords = listOf("repeat", "say again", "what")
        ) {
            override fun execute(input: String) {
                if (lastCommand.isNotEmpty()) {
                    speak("I heard: $lastCommand")
                } else {
                    speak("No previous command to repeat")
                }
            }
        })
    }

    // ==================== SYSTEM METHODS ====================

    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isCharging(): Boolean {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun adjustVolume(direction: Int) {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    private fun setMaxVolume() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_SHOW_UI)
    }

    private fun muteVolume() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
    }

    private fun getCurrentVolumePercentage(): Int {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (maxVolume > 0) (currentVolume * 100 / maxVolume) else 0
    }

    // ==================== PHONE CALL HELPERS ====================

    private fun extractPhoneNumber(input: String): String? {
        // Remove common words and spaces
        val cleaned = input.replace(Regex("[^0-9+]"), "")
        // Check if it looks like a phone number (at least 7 digits)
        return if (cleaned.length >= 7) cleaned else null
    }

    private fun findContactNumber(name: String): String? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val cursor = contentResolver.query(
            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),
            "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val numberIndex = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex >= 0) {
                    return it.getString(numberIndex)
                }
            }
        }
        return null
    }

    private fun makeCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = "tel:$phoneNumber".toUri()
            }
            startActivity(intent)
        } else {
            // Fallback to dialer if permission not granted
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = "tel:$phoneNumber".toUri()
            }
            startActivity(intent)
        }
    }

    // ==================== INTENT LAUNCHERS ====================

    private fun openWifiSettings() {
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    private fun openBluetoothSettings() {
        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    private fun openDisplaySettings() {
        startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
    }

    private fun openDialer() {
        val intent = Intent(Intent.ACTION_DIAL)
        startActivity(intent)
    }

    private fun openMessaging() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
        }
        startActivity(intent)
    }

    private fun openCamera() {
        val intent = Intent("android.media.action.IMAGE_CAPTURE")
        startActivity(intent)
    }

    private fun openCalendar() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALENDAR)
        }
        startActivity(intent)
    }

    private fun openAlarmSettings() {
        startActivity(Intent(android.provider.AlarmClock.ACTION_SET_ALARM))
    }

    private fun openSettings() {
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }

    private fun openContacts() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.provider.ContactsContract.Contacts.CONTENT_URI
        }
        startActivity(intent)
    }

    private fun handleEmergency() {
        val emergencyNumber = prefs.getString("emergency_number", "911")
        speak("Calling emergency contact")
        statusMessage = "Emergency"

        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = "tel:$emergencyNumber".toUri()
        }
        startActivity(intent)
    }

    // ==================== VOLUME BUTTON HANDLING ====================

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && event?.repeatCount == 0) {
            isVolumeButtonPressed = true
            buttonPressStartTime = System.currentTimeMillis()
            capturedCommand = null

            handler.postDelayed({
                if (isVolumeButtonPressed) {
                    startListening()
                }
            }, longPressThreshold)

            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val pressDuration = System.currentTimeMillis() - buttonPressStartTime
            isVolumeButtonPressed = false
            handler.removeCallbacksAndMessages(null)

            if (pressDuration >= longPressThreshold && isListening) {
                stopListening()
                statusMessage = "Processing..."

                handler.postDelayed({
                    capturedCommand?.let { commandRegistry.execute(it) }
                }, 300)
            }

            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun startListening() {
        if (!isListening && SpeechRecognizer.isRecognitionAvailable(this)) {
            try {
                statusMessage = "Listening..."
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speechRecognizer.startListening(intent)
            } catch (e: Exception) {
                statusMessage = "Error: ${e.message}"
                isListening = false
            }
        }
    }

    private fun stopListening() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // ==================== UI COMPOSITION ====================

    @Composable
    fun MainScreen() {
        val scrollState = rememberScrollState()
        val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
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
                Spacer(modifier = Modifier.height(40.dp))

                // Logo/Title
                Text(
                    text = "ZIRA",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Voice Assistant",
                    fontSize = 18.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Microphone Indicator
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isListening) Color(0xFF2196F3) else Color(0xFF1A1A1A)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isListening) 12.dp else 4.dp
                    ),
                    modifier = Modifier.scale(if (isListening) scale else 1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎤",
                            fontSize = 72.sp,
                            color = if (isListening) Color.White else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Status Message
                Text(
                    text = statusMessage,
                    fontSize = 20.sp,
                    fontWeight = if (isListening) FontWeight.Bold else FontWeight.Normal,
                    color = if (isListening) Color(0xFF2196F3) else Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                if (lastCommand.isNotEmpty() && !isListening) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = lastCommand,
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Instructions Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "How to Use",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "1. Long press Volume Up button\n" +
                                    "2. Speak your command clearly\n" +
                                    "3. Release button when finished",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            lineHeight = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Available Commands Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Available Commands",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val commandGroups = listOf(
                            "⏰ Time & Date" to "\"What time is it?\" \"What's the date?\"",
                            "🔋 Battery" to "\"Battery level\" \"Am I charging?\"",
                            "🔊 Volume" to "\"Volume up\" \"Volume down\" \"Mute\"",
                            "📡 Connectivity" to "\"WiFi status\" \"Bluetooth settings\"",
                            "📞 Communication" to "\"Call [name]\" \"Open messages\"",
                            "📷 Camera" to "\"Open camera\" \"Read this text\"",
                            "🗺️ Navigation" to "\"Where am I?\" \"Navigate to [place]\"",
                            "📱 Apps" to "\"Open [app name]\" \"Launch contacts\"",
                            "📅 Calendar" to "\"Open calendar\" \"My schedule\"",
                            "⏰ Alarms" to "\"Set alarm\" \"Timer\"",
                            "🆘 Emergency" to "\"Emergency\" \"Help\""
                        )

                        commandGroups.forEach { (category, examples) ->
                            Text(
                                text = category,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = examples,
                                fontSize = 13.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        speechRecognizer.destroy()
        tts.shutdown()
        super.onDestroy()
    }
}