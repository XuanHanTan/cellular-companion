package com.xuanhan.cellularcompanion.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanhan.cellularcompanion.bluetoothModel
import com.xuanhan.cellularcompanion.models.dataStore
import com.xuanhan.cellularcompanion.passwordKey
import com.xuanhan.cellularcompanion.ssidKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.xuanhan.cellularcompanion.models.BluetoothModel.Companion.ConnectStatus

/**
 * This class is the view model for the Change Hotspot Credentials screen.
 */
class ChangeHotspotCredentialsViewModel(context: Context) : ViewModel() {
    val ssid = MutableStateFlow("")
    val password = MutableStateFlow("")
    private val _prevSSID = MutableStateFlow("")
    val prevSSID = _prevSSID.asStateFlow()
    private val _prevPassword = MutableStateFlow("")
    val prevPassword = _prevPassword.asStateFlow()

    init {
        viewModelScope.launch {
            context.dataStore.data.map { settings ->
                settings[ssidKey] ?: ""
            }.collect {
                ssid.value = it
                _prevSSID.value = it
            }
        }
        viewModelScope.launch {
            context.dataStore.data.map { settings ->
                settings[passwordKey] ?: ""
            }.collect {
                password.value = it
                _prevPassword.value = it
            }
        }
    }

    /**
     * This function saves the new hotspot credentials to the phone's DataStore. Either the [onCompleteCallback] or [onDeferredCallback] will be called.
     * @param context The context of the activity that calls this function.
     * @param onCompleteCallback The callback function to be called when the hotspot details have been shared.
     * @param onDeferredCallback The callback function to be called when the hotspot details have not been shared but is queued to be shared.
     */
    fun save(context: Context, onCompleteCallback: () -> Unit, onDeferredCallback: () -> Unit) {
        var skipOnCompleteCallback = false

        // Do not wait for completion if Mac is currently disconnected
        if (bluetoothModel.connectStatus.value == ConnectStatus.Disconnected) {
            onDeferredCallback()
            skipOnCompleteCallback = true
        }

        // Share hotspot details
        bluetoothModel.shareHotspotDetails(
            ssid.value,
            password.value,
            onHotspotDetailsSharedCallback = {
                viewModelScope.launch {
                    context.dataStore.edit { settings ->
                        settings[ssidKey] = ssid.value
                        settings[passwordKey] = password.value
                    }

                    if (!skipOnCompleteCallback) {
                        onCompleteCallback()
                    }
                }
            }
        )
    }
}