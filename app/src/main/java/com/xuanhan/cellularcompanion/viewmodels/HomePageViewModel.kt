package com.xuanhan.cellularcompanion.viewmodels

import com.xuanhan.cellularcompanion.bluetoothModel
import com.xuanhan.cellularcompanion.models.BluetoothModel.Companion.ConnectStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomePageViewModel {
    private val _hotspotStatusMessage = MutableStateFlow("Disconnected")
    val hotspotStatusMessage: StateFlow<String> = _hotspotStatusMessage.asStateFlow()

    init {
        bluetoothModel.registerForUIChanges(onConnectStatusUpdate = ::onConnectStatusUpdate)
    }

    private fun onConnectStatusUpdate(status: ConnectStatus) {
        when (status) {
            ConnectStatus.Disconnected -> _hotspotStatusMessage.value = "Disconnected"
            ConnectStatus.Idle -> _hotspotStatusMessage.value = "Idle"
            ConnectStatus.Connecting -> _hotspotStatusMessage.value = "Connecting"
            ConnectStatus.Connected -> _hotspotStatusMessage.value = "Connected"
        }
    }
}