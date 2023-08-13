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

class ChangeHotspotCredentialsViewModel(context: Context) : ViewModel() {
    val ssid = MutableStateFlow("")
    val password = MutableStateFlow("")
    private val _prevSSID = MutableStateFlow("")
    val prevSSID = _prevSSID.asStateFlow()

    init {
        viewModelScope.launch {
            context.dataStore.data.map { settings ->
                settings[ssidKey] ?: ""
            }.collect {
                ssid.value = it
                _prevSSID.value = it
            }
            context.dataStore.data.map { settings ->
                settings[passwordKey] ?: ""
            }.collect {
                password.value = it
            }
        }
    }

    fun save(context: Context, onCompleteCallback: () -> Unit, onDefferedCallback: () -> Unit) {
        var skipOnCompleteCallback = false

        if (bluetoothModel.connectStatus.value == ConnectStatus.Disconnected) {
            onDefferedCallback()
            skipOnCompleteCallback = true
        }

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