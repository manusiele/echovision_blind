package com.example.zira

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zira.ui.theme.ZiraTheme
import java.text.SimpleDateFormat
import java.util.*

// Constants
object Constants {
    const val EXTRA_COMMAND = "extra_command"
}


// Alarm Activity
class AlarmActivity : ComponentActivity() {
    private lateinit var command: String
    private lateinit var tts: TextToSpeech
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        command = intent.getStringExtra(Constants.EXTRA_COMMAND) ?: ""

        initializeTTS()

        setContent {
            ZiraTheme {
                AlarmScreen()
            }
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                speak("Alarm and timer settings")
            }
        }
    }

    @Composable
    fun AlarmScreen() {
        val alarms = remember { mutableStateOf(getDefaultAlarms()) }
        var showNewAlarmDialog by remember { mutableStateOf(false) }

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
                    text = "Alarms & Timers",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { showNewAlarmDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Alarm", tint = Color(0xFF2196F3))
                }
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

            // Alarms List
            if (alarms.value.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alarms.value) { alarm ->
                        AlarmCard(alarm) {
                            alarms.value = alarms.value.filter { it.id != alarm.id }
                            setAlarm(alarm)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No alarms set. Tap + to add one.",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // New Alarm Dialog
        if (showNewAlarmDialog) {
            NewAlarmDialog(
                onDismiss = { showNewAlarmDialog = false },
                onAddAlarm = { time, label ->
                    val newAlarm = Alarm(
                        id = System.currentTimeMillis().toInt(),
                        time = time,
                        label = label,
                        enabled = true
                    )
                    alarms.value = alarms.value + newAlarm
                    setAlarm(newAlarm)
                    showNewAlarmDialog = false
                }
            )
        }
    }

    @Composable
    fun AlarmCard(alarm: Alarm, onDelete: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDelete() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alarm.time,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                    Text(
                        text = alarm.label,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (alarm.enabled) "ON" else "OFF",
                        fontSize = 12.sp,
                        color = if (alarm.enabled) Color(0xFF4CAF50) else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun NewAlarmDialog(
        onDismiss: () -> Unit,
        onAddAlarm: (String, String) -> Unit
    ) {
        var selectedHour by remember { mutableStateOf(7) }
        var selectedMinute by remember { mutableStateOf(0) }
        var alarmLabel by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Set New Alarm", color = Color.White) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Time Pickers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour
                        NumberPicker(
                            value = selectedHour,
                            range = 0..23,
                            onValueChange = { selectedHour = it }
                        )
                        Text(
                            text = ":",
                            fontSize = 24.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        // Minute
                        NumberPicker(
                            value = selectedMinute,
                            range = 0..59,
                            onValueChange = { selectedMinute = it }
                        )
                    }

                    // Label TextField
                    TextField(
                        value = alarmLabel,
                        onValueChange = { alarmLabel = it },
                        label = { Text("Alarm Label", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2A2A2A),
                            unfocusedContainerColor = Color(0xFF2A2A2A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val timeStr = String.format("%02d:%02d", selectedHour, selectedMinute)
                        onAddAlarm(timeStr, alarmLabel.ifEmpty { "Alarm" })
                        speak("Alarm set for $timeStr")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("Set Alarm", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    @Composable
    fun NumberPicker(
        value: Int,
        range: IntRange,
        onValueChange: (Int) -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { if (value < range.last) onValueChange(value + 1) },
                modifier = Modifier.size(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("▲", color = Color.White)
            }
            Text(
                text = String.format("%02d", value),
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Button(
                onClick = { if (value > range.first) onValueChange(value - 1) },
                modifier = Modifier.size(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("▼", color = Color.White)
            }
        }
    }

    private fun getDefaultAlarms(): List<Alarm> {
        return listOf(
            Alarm(1, "6:30 AM", "Wake Up", true),
            Alarm(2, "9:00 AM", "Meeting", false),
            Alarm(3, "5:00 PM", "Workout", true)
        )
    }

    private fun setAlarm(alarm: Alarm) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, AlarmReceiver::class.java).apply {
                putExtra("alarm_label", alarm.label)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                alarm.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                val parts = alarm.time.split(":")
                set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(Calendar.MINUTE, parts[1].toInt())
                set(Calendar.SECOND, 0)
            }

            if (alarm.enabled) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            speak("Could not set alarm: ${e.message}")
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    data class Alarm(
        val id: Int,
        val time: String,
        val label: String,
        val enabled: Boolean
    )

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}

