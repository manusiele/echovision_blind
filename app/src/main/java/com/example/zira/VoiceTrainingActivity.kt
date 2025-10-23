//package com.example.zira
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import java.text.SimpleDateFormat
//import java.util.*
//
//class TimeActivity : BaseFeatureActivity() {
//
//    override fun onTTSReady() {
//        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
//        speak("The time is $time")
//    }
//
//    @Composable
//    override fun FeatureContent() {
//        // Get voice command from intent
//        val voiceCommand = intent.getStringExtra("voice_command") ?: ""
//
//        val currentTime by remember {
//            mutableStateOf(SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()))
//        }
//
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text(
//                text = "⏰",
//                fontSize = 72.sp
//            )
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            Text(
//                text = "Current Time",
//                fontSize = 24.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color(0xFF2196F3)
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Text(
//                text = currentTime,
//                fontSize = 48.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color.White
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Text(
//                text = "Command: \"$voiceCommand\"",
//                fontSize = 14.sp,
//                color = Color.Gray
//            )
//        }
//    }
//}