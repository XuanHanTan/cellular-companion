package com.xuanhan.cellularcompanion.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.xuanhan.cellularcompanion.bluetoothModel

class SettingUpViewModel(private val context: Context) {
    val loadingMessage = mutableStateOf("Establishing a secure connection...")

    suspend fun setupBluetooth(serviceUUID: String, sharedPIN: String) {
        bluetoothModel.initializeFromQR(
            serviceUUID,
            sharedPIN,
            initOnConnectCallback = {
                loadingMessage.value = "Sharing hotspot credentials..."
            },
            context = context.applicationContext
        )
    }
}