package com.example.cinebox

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

// Báo khi bật/tắt chế độ máy bay (giống ví dụ chương 7)
class AirplaneReceiver(private val onChange: (Boolean) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isOn = intent.getBooleanExtra("state", false)
        onChange(isOn)
    }
}

// Báo khi pin yếu
class BatteryLowReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BATTERY_LOW) {
            Toast.makeText(context, "Pin yếu, hãy cắm sạc!", Toast.LENGTH_LONG).show()
        }
    }
}