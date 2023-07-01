package com.xuanhan.cellularcompanion.viewmodels

import android.bluetooth.BluetoothManager
import android.content.Context
import com.xuanhan.cellularcompanion.bluetoothModel
import com.xuanhan.cellularcompanion.requiresBtPermissionCheck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel {
    private val _isShowingScanFailedDialog = MutableStateFlow(false)
    val isShowingScanFailedDialog: StateFlow<Boolean> = _isShowingScanFailedDialog.asStateFlow()
    private val _isShowingUnexpectedErrorDialog = MutableStateFlow(false)
    val isShowingUnexpectedErrorDialog: StateFlow<Boolean> =
        _isShowingUnexpectedErrorDialog.asStateFlow()
    private val _isShowingHotspotDetailsShareFailedDialog = MutableStateFlow(false)
    val isShowingHotspotDetailsShareFailedDialog: StateFlow<Boolean> =
        _isShowingHotspotDetailsShareFailedDialog.asStateFlow()
    private val _isShowingConnectFailedDialog = MutableStateFlow(false)
    val isShowingConnectFailedDialog: StateFlow<Boolean> =
        _isShowingConnectFailedDialog.asStateFlow()
    private val _isBluetoothEnabled = MutableStateFlow(true)
    val isBluetoothEnabled: StateFlow<Boolean> =
        _isBluetoothEnabled.asStateFlow()

    init {
        bluetoothModel.registerForErrorHandling(
            ::showScanFailedDialog,
            ::showUnexpectedErrorDialog,
            ::showHotspotDetailsShareFailedDialog,
            ::showConnectFailedDialog
        )
    }

    private fun showScanFailedDialog() {
        _isShowingScanFailedDialog.value = true
    }

    fun confirmScanFailedDialog() {
        _isShowingScanFailedDialog.value = false
        bluetoothModel.onErrorDismissedCallback()
    }

    private fun showUnexpectedErrorDialog() {
        _isShowingUnexpectedErrorDialog.value = true
    }

    fun confirmUnexpectedErrorDialog() {
        _isShowingUnexpectedErrorDialog.value = false
        bluetoothModel.onErrorDismissedCallback()
    }

    private fun showHotspotDetailsShareFailedDialog() {
        _isShowingHotspotDetailsShareFailedDialog.value = true
    }

    fun confirmHotspotDetailsShareFailedDialog() {
        _isShowingHotspotDetailsShareFailedDialog.value = false
        bluetoothModel.onErrorDismissedCallback()
    }

    private fun showConnectFailedDialog() {
        _isShowingConnectFailedDialog.value = true
    }

    fun confirmConnectFailedDialog() {
        _isShowingConnectFailedDialog.value = false
        bluetoothModel.onErrorDismissedCallback()
    }

    fun setBluetoothEnabled(context: Context): Boolean {
        if (!requiresBtPermissionCheck) {
            _isBluetoothEnabled.value = true
        } else {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            _isBluetoothEnabled.value = bluetoothAdapter.isEnabled
        }
        return _isBluetoothEnabled.value
    }
}