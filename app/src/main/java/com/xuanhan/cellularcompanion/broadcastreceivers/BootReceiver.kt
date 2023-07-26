package com.xuanhan.cellularcompanion.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.xuanhan.cellularcompanion.services.BluetoothService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "onReceive: Boot completed")
            Intent(context, BluetoothService::class.java).also { intent ->
                context.startForegroundService(intent)
            }
        }
    }
}