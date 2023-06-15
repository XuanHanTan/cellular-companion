package com.xuanhan.cellularcompanion.models

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import java.util.regex.Pattern

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class BluetoothModel {
    private val serviceUUIDKey = stringPreferencesKey("serviceUUID")
    private val sharedPINKey = stringPreferencesKey("sharedPIN")

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var context: Context
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private var serviceUUID: String? = null
    private var sharedPIN: String? = null
    private var connectDevice: BluetoothDevice? = null
    private var gatt: BluetoothGatt? = null
    private var isScanning = false
    private var isConnecting = false
    private var isDisconnecting = false
    private var connectionRetryCount = 0

    private var isFirstInitialising = false
    private var initOnConnectCallback: (() -> Unit)? = null

    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            with(result.device) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }

                stopScan()
                isScanning = false

                println("Found Bluetooth device: ${result.scanRecord?.serviceUuids} $name $address")
                println("Now attempting to connect to Bluetooth device.")

                connectDevice = this
                connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            when (errorCode) {
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> {
                    // TODO: Handle scan failed
                    println("Scan failed: restart Bluetooth and press Retry")
                }

                else -> {
                    // TODO: Handle scan failed
                    println("Scan failed with error code $errorCode")
                }
            }
        }
    }


    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt?,
            status: Int,
            newState: Int
        ) {
            super.onConnectionStateChange(gatt, status, newState)

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                connectionRetryCount = 0

                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    this@BluetoothModel.gatt = gatt

                    println("Connected to device ${gatt!!.device.address}")

                    if (isFirstInitialising) {
                        initOnConnectCallback?.invoke()
                        // TODO: share hotspot info
                    }
                } else {
                    println("Disconnected from device advertising: ${gatt!!.device.address}")
                }
            } else {
                if (isConnecting && connectionRetryCount < 3) {
                    connectionRetryCount++
                    connectDevice!!.connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE)
                    println("GATT connection failed but will retry: $status")
                } else if (isDisconnecting && connectionRetryCount < 3) {
                    connectionRetryCount++
                    gatt!!.disconnect()
                    println("GATT disconnection failed but will retry: $status")
                } else {
                    println("Error: GATT connection failed with status $status")
                }
            }
        }
    }

    suspend fun initializeFromQR(
        serviceUUID: String,
        sharedPIN: String,
        initOnConnectCallback: () -> (Unit),
        context: Context
    ) {
        // Validate and store service UUID and PIN in DataStore
        val uuidRegex =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        if (!uuidRegex.matcher(serviceUUID).matches()) {
            println("Invalid service UUID.")
            return
        }
        if (!uuidRegex.matcher(sharedPIN).matches()) {
            println("Invalid shared PIN.")
            return
        }

        context.dataStore.edit { settings ->
            settings[serviceUUIDKey] = serviceUUID
            settings[sharedPINKey] = sharedPIN
        }

        isFirstInitialising = true
        this.initOnConnectCallback = initOnConnectCallback
        this.serviceUUID = serviceUUID
        this.sharedPIN = sharedPIN
        initialize(context)
    }

    suspend fun initializeFromDataStore(context: Context) {
        // Retrieve service UUID and PIN from DataStore
        serviceUUID = context.dataStore.data.map { settings ->
            settings[serviceUUIDKey] ?: ""
        }.collect().toString()

        sharedPIN = context.dataStore.data.map { settings ->
            settings[sharedPINKey] ?: ""
        }.collect().toString()

        if (serviceUUID == "") {
            println("Error: Service UUID not found in DataStore.")
            return
        }

        if (sharedPIN == "") {
            println("Error: Shared PIN not found in DataStore.")
            return
        }

        initialize(context)
    }

    private fun initialize(context: Context) {
        // Initialise context and BluetoothManager
        this.context = context
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Scan for BLE devices advertising this service UUID
        val filter =
            ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(serviceUUID)).build()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            .build()

        // Start BLE scan
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        bluetoothAdapter.bluetoothLeScanner.startScan(
            listOf(filter),
            scanSettings,
            bleScanCallback
        )
        isScanning = true
    }

    fun stopScan() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (isScanning) {
            bluetoothAdapter.bluetoothLeScanner.stopScan(bleScanCallback)
        }
    }
}