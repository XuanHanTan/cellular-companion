package com.xuanhan.cellularcompanion.viewmodels

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanhan.cellularcompanion.bluetoothModel
import com.xuanhan.cellularcompanion.models.dataStore
import com.xuanhan.cellularcompanion.passwordKey
import com.xuanhan.cellularcompanion.ssidKey
import kotlinx.coroutines.launch

fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("Error: Context should be of an Activity.")
}

class SettingUpViewModel: ViewModel() {
    suspend fun setupBluetooth(
        serviceUUID: String,
        sharedKey: String,
        context: Context,
        onConnectCallback: () -> Unit,
    ) {
        bluetoothModel.initializeFromQR(
            serviceUUID,
            sharedKey,
            initOnConnectCallback = onConnectCallback,
            context = context.applicationContext
        )
    }

    fun shareHotspotDetails(ssid: String, password: String, context: Context, onCompleteCallback: () -> Unit) {
        bluetoothModel.shareHotspotDetails(
            ssid,
            password,
            onHotspotDetailsSharedCallback = {
                viewModelScope.launch {
                    context.dataStore.edit { settings ->
                        settings[ssidKey] = ssid
                        settings[passwordKey] = password
                    }
                    onCompleteCallback()
                }
            }
        )
    }

    suspend fun completeSetup() {
        bluetoothModel.markSetupComplete()
    }
}