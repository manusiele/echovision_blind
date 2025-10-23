package com.example.zira

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.zira.ui.theme.ZiraTheme
import java.util.*

// ==================== DATABASE HELPER ====================
class AlarmDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ZiraAlarms.db"
        private const val DATABASE_VERSION = 1

        // Table name
        private const val TABLE_ALARMS = "alarms"

        // Column names
        private const val COLUMN_ID = "id"
        private const val COLUMN_TIME = "time"
        private const val COLUMN_LABEL = "label"
        private const val COLUMN_ENABLED = "enabled"
        private const val COLUMN_REPEAT_DAYS = "repeat_days"
        private const val COLUMN_VIBRATE = "vibrate"
        private const val COLUMN_RINGTONE_URI = "ringtone_uri"
        private const val COLUMN_VOLUME = "volume"
        private const val COLUMN_SNOOZE_ENABLED = "snooze_enabled"
        private const val COLUMN_SNOOZE_DURATION = "snooze_duration"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_LAST_TRIGGERED = "last_triggered"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = """
            CREATE TABLE $TABLE_ALARMS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TIME TEXT NOT NULL,
                $COLUMN_LABEL TEXT NOT NULL,
                $COLUMN_ENABLED INTEGER NOT NULL DEFAULT 1,
                $COLUMN_REPEAT_DAYS TEXT DEFAULT 'NONE',
                $COLUMN_VIBRATE INTEGER DEFAULT 1,
                $COLUMN_RINGTONE_URI TEXT,
                $COLUMN_VOLUME INTEGER DEFAULT 70,
                $COLUMN_SNOOZE_ENABLED INTEGER DEFAULT 1,
                $COLUMN_SNOOZE_DURATION INTEGER DEFAULT 5,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_LAST_TRIGGERED INTEGER DEFAULT 0
            )
        """.trimIndent()

        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_ALARMS")
        onCreate(db)
    }

    // Insert new alarm
    fun insertAlarm(alarm: Alarm): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TIME, alarm.time)
            put(COLUMN_LABEL, alarm.label)
            put(COLUMN_ENABLED, if (alarm.enabled) 1 else 0)
            put(COLUMN_REPEAT_DAYS, alarm.repeatDays)
            put(COLUMN_VIBRATE, if (alarm.vibrate) 1 else 0)
            put(COLUMN_RINGTONE_URI, alarm.ringtoneUri)
            put(COLUMN_VOLUME, alarm.volume)
            put(COLUMN_SNOOZE_ENABLED, if (alarm.snoozeEnabled) 1 else 0)
            put(COLUMN_SNOOZE_DURATION, alarm.snoozeDuration)
            put(COLUMN_CREATED_AT, alarm.createdAt)
            put(COLUMN_LAST_TRIGGERED, alarm.lastTriggered)
        }

        return db.insert(TABLE_ALARMS, null, values)
    }

    // Get all alarms
    fun getAllAlarms(): List<Alarm> {
        val alarms = mutableListOf<Alarm>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ALARMS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_CREATED_AT DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val alarm = Alarm(
                    id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID)),
                    time = it.getString(it.getColumnIndexOrThrow(COLUMN_TIME)),
                    label = it.getString(it.getColumnIndexOrThrow(COLUMN_LABEL)),
                    enabled = it.getInt(it.getColumnIndexOrThrow(COLUMN_ENABLED)) == 1,
                    repeatDays = it.getString(it.getColumnIndexOrThrow(COLUMN_REPEAT_DAYS)),
                    vibrate = it.getInt(it.getColumnIndexOrThrow(COLUMN_VIBRATE)) == 1,
                    ringtoneUri = it.getString(it.getColumnIndexOrThrow(COLUMN_RINGTONE_URI)) ?: "",
                    volume = it.getInt(it.getColumnIndexOrThrow(COLUMN_VOLUME)),
                    snoozeEnabled = it.getInt(it.getColumnIndexOrThrow(COLUMN_SNOOZE_ENABLED)) == 1,
                    snoozeDuration = it.getInt(it.getColumnIndexOrThrow(COLUMN_SNOOZE_DURATION)),
                    createdAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                    lastTriggered = it.getLong(it.getColumnIndexOrThrow(COLUMN_LAST_TRIGGERED))
                )
                alarms.add(alarm)
            }
        }
        return alarms
    }

    // Update alarm - removed unused function, use toggleAlarm or specific updates instead

    // Delete alarm
    fun deleteAlarm(alarmId: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_ALARMS, "$COLUMN_ID = ?", arrayOf(alarmId.toString()))
    }

    // Toggle alarm enabled state
    fun toggleAlarm(alarmId: Int, enabled: Boolean): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ENABLED, if (enabled) 1 else 0)
        }
        return db.update(TABLE_ALARMS, values, "$COLUMN_ID = ?", arrayOf(alarmId.toString()))
    }
}

// ==================== DATA MODEL ====================
data class Alarm(
    val id: Int = 0,
    val time: String,
    val label: String,
    val enabled: Boolean = true,
    val repeatDays: String = "NONE", // "NONE", "EVERYDAY", "MON,TUE,WED,THU,FRI", etc.
    val vibrate: Boolean = true,
    val ringtoneUri: String = "",
    val volume: Int = 70,
    val snoozeEnabled: Boolean = true,
    val snoozeDuration: Int = 5,
    val createdAt: Long = System.currentTimeMillis(),
    val lastTriggered: Long = 0
)

// ==================== ALARM ACTIVITY ====================
class AlarmActivity : ComponentActivity() {
    private lateinit var command: String
    private lateinit var tts: TextToSpeech
    private lateinit var dbHelper: AlarmDatabaseHelper

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        command = intent.getStringExtra("extra_command") ?: ""

        // Initialize database
        dbHelper = AlarmDatabaseHelper(this)

        // Check for required permissions
        checkAndRequestPermissions()

        initializeTTS()

        setContent {
            ZiraTheme {
                AlarmScreen()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.SCHEDULE_EXACT_ALARM)
            }

            if (permissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
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
        var alarms by remember { mutableStateOf(loadAlarmsFromDatabase()) }
        var showNewAlarmDialog by remember { mutableStateOf(false) }

        // Reload alarms when they change
        LaunchedEffect(Unit) {
            alarms = loadAlarmsFromDatabase()
        }

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
                IconButton(onClick = {
                    speak("Closing alarms")
                    finish()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Alarms & Timers",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    speak("Add new alarm")
                    showNewAlarmDialog = true
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Alarm", tint = Color(0xFF2196F3))
                }
            }

            // Command Info
            if (command.isNotEmpty()) {
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
            }

            // Alarms List
            if (alarms.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onToggle = {
                                val newEnabled = !alarm.enabled
                                dbHelper.toggleAlarm(alarm.id, newEnabled)
                                alarms = loadAlarmsFromDatabase()

                                if (newEnabled) {
                                    scheduleAlarm(alarm.copy(enabled = true))
                                    speak("Alarm enabled for ${alarm.time}")
                                } else {
                                    cancelAlarm(alarm.id)
                                    speak("Alarm disabled")
                                }
                            },
                            onDelete = {
                                speak("Deleting alarm for ${alarm.time}")
                                dbHelper.deleteAlarm(alarm.id)
                                cancelAlarm(alarm.id)
                                alarms = loadAlarmsFromDatabase()
                            }
                        )
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
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // New Alarm Dialog
        if (showNewAlarmDialog) {
            NewAlarmDialog(
                onDismiss = {
                    speak("Cancelled")
                    showNewAlarmDialog = false
                },
                onAddAlarm = { time, label, repeatDays ->
                    val newAlarm = Alarm(
                        time = time,
                        label = label,
                        enabled = true,
                        repeatDays = repeatDays,
                        vibrate = true,
                        volume = 70,
                        snoozeEnabled = true,
                        snoozeDuration = 5,
                        createdAt = System.currentTimeMillis()
                    )

                    val id = dbHelper.insertAlarm(newAlarm)
                    alarms = loadAlarmsFromDatabase()

                    val savedAlarm = alarms.find { it.id == id.toInt() }
                    if (savedAlarm != null) {
                        scheduleAlarm(savedAlarm)
                    }

                    val repeatText = when (repeatDays) {
                        "EVERYDAY" -> "everyday"
                        "NONE" -> "once"
                        else -> "on selected days"
                    }
                    speak("Alarm set for $time $repeatText")
                    showNewAlarmDialog = false
                }
            )
        }
    }

    @Composable
    fun AlarmCard(
        alarm: Alarm,
        onToggle: () -> Unit,
        onDelete: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (alarm.enabled) Color(0xFF1A1A1A) else Color(0xFF0D0D0D)
            ),
            shape = RoundedCornerShape(12.dp)
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
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alarm.enabled) Color(0xFF2196F3) else Color.Gray
                    )
                    Text(
                        text = alarm.label,
                        fontSize = 16.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (alarm.repeatDays != "NONE") {
                        Text(
                            text = getRepeatText(alarm.repeatDays),
                            fontSize = 14.sp,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toggle Switch
                    Switch(
                        checked = alarm.enabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2196F3),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF424242)
                        )
                    )

                    // Delete Button
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun NewAlarmDialog(
        onDismiss: () -> Unit,
        onAddAlarm: (String, String, String) -> Unit
    ) {
        var selectedHour by remember { mutableIntStateOf(7) }
        var selectedMinute by remember { mutableIntStateOf(0) }
        var alarmLabel by remember { mutableStateOf("") }
        var repeatType by remember { mutableStateOf("NONE") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Set New Alarm", color = Color.White, fontSize = 20.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    // Time Pickers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NumberPicker(
                            value = selectedHour,
                            range = 0..23,
                            onValueChange = { selectedHour = it }
                        )
                        Text(
                            text = ":",
                            fontSize = 32.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        NumberPicker(
                            value = selectedMinute,
                            range = 0..59,
                            onValueChange = { selectedMinute = it }
                        )
                    }

                    // Label TextField
                    OutlinedTextField(
                        value = alarmLabel,
                        onValueChange = { alarmLabel = it },
                        label = { Text("Label (optional)", color = Color.Gray) },
                        placeholder = { Text("e.g., Wake Up", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    // Repeat Options
                    Text(
                        text = "Repeat",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        RepeatOption("Once", "NONE", repeatType) { repeatType = it }
                        RepeatOption("Everyday", "EVERYDAY", repeatType) { repeatType = it }
                        RepeatOption("Weekdays", "MON,TUE,WED,THU,FRI", repeatType) { repeatType = it }
                        RepeatOption("Weekends", "SAT,SUN", repeatType) { repeatType = it }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val timeStr = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)
                        val label = alarmLabel.ifEmpty { "Alarm" }
                        onAddAlarm(timeStr, label, repeatType)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("Set Alarm", fontSize = 16.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.Gray, fontSize = 16.sp)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    @Composable
    fun RepeatOption(
        label: String,
        value: String,
        currentValue: String,
        onSelect: (String) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(value) }
                .background(
                    if (currentValue == value) Color(0xFF2196F3).copy(alpha = 0.2f)
                    else Color.Transparent,
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentValue == value,
                onClick = { onSelect(value) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF2196F3),
                    unselectedColor = Color.Gray
                )
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
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
            IconButton(
                onClick = { if (value < range.last) onValueChange(value + 1) }
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, "Increase", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Text(
                text = String.format(Locale.US, "%02d", value),
                fontSize = 28.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { if (value > range.first) onValueChange(value - 1) }
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, "Decrease", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }

    private fun loadAlarmsFromDatabase(): List<Alarm> {
        return dbHelper.getAllAlarms()
    }

    private fun scheduleAlarm(alarm: Alarm) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarm.id)
                putExtra("alarm_label", alarm.label)
                putExtra("alarm_time", alarm.time)
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
                set(Calendar.MILLISECOND, 0)

                // If time has passed today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            if (alarm.enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        if (alarm.repeatDays == "EVERYDAY") {
                            alarmManager.setRepeating(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                AlarmManager.INTERVAL_DAY,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent
                            )
                        }
                    }
                } else {
                    if (alarm.repeatDays == "EVERYDAY") {
                        alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            AlarmManager.INTERVAL_DAY,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                }
            }
        } catch (exception: Exception) {
            speak("Could not set alarm: ${exception.message}")
        }
    }

    private fun cancelAlarm(alarmId: Int) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (exception: Exception) {
            // Silent fail - intentionally unused parameter
        }
    }

    private fun getRepeatText(repeatDays: String): String {
        return when (repeatDays) {
            "EVERYDAY" -> "Everyday"
            "MON,TUE,WED,THU,FRI" -> "Weekdays"
            "SAT,SUN" -> "Weekends"
            "NONE" -> ""
            else -> "Custom"
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        tts.shutdown()
        dbHelper.close()
        super.onDestroy()
    }
}

// ==================== ALARM RECEIVER ====================
class AlarmReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        val alarmId = intent?.getIntExtra("alarm_id", -1) ?: -1
        val label = intent?.getStringExtra("alarm_label") ?: "Alarm"
        val time = intent?.getStringExtra("alarm_time") ?: ""

        // Launch AlarmRingingActivity
        val ringingIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("alarm_id", alarmId)
            putExtra("alarm_label", label)
            putExtra("alarm_time", time)
        }
        context.startActivity(ringingIntent)
    }
}

// ==================== ALARM RINGING ACTIVITY ====================
class AlarmRingingActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech
    private lateinit var ringtone: android.media.Ringtone
    private var vibrator: Vibrator? = null
    private var alarmLabel = ""
    private var alarmTime = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get alarm details
        alarmLabel = intent.getStringExtra("alarm_label") ?: "Alarm"
        alarmTime = intent.getStringExtra("alarm_time") ?: ""

        // Turn screen on and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Initialize TTS
        initializeTTS()

        // Start ringtone
        startRingtone()

        // Start vibration
        startVibration()

        setContent {
            ZiraTheme {
                AlarmRingingScreen()
            }
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                // Speak alarm name
                speak("Alarm! $alarmLabel")
            }
        }
    }

    private fun startRingtone() {
        try {
            // Get default alarm ringtone
            val alarmUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            ringtone = android.media.RingtoneManager.getRingtone(this, alarmUri)
            ringtone.play()
        } catch (exception: Exception) {
            // If alarm ringtone fails, try notification sound
            try {
                val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                ringtone = android.media.RingtoneManager.getRingtone(this, notificationUri)
                ringtone.play()
            } catch (exception2: Exception) {
                // Silent fail - intentionally unused parameter
            }
        }
    }

    private fun startVibration() {
        try {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

            // Vibration pattern: wait 0ms, vibrate 1000ms, wait 1000ms, repeat
            val pattern = longArrayOf(0, 1000, 1000)

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(
                        VibrationEffect.createWaveform(pattern, 0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(pattern, 0)
                }
            }
        } catch (exception: Exception) {
            // Silent fail if vibration not supported - intentionally unused parameter
        }
    }

    @Composable
    fun AlarmRingingScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                // Alarm Icon
                Text(
                    text = "⏰",
                    fontSize = 120.sp,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Time
                Text(
                    text = alarmTime,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Label
                Text(
                    text = alarmLabel,
                    fontSize = 32.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(64.dp))

                // Dismiss Button
                Button(
                    onClick = { dismissAlarm() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "DISMISS",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Snooze Button
                Button(
                    onClick = { snoozeAlarm() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFA726)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "SNOOZE (5 min)",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    private fun dismissAlarm() {
        speak("Alarm dismissed")
        stopAlarm()
        finish()
    }

    private fun snoozeAlarm() {
        speak("Alarm snoozed for 5 minutes")

        // Schedule alarm again after 5 minutes
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", getIntent().getIntExtra("alarm_id", -1))
            putExtra("alarm_label", alarmLabel)
            putExtra("alarm_time", alarmTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(), // Unique ID for snooze
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeTime = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                snoozeTime,
                pendingIntent
            )
        }

        stopAlarm()
        finish()
    }

    private fun stopAlarm() {
        try {
            if (::ringtone.isInitialized && ringtone.isPlaying) {
                ringtone.stop()
            }
        } catch (exception: Exception) {
            // Silent fail - intentionally unused parameter
        }

        try {
            vibrator?.cancel()
        } catch (exception: Exception) {
            // Silent fail - intentionally unused parameter
        }
    }

    private fun speak(text: String) {
        if (::tts.isInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onDestroy() {
        stopAlarm()
        if (::tts.isInitialized) {
            tts.shutdown()
        }
        super.onDestroy()
    }
}