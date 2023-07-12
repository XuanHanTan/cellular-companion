package com.xuanhan.cellularcompanion.viewmodels

import com.xuanhan.cellularcompanion.bluetoothModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomePageViewModel {
    private val _hotspotStatusMessage = MutableStateFlow("Disconnected")
    val hotspotStatusMessage: StateFlow<String> = _hotspotStatusMessage.asStateFlow()

    init {
        bluetoothModel.registerForUIChanges(onConnectStatusUpdate = ::onConnectStatusUpdate)
    }

    private fun onConnectStatusUpdate(isConnected: Boolean) {
        if (isConnected) {
            _hotspotStatusMessage.value = "Idle"
        } else {
            _hotspotStatusMessage.value = "Disconnected"
        }
    }
}