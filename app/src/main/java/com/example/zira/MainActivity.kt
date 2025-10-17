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
import androidx.compose.foundation.shape.CircleShape
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
    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListening by mutableStateOf(false)
    private var statusMessage by mutableStateOf("Ready - Long press Volume Up to speak")
    private var lastCommand by mutableStateOf("")
    private val prefs by lazy { getSharedPreferences("ZiraPrefs", Context.MODE_PRIVATE) }
    private lateinit var audioManager: AudioManager
    private lateinit var wifiManager: WifiManager

    private var buttonPressStartTime = 0L
    private val LONG_PRESS_THRESHOLD = 500L
    private val handler = Handler(Looper.getMainLooper())
    private var isVolumeButtonPressed = false
    private var capturedCommand: String? = null

    // Command handler interface for extensibility
    private interface CommandHandler {
        fun canHandle(command: String): Boolean
        fun execute(command: String)
    }

    private val commandHandlers = mutableListOf<CommandHandler>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        initializeTTS()
        initializeSpeechRecognizer()
        registerCommandHandlers()

        setContent {
            ZiraTheme {
                MainScreen()
            }
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                speak("Zira is ready. Long press Volume Up to activate.")
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

            override fun onEndOfSpeech() {
                // Keep isListening true until user releases button or timeout
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    else -> "Recognition error"
                }
                statusMessage = "Error: $errorMessage"
                speak("Sorry, I didn't catch that.")
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val command = matches?.firstOrNull()

                if (command != null) {
                    capturedCommand = command
                    lastCommand = "You said: \"$command\""
                    statusMessage = lastCommand
                } else {
                    statusMessage = "No command detected"
                    speak("I didn't hear anything.")
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

    private fun registerCommandHandlers() {
        // Time Commands
        commandHandlers.add(object : CommandHandler {
            override fun canHandle(command: String) = command.contains("time")
            override fun execute(command: String) {
                val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                speak("The time is $time")
                statusMessage = "Current time: $time"
            }
        })

        // Date Commands
        commandHandlers.add(object : CommandHandler {
            override fun canHandle(command: String) =
                command.contains("date") || command.contains("day") || command.contains("today")
            override fun execute(command: String) {
                val date = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
                speak("Today is $date")
                statusMessage = "Date: $date"
            }
        })

        // Battery Commands
        commandHandlers.add(object : CommandHandler {
            override fun canHandle(command: String) =
                command.contains("battery") || command.contains("charge")
            override fun execute(command: String) {
                val batteryLevel = getBatteryLevel()
                val status = if (isCharging()) "and charging" else ""
                speak("Battery is at $batteryLevel percent $status")
                statusMessage = "Battery: $batteryLevel% $status"
            }
        })

        // Volume Commands
        commandHandlers.add(object : CommandHandler {
            override fun canHandle(command: String) =
                command.contains("volume") || command.contains("sound")
            override fun execute(command: String) {
                when {
                    command.contains("up") || command.contains("increase") || command.contains("louder") -> {
                        adjustVolume(AudioManager.ADJUST_RAISE)
                        speak("Volume increased")
                    }
                    command.contains("down") || command.contains("decrease") || command.contains("lower") -> {
                        adjustVolume(AudioManager.ADJUST_LOWER)
                        speak("Volume decreased")
                    }
                    command.contains("max") || command.contains("maximum") -> {
                        setMaxVolume()
                        speak("Volume set to maximum")
                    }
                    command.contains("mute") || command.contains("silent") -> {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                        speak("Volume muted")
                    }
                    else -> {
                        val currentVolume = getCurrentVolumePercentage()
                        speak("Volume is at $currentVolume percent")
                        statusMessage = "Volume: $currentVolume%"
                    }
                }
            }
        })

        // WiFi Commands
        commandHandlers.add(object : CommandHandler {
            override fun canHandle(command: String) =
                command.contains("wifi") || command.contains("wi-fi")
            override fun execute(command: String) {
                when {
                    command.contains("on") || command.contains("enable") -> {
                        speak("Opening WiFi settings. Please enable WiFi manually.")
                        openWifiSettings()
                    }
                    command.contains("off") || command.contains("disable") -> {
                        speak("Opening WiFi settings. Please disable WiFi manually.")
                        openWifiSettings()
                    }
                    else -> {
                        val status = if (wifiManager.isWifiEnabled) "enabled" else "disabled"
                        speak("WiFi is currently $status")
                        statusMessage = "WiFi: $status"
                    }
                }
            }
        })

        // Bluetooth Commands
        commandHandlers.add(object : CommandHandler {
            override fun canHandle(command: String) = command.contains("bluetooth")
            override fun execute(command: String) {
                speak("Opening Bluetooth settings")
                openBluetoothSettings()
                statusMessage = "Opening Bluetooth settings"
            }
        })

        // Brightness Commands
        commandHandlers.add(object : CommandHandler {
            override fun canHandle(command: String) =
                command.contains("brightness") || command.contains("screen")
            override fun execute(command: String) {
                speak("Opening display settings for brightness control")
                openDisplaySettings()
                statusMessage = "Opening brightness settings"
            }
        })

        // Phone Call Commands
        commandHandlers.add(object : CommandHandler {
            override fun canHandle(command: String) =
                command.contains("call") || command.contains("dial")
            override fun execute(command: String) {
                val contact = command.replace(Regex("(call|dial)"), "").trim()
                if (contact.isNotEmpty()) {
                    speak("Opening dialer for $contact")
                    statusMessage = "Calling: $contact"
                    openDialer(contact)
                } else {
                    speak("Who would you like to call?")
                }
            }
        })

        // Message Commands
        commandHandlers.add(object : CommandHandler {
            override fun canHandle(command: String) =
                command.contains("message") || command.contains("text") || command.contains("sms")
            override fun execute(command: String) {
                when {
                    command.contains("read") -> {
                        speak("Opening messages to read")
                        statusMessage = "Reading messages..."
                    }
                    command.contains("send") -> {
                        speak("Opening messages to send")
                        statusMessage = "Ready to send message"
                    }
                    else -> {
                        speak("Opening messages")
                        openMessaging()
                    }
                }
            }
        })

        // Camera Commands
        commandHandlers.add(object : CommandHandler {
            override fun canHandle(command: String) =
                command.contains("camera") || command.contains("photo") ||
                        command.contains("picture") || command.contains("read this") ||
                        command.contains("what is this") || command.contains("identify")
            override fun execute(command: String) {
                speak("Opening camera")
                statusMessage = "Opening camera..."
                openCamera()
            }
        })

        // Navigation Commands
        commandHandlers.add(object : CommandHandler {
            override fun canHandle(command: String) =
                command.contains("navigate") || command.contains("directions") ||
                        command.contains("where am i") || command.contains("location")
            override fun execute(command: String) {
                when {
                    command.contains("where am i") || command.contains("location") -> {
                        speak("Opening location services")
                        statusMessage = "Getting location..."
                    }
                    else -> {
                        val destination = command.replace(Regex("(navigate|directions|to)"), "").trim()
                        speak("Opening navigation for $destination")
                        statusMessage = "Navigating to: $destination"
                    }
                }
            }
        })

        // App Opening Commands
        commandHandlers.add(object : CommandHandler {
            override fun canHandle(command: String) =
                command.contains("open") || command.contains("launch") || command.contains("start")
            override fun execute(command: String) {
                val appName = command.replace(Regex("(open|launch|start)"), "").trim()
                if (appName.isNotEmpty()) {
                    speak("Opening $appName")
                    statusMessage = "Opening: $appName"
                    openApp(appName)
                } else {
                    speak("Which app would you like to open?")
                }
            }
        })

        // Emergency Commands
        commandHandlers.add(object : CommandHandler {
            override fun canHandle(command: String) =
                command.contains("emergency") || command.contains("help") || command.contains("sos")
            override fun execute(command: String) {
                handleEmergency()
            }
        })

        // Calendar Commands
        commandHandlers.add(object : CommandHandler {
            override fun canHandle(command: String) =
                command.contains("calendar") || command.contains("schedule") ||
                        command.contains("appointment") || command.contains("meeting")
            override fun execute(command: String) {
                speak("Opening calendar")
                statusMessage = "Opening calendar..."
                openCalendar()
            }
        })

        // Alarm Commands
        commandHandlers.add(object : CommandHandler {
            override fun canHandle(command: String) =
                command.contains("alarm") || command.contains("wake me")
            override fun execute(command: String) {
                speak("Opening alarm settings")
                openAlarmSettings()
                statusMessage = "Opening alarms"
            }
        })

        // Flashlight Commands
        commandHandlers.add(object : CommandHandler {
            override fun canHandle(command: String) =
                command.contains("flashlight") || command.contains("torch") || command.contains("light")
            override fun execute(command: String) {
                speak("Flashlight control")
                statusMessage = "Flashlight toggled"
            }
        })
    }

    private fun processCommand(command: String) {
        val lowerCommand = command.lowercase().trim()

        val handler = commandHandlers.find { it.canHandle(lowerCommand) }

        if (handler != null) {
            handler.execute(lowerCommand)
        } else {
            speak("I heard: $command. But I don't understand that command yet.")
            statusMessage = "Unknown command: \"$command\""
        }

        this@MainActivity.handler.postDelayed({
            statusMessage = "Ready - Long press Volume Up to speak"
        }, 3000)
    }

    // System Information Methods
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

    private fun getCurrentVolumePercentage(): Int {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return (currentVolume * 100 / maxVolume)
    }

    // Intent Launchers
    private fun openWifiSettings() {
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    private fun openBluetoothSettings() {
        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    private fun openDisplaySettings() {
        startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
    }

    private fun openDialer(contact: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = "tel:".toUri()
        }
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
        startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
    }

    private fun openApp(appName: String) {
        speak("App launcher feature coming soon")
    }

    private fun handleEmergency() {
        val emergencyNumber = prefs.getString("emergency_number", "911")
        speak("Calling emergency contact at $emergencyNumber")
        statusMessage = "Emergency call initiated"

        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = "tel:$emergencyNumber".toUri()
        }
        startActivity(intent)
    }

    // Volume Button Handling
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && event?.repeatCount == 0) {
            isVolumeButtonPressed = true
            buttonPressStartTime = System.currentTimeMillis()
            capturedCommand = null

            handler.postDelayed({
                if (isVolumeButtonPressed) {
                    startListening()
                }
            }, LONG_PRESS_THRESHOLD)

            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val pressDuration = System.currentTimeMillis() - buttonPressStartTime
            isVolumeButtonPressed = false
            handler.removeCallbacksAndMessages(null)

            if (pressDuration >= LONG_PRESS_THRESHOLD && isListening) {
                stopListening()
                statusMessage = "Processing command..."

                handler.postDelayed({
                    capturedCommand?.let { processCommand(it) }
                }, 300)
            } else {
                statusMessage = "Ready - Long press Volume Up to speak"
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

    @Composable
    fun MainScreen() {
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ZIRA",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Voice Assistant for Everyone",
                    fontSize = 18.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(48.dp))

                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isListening) Color(0xFF2196F3) else Color(0xFF1A1A1A)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isListening) 8.dp else 2.dp
                    ),
                    modifier = Modifier.scale(if (isListening) scale else 1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎤",
                            fontSize = 64.sp,
                            color = if (isListening) Color.White else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = statusMessage,
                    fontSize = 18.sp,
                    color = if (isListening) Color(0xFF2196F3) else Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                if (lastCommand.isNotEmpty() && !isListening) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = lastCommand,
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Available Commands:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Time & Date • Battery Status\n" +
                                    "• Volume Control • WiFi/Bluetooth\n" +
                                    "• Phone Calls • Messages\n" +
                                    "• Camera/Scanner • Navigation\n" +
                                    "• Open Apps • Emergency Call",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            lineHeight = 20.sp
                        )
                    }
                }
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