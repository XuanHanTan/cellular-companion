package com.xuanhan.cellularcompanion.viewmodels

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.xuanhan.cellularcompanion.MainActivity
import com.xuanhan.cellularcompanion.bluetoothModel

fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("Error: Context should be of an Activity.")
}

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
        val mainActivity = context.findActivity() as MainActivity
        mainActivity.startService()
        mainActivity.connectService()
    }
}