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

class PackageUpdatedBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("PackageUpdatedBroadcastReceiver", "onReceive: Package updated")

            CoroutineScope(Dispatchers.IO).launch {
                isSetupComplete = context.dataStore.data.map { settings ->
                    settings[isSetupCompleteKey] ?: false
                }.first()
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