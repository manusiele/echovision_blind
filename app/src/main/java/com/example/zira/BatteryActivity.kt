package com.example.zira

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class BatteryActivity : BaseFeatureActivity() {

    override fun onTTSReady() {
        val level = getBatteryLevel()
        val charging = if (isCharging()) "and charging" else ""
        speak("Battery is at $level percent $charging")
    }

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

    @Composable
    override fun FeatureContent() {
        var batteryLevel by remember { mutableStateOf(getBatteryLevel()) }
        var charging by remember { mutableStateOf(isCharging()) }

        LaunchedEffect(Unit) {
            // Update battery status periodically
            while (true) {
                batteryLevel = getBatteryLevel()
                charging = isCharging()
                delay(1000)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Battery Status",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "$batteryLevel%",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    batteryLevel > 80 -> Color.Green
                    batteryLevel > 20 -> Color.Yellow
                    else -> Color.Red
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { batteryLevel / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = when {
                    batteryLevel > 80 -> Color.Green
                    batteryLevel > 20 -> Color.Yellow
                    else -> Color.Red
                },
                trackColor = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (charging) "⚡ Charging" else "🔋 Not Charging",
                fontSize = 20.sp,
                color = Color.White
            )
        }
    }
}