package com.xuanhan.cellularcompanion.viewmodels

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xuanhan.cellularcompanion.bluetoothModel
import com.xuanhan.cellularcompanion.models.BluetoothModel.Companion.ConnectStatus
import com.xuanhan.cellularcompanion.requiresBtPermissionCheck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _isShowingScanFailedDialog = MutableStateFlow(false)
    val isShowingScanFailedDialog: StateFlow<Boolean> = _isShowingScanFailedDialog.asStateFlow()
    private lateinit var retryScanFailedDialogCallback: () -> Unit
    private val _isShowingUnexpectedErrorDialog = MutableStateFlow(false)
    val isShowingUnexpectedErrorDialog: StateFlow<Boolean> =
        _isShowingUnexpectedErrorDialog.asStateFlow()
    private lateinit var retryUnexpectedErrorDialogCallback: () -> Unit
    private val _isShowingHotspotDetailsShareFailedDialog = MutableStateFlow(false)
    val isShowingHotspotDetailsShareFailedDialog: StateFlow<Boolean> =
        _isShowingHotspotDetailsShareFailedDialog.asStateFlow()
    private lateinit var retryHotspotDetailsShareFailedDialogCallback: () -> Unit
    private val _isShowingConnectFailedDialog = MutableStateFlow(false)
    val isShowingConnectFailedDialog: StateFlow<Boolean> =
        _isShowingConnectFailedDialog.asStateFlow()
    private lateinit var retryConnectFailedDialogCallback: () -> Unit
    private val _isShowingBondFailedDialog = MutableStateFlow(false)
    val isShowingBondFailedDialog: StateFlow<Boolean> =
        _isShowingBondFailedDialog.asStateFlow()
    private lateinit var retryBondFailedDialogCallback: () -> Unit
    private val _isShowingHotspotFailedDialog = MutableStateFlow(false)
    val isShowingHotspotFailedDialog: StateFlow<Boolean> =
        _isShowingHotspotFailedDialog.asStateFlow()
    private lateinit var retryHotspotFailedDialogCallback: () -> Unit
    private val _isShowingResetFailedDialog = MutableStateFlow(false)
    val isShowingResetFailedDialog: StateFlow<Boolean> =
        _isShowingResetFailedDialog.asStateFlow()
    private lateinit var retryResetFailedDialogCallback: () -> Unit
    private val _isBluetoothEnabled = MutableStateFlow(true)
    val isBluetoothEnabled: StateFlow<Boolean> =
        _isBluetoothEnabled.asStateFlow()

    var connectStatus = ConnectStatus.Disconnected
        private set

    init {
        bluetoothModel.registerForErrorHandling(
            ::showScanFailedDialog,
            ::showUnexpectedErrorDialog,
            ::showConnectFailedDialog,
            ::showBondFailedDialog,
            ::showHotspotDetailsShareFailedDialog,
            ::showHotspotFailedDialog,
            ::showResetFailedDialog
        )

        viewModelScope.launch {
            bluetoothModel.connectStatus.collect {
                connectStatus = it
            }
        }
    }

    private fun showScanFailedDialog(retryCallback: () -> Unit) {
        _isShowingScanFailedDialog.value = true
        retryScanFailedDialogCallback = retryCallback
    }

    fun confirmScanFailedDialog() {
        _isShowingScanFailedDialog.value = false
        retryScanFailedDialogCallback()
    }

    private fun showUnexpectedErrorDialog(retryCallback: () -> Unit) {
        _isShowingUnexpectedErrorDialog.value = true
        retryUnexpectedErrorDialogCallback = retryCallback
    }

    fun confirmUnexpectedErrorDialog() {
        _isShowingUnexpectedErrorDialog.value = false
        retryUnexpectedErrorDialogCallback()
    }

    private fun showHotspotDetailsShareFailedDialog(retryCallback: () -> Unit) {
        _isShowingHotspotDetailsShareFailedDialog.value = true
        retryHotspotDetailsShareFailedDialogCallback = retryCallback
    }

    fun confirmHotspotDetailsShareFailedDialog() {
        _isShowingHotspotDetailsShareFailedDialog.value = false
        retryHotspotDetailsShareFailedDialogCallback()
    }

    private fun showConnectFailedDialog(retryCallback: () -> Unit) {
        _isShowingConnectFailedDialog.value = true
        retryConnectFailedDialogCallback = retryCallback
    }

    fun confirmConnectFailedDialog() {
        _isShowingConnectFailedDialog.value = false
        retryConnectFailedDialogCallback()
    }

    private fun showBondFailedDialog(retryCallback: () -> Unit) {
        _isShowingBondFailedDialog.value = true
        retryBondFailedDialogCallback = retryCallback
    }

    fun confirmBondFailedDialog() {
        _isShowingBondFailedDialog.value = false
        retryBondFailedDialogCallback()
    }

    private fun showHotspotFailedDialog(retryCallback: () -> Unit) {
        _isShowingHotspotFailedDialog.value = true
        retryHotspotFailedDialogCallback = retryCallback
    }

    fun confirmHotspotFailedDialog() {
        _isShowingHotspotFailedDialog.value = false
        retryHotspotFailedDialogCallback()
    }

    private fun showResetFailedDialog(retryCallback: () -> Unit) {
        _isShowingResetFailedDialog.value = true
        retryResetFailedDialogCallback = retryCallback
    }

    fun confirmResetFailedDialog() {
        _isShowingResetFailedDialog.value = false
        retryResetFailedDialogCallback()
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