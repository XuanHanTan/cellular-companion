package com.xuanhan.cellularcompanion.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanhan.cellularcompanion.bluetoothModel
import com.xuanhan.cellularcompanion.models.dataStore
import com.xuanhan.cellularcompanion.passwordKey
import com.xuanhan.cellularcompanion.ssidKey
import kotlinx.coroutines.launch

/**
 * This class is the view model for the Setting Up (2) screen.
 */
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