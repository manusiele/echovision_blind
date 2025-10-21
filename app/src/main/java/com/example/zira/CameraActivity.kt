package com.example.zira

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
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
import androidx.compose.material.icons.filled.PhotoCamera
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

class CameraActivity : ComponentActivity() {
    private lateinit var command: String
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        command = intent.getStringExtra(MainActivity.EXTRA_COMMAND) ?: ""

        initializeTTS()
        checkCameraPermissions()

        setContent {
            ZiraTheme {
                CameraScreen()
            }
        }
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                speak("Camera mode activated")
            }
        }
    }

    private fun checkCameraPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        ) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 103)
        }
    }

    @Composable
    fun CameraScreen() {
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
                    text = "Camera",
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

            // Camera Options
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(getCameraOptions()) { option ->
                    CameraOptionCard(option) {
                        handleCameraAction(option)
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    @Composable
    fun CameraOptionCard(option: CameraOption, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
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
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = "Camera",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    private fun getCameraOptions(): List<CameraOption> {
        return listOf(
            CameraOption("📸 Take Photo", "Capture a photo", "photo"),
            CameraOption("🎥 Record Video", "Start video recording", "video"),
            CameraOption("📖 Read Text", "Scan and read text", "text"),
            CameraOption("🔍 Identify Object", "Identify what's in frame", "identify"),
            CameraOption("🎭 Face Detection", "Detect faces", "face"),
            CameraOption("📊 QR Code", "Scan QR code", "qr")
        )
    }

    private fun handleCameraAction(option: CameraOption) {
        when (option.action) {
            "photo" -> {
                try {
                    val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivity(cameraIntent)
                    speak("Opening camera to take photo")
                } catch (e: Exception) {
                    speak("Camera app not available")
                }
            }
            "video" -> {
                try {
                    val videoIntent = Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE)
                    startActivity(videoIntent)
                    speak("Opening camera to record video")
                } catch (e: Exception) {
                    speak("Video recording not available")
                }
            }
            "text" -> {
                speak("Text recognition requires additional ML Kit setup")
            }
            "identify" -> {
                speak("Object identification requires ML Kit integration")
            }
            "face" -> {
                speak("Face detection requires ML Kit setup")
            }
            "qr" -> {
                speak("QR code scanning requires barcode scanner library")
            }
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    data class CameraOption(
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