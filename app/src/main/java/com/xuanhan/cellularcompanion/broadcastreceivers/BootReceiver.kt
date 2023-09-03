package com.xuanhan.cellularcompanion.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.xuanhan.cellularcompanion.isSetupComplete
import com.xuanhan.cellularcompanion.isSetupCompleteKey
import com.xuanhan.cellularcompanion.models.dataStore
import com.xuanhan.cellularcompanion.services.BluetoothService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * This broadcast receiver is used to detect when the device has booted up.
 * */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "onReceive: Boot completed")

            CoroutineScope(Dispatchers.IO).launch {
                isSetupComplete = context.dataStore.data.map { settings ->
                    settings[isSetupCompleteKey] ?: false
                }.first()

                // Start Bluetooth service if setup is complete
                if (isSetupComplete) {
                    CoroutineScope(Dispatchers.Main).launch {
                        Intent(context, BluetoothService::class.java).also { intent ->
                            context.startForegroundService(intent)
                        }
                    }
                }
            }
        }
    }
}