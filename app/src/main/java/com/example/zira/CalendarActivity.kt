package com.example.zira

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
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

class CalendarActivity : ComponentActivity() {
    private lateinit var command: String
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        command = intent.getStringExtra(MainActivity.EXTRA_COMMAND) ?: ""

        initializeTTS()

        setContent {
            ZiraTheme {
                CalendarScreen()
            }
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                speak("Showing your calendar")
            }
        }
    }

    @Composable
    fun CalendarScreen() {
        val currentDate = remember { Calendar.getInstance() }
        val monthYear = remember(currentDate) {
            SimpleDateFormat("MMMM yyyy", Locale.US).format(currentDate.time)
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
                IconButton(onClick = { finish() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Calendar",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { openCalendarApp() }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Event", tint = Color(0xFF2196F3))
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

            // Current Month/Year
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = monthYear,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US).format(currentDate.time),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            // Quick Actions
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(getQuickActions()) { action ->
                    QuickActionCard(action)
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    @Composable
    fun QuickActionCard(action: CalendarAction) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { performAction(action) },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = action.emoji + " " + action.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = action.description,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Text(
                    text = "→",
                    fontSize = 20.sp,
                    color = Color(0xFF2196F3)
                )
            }
        }
    }

    private fun getQuickActions(): List<CalendarAction> {
        return listOf(
            CalendarAction("📅 View Calendar", "Open full calendar", "calendar"),
            CalendarAction("➕ New Event", "Add new event", "new_event"),
            CalendarAction("🔍 Search Events", "Search your events", "search"),
            CalendarAction("📌 Today's Events", "Show today's schedule", "today")
        )
    }

    private fun performAction(action: CalendarAction) {
        when (action.action) {
            "calendar" -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = CalendarContract.CONTENT_URI
                }
                startActivity(intent)
            }
            "new_event" -> openCalendarApp()
            "search" -> speak("Event search not yet available in voice mode")
            "today" -> speak("Showing today's events")
        }
    }

    private fun openCalendarApp() {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
            }
            startActivity(intent)
        } catch (e: Exception) {
            speak("Calendar app not available")
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    data class CalendarAction(
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