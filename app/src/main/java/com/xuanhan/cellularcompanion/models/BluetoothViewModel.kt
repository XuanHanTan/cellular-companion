package com.xuanhan.cellularcompanion.models

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid


class BluetoothViewModel(private val context: Context) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val cellularServiceUUID = "c3b9b9e9-be4e-4abf-9200-770f88b59977"

    fun initialize() {
        // scan for BLE devices with UUID of cellularServiceUUID
        // if found, connect to it
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(cellularServiceUUID)).build()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                with(result.device) {

                    println("Found Bluetooth device: ${result.scanRecord?.serviceUuids} $name $address")
                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                println("Scan failed with error code $errorCode")
            }
        }

        bluetoothAdapter.bluetoothLeScanner.startScan(
            listOf(filter),
            scanSettings,
            scanCallback
        )
        println("Init scan")
    }
}