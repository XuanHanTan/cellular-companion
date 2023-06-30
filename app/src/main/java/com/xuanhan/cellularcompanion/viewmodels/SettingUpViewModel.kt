package com.xuanhan.cellularcompanion.viewmodels

import android.content.Context
import com.xuanhan.cellularcompanion.bluetoothModel

class SettingUpViewModel(private val context: Context) {
    suspend fun setupBluetooth(
        serviceUUID: String,
        sharedKey: String,
        onConnectCallback: () -> Unit
    ) {
        bluetoothModel.initializeFromQR(
            serviceUUID,
            sharedKey,
            initOnConnectCallback = onConnectCallback,
            context = context.applicationContext
        )
    }

    fun shareHotspotDetails(ssid: String, password: String, onCompleteCallback: () -> Unit) {
        bluetoothModel.shareHotspotDetails(
            ssid,
            password,
            onHotspotDetailsSharedCallback = onCompleteCallback
        )
    }

    suspend fun completeSetup() {
        bluetoothModel.markSetupComplete()
    }
}