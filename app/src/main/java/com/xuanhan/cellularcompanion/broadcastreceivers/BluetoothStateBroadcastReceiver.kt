package com.xuanhan.cellularcompanion.broadcastreceivers

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xuanhan.cellularcompanion.isBluetoothEnabled

class BluetoothStateBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (BluetoothAdapter.ACTION_STATE_CHANGED == intent.action) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                BluetoothAdapter.STATE_ON -> {
                    isBluetoothEnabled.value = true
                }
                BluetoothAdapter.STATE_OFF -> {
                    isBluetoothEnabled.value = false
                }
            }
            println("Bluetooth enabled state change: ${isBluetoothEnabled.value}")
        }
    }
}