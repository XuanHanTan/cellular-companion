package com.xuanhan.cellularcompanion.broadcastreceivers

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xuanhan.cellularcompanion.bluetoothModel

class BondStateBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1?.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
            println("Error: BondStateBroadcastReceiver called with invalid action.")
            return
        }

        when (p1.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)) {
            BluetoothDevice.BOND_BONDED -> {
                println("BondStateBroadcastReceiver: Bonded")
                bluetoothModel.onBonded()
            }
            BluetoothDevice.BOND_BONDING -> {
                println("BondStateBroadcastReceiver: Bonding")
            }
            BluetoothDevice.BOND_NONE -> {
                println("BondStateBroadcastReceiver: Not bonded")
                bluetoothModel.onBondingFailed()
            }
            else -> {
                println("BondStateBroadcastReceiver: Unknown bond state")
            }
        }
    }
}