package com.xuanhan.cellularcompanion.broadcastreceivers

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xuanhan.cellularcompanion.bluetoothModel

/**
 * This broadcast receiver is used to detect changes in the Bluetooth bond state.
 * */
class BondStateBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
            println("Error: BondStateBroadcastReceiver called with invalid action.")
            return
        }

        when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)) {
            BluetoothDevice.BOND_BONDED -> {
                println("BondStateBroadcastReceiver: Bonded")

                // Inform Bluetooth model that bonding is successful
                bluetoothModel.onBonded()

                // Unregister this broadcast receiver
                context.unregisterReceiver(this)
            }
            BluetoothDevice.BOND_BONDING -> {
                println("BondStateBroadcastReceiver: Bonding")
            }
            BluetoothDevice.BOND_NONE -> {
                println("BondStateBroadcastReceiver: Not bonded")

                // Inform Bluetooth model that bonding has failed
                bluetoothModel.onBondingFailed()

                // Unregister this broadcast receiver
                context.unregisterReceiver(this)
            }
            else -> {
                println("BondStateBroadcastReceiver: Unknown bond state")
            }
        }
    }
}