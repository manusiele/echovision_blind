package com.example.zira

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zira.ui.theme.ZiraTheme
import kotlinx.coroutines.delay

class EmergencyContactActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech
    private var selectedContact by mutableStateOf("")
    private var selectedNumber by mutableStateOf("")

    private val contactPicker = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let { processContactSelection(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts.language = java.util.Locale.US
        }

        setContent {
            ZiraTheme {
                EmergencyContactScreen()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (selectedContact.isNotEmpty()) {
                    completeSetup()
                } else {
                    speak("Please select an emergency contact first.")
                }
            }
        })
    }

    @Composable
    fun EmergencyContactScreen() {
        LaunchedEffect(Unit) {
            speak("Emergency contact setup. Who should I call in emergencies?")
            delay(1500)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Step 4 of 5",
                    color = Color.Gray,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Emergency Contact",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Who should Zira call if you say 'Emergency!'?",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Selected Contact Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFF2196F3)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedContact.isEmpty()) "No contact selected" else selectedContact,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedContact.isEmpty()) Color.Gray else Color.White
                            )
                            Text(
                                text = selectedNumber,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Pick Contact Button
                Button(
                    onClick = { contactPicker.launch(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Contact", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Manual Entry Button
                OutlinedButton(
                    onClick = {
                        speak("Manual entry coming soon. Please select from contacts.")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enter Number Manually")
                }

                Spacer(modifier = Modifier.weight(1f))

                // Progress
                LinearProgressIndicator(
                    progress = { 0.8f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF2196F3)
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    private fun processContactSelection(uri: Uri) {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameColumn = it.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME)
                selectedContact = it.getString(nameColumn)

                val contactIdColumn = it.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
                val contactId = it.getString(contactIdColumn)

                val phoneCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )
                phoneCursor?.use { phone ->
                    if (phone.moveToFirst()) {
                        val numberColumn = phone.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        selectedNumber = phone.getString(numberColumn)
                    }
                }
            }
        }
        speak("Selected: $selectedContact")
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun completeSetup() {
        val prefs = getSharedPreferences("ZiraPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("emergency_contact", selectedContact)
            .putString("emergency_number", selectedNumber)
            .putBoolean("onboarding_completed", true)
            .apply()

        speak("Emergency contact saved! Moving to activation test.")
        val intent = Intent(this, ActivationTestActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}