package com.xuanhan.cellularcompanion.viewmodels

import com.xuanhan.cellularcompanion.bluetoothModel
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
}