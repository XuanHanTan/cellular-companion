package com.xuanhan.cellularcompanion.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.xuanhan.cellularcompanion.bluetoothModel

class SettingUpViewModel(private val context: Context) {
    val loadingMessage = mutableStateOf("Establishing a secure connection...")

    suspend fun setupBluetooth(serviceUUID: String, sharedKey: String) {
        bluetoothModel.initializeFromQR(
            serviceUUID,
            sharedKey,
            initOnConnectCallback = {
                loadingMessage.value = "Sharing hotspot credentials..."
                bluetoothModel.shareHotspotDetails("test", "test")
            },
            context = context.applicationContext
        )
    }
}