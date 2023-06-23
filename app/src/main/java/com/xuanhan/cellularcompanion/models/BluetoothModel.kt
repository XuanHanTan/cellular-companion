package com.xuanhan.cellularcompanion.models

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xuanhan.cellularcompanion.utilities.AES
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.regex.Pattern

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class BluetoothModel {
    private val operationQueue = ConcurrentLinkedQueue<() -> Unit>()
    private var isRunningOperation = false
    private val serviceUUIDKey = stringPreferencesKey("serviceUUID")
    private val sharedKeyKey = stringPreferencesKey("sharedKey")

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var context: Context
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var aes: AES
    private val commandCharacteristicUUID = "00000001-0000-1000-8000-00805f9b34fb"

    private var serviceUUID: String? = null
    private var sharedKey: String? = null
    private var connectDevice: BluetoothDevice? = null
    private var gatt: BluetoothGatt? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var isScanning = false
    private var isConnecting = false
    private var isDisconnecting = false
    private var connectionRetryCount = 0

    private var isFirstInitialising = false
    private var initOnConnectCallback: (() -> Unit)? = null
    private var isSharingHotspotDetails = false
    private var shareHotspotDetails2: (() -> Unit)? = null
    private var onHotspotDetailsSharedCallback: (() -> Unit)? = null

    private var onScanFailedCallback: (() -> Unit)? = null
    private var onUnexpectedErrorCallback: (() -> Unit)? = null
    private var onConnectFailedCallback: (() -> Unit)? = null
    private var onHotspotDetailsShareFailedCallback: (() -> Unit)? = null
    val onErrorDismissedCallback = {
        reset()
        initialize(context)
    }

    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            with(result.device) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        println("Error: The Bluetooth Connect permission has not been granted.")
                        return
                    }
                }

                stopScan()
                isScanning = false

                println("Found Bluetooth device: ${result.scanRecord?.serviceUuids} $name $address")
                println("Now attempting to connect to Bluetooth device.")

                isConnecting = true
                connectDevice = this
                connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            when (errorCode) {
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> {
                    onScanFailedCallback?.invoke()
                    indicateOperationComplete()
                    println("Scan failed: restart Bluetooth and press Retry")
                }

                else -> {
                    onScanFailedCallback?.invoke()
                    indicateOperationComplete()
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    println("Error: The Bluetooth Connect permission has not been granted.")
                    return
                }
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                connectionRetryCount = 0

                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    this@BluetoothModel.gatt = gatt

                    println("Connected to device ${gatt!!.device.address}")

                    gatt.discoverServices()
                } else {
                    // TODO: Handle unlinking device --> don't start scan
                    startScan()
                    println("Disconnected from device advertising: ${gatt!!.device.address}")
                }
            } else {
                if (isConnecting) {
                    if (connectionRetryCount < 3) {
                        connectionRetryCount++
                        connectDevice!!.connectGatt(
                            context,
                            false,
                            this,
                            BluetoothDevice.TRANSPORT_LE
                        )
                        println("GATT connection failed but will retry: $status")
                    } else {
                        if (isFirstInitialising) {
                            onConnectFailedCallback?.invoke()
                        }
                        startScan()
                        println("Error: GATT connection failed with status $status")
                    }
                } else if (isDisconnecting && connectionRetryCount < 3) {
                    connectionRetryCount++
                    gatt!!.disconnect()
                    println("GATT disconnection failed but will retry: $status")
                } else {
                    println("Error: GATT disconnection failed with status $status")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            this@BluetoothModel.gatt = gatt

            if (status == BluetoothGatt.GATT_SUCCESS) {
                println("Discovered ${gatt!!.services.size} services for device ${gatt.device.address}")

                if (gatt.services.find { it.uuid.toString() == serviceUUID } != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            println("Error: The Bluetooth Connect permission has not been granted.")
                            return
                        }
                    }

                    println("Found Cellular service!")

                    commandCharacteristic = getCharacteristicForDevice(commandCharacteristicUUID)

                    gatt.requestMtu(517)
                } else {
                    onUnexpectedErrorCallback?.invoke()
                    indicateOperationComplete()
                    println("Could not find service.")
                }
            } else {
                onUnexpectedErrorCallback?.invoke()
                indicateOperationComplete()
                println("Service discovery failed due to internal error: $status")
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (isFirstInitialising) {
                    initialize2()
                } else if (isSharingHotspotDetails) {
                    shareHotspotDetails2?.invoke()
                }

                println("MTU changed to $mtu")
            } else {
                onUnexpectedErrorCallback?.invoke()
                indicateOperationComplete()
                println("Error: MTU change failed with status $status")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                println("Wrote to characteristic ${characteristic!!.uuid}")

                if (isFirstInitialising) {
                    initOnConnectCallback?.invoke()
                    isFirstInitialising = false
                    initOnConnectCallback = null
                    indicateOperationComplete()
                } else if (isSharingHotspotDetails) {
                    onHotspotDetailsSharedCallback?.invoke()
                    isSharingHotspotDetails = false
                    onHotspotDetailsSharedCallback = null
                    indicateOperationComplete()
                }
            } else {
                println("Error: Failed to write to characteristic ${characteristic!!.uuid} with status $status")

                if (isFirstInitialising) {
                    onConnectFailedCallback?.invoke()
                    indicateOperationComplete()
                } else if (isSharingHotspotDetails) {
                    onHotspotDetailsShareFailedCallback?.invoke()
                    indicateOperationComplete()
                }
            }
        }
    }

    @Synchronized
    private fun enqueueOperation(operation: () -> Unit) {
        operationQueue.add(operation)
        if (!isRunningOperation) {
            doNextOperation()
        }
    }

    @Synchronized
    private fun doNextOperation() {
        isRunningOperation = true
        operationQueue.poll()?.invoke()
    }

    @Synchronized
    private fun indicateOperationComplete() {
        isRunningOperation = false
        doNextOperation()
    }

    suspend fun initializeFromQR(
        serviceUUID: String,
        sharedKey: String,
        initOnConnectCallback: () -> (Unit),
        context: Context
    ) {
        // Validate and store service UUID and Key in DataStore
        val uuidRegex =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        val alphaNumericRegex = Pattern.compile("^[a-zA-Z0-9]*$")
        if (!uuidRegex.matcher(serviceUUID).matches()) {
            println("Invalid service UUID.")
            return
        }
        if (!alphaNumericRegex.matcher(sharedKey).matches()) {
            println("Invalid shared key.")
            return
        }

        context.dataStore.edit { settings ->
            settings[serviceUUIDKey] = serviceUUID.lowercase()
            settings[sharedKeyKey] = sharedKey
        }

        isFirstInitialising = true
        this.initOnConnectCallback = initOnConnectCallback
        this.serviceUUID = serviceUUID.lowercase()
        this.sharedKey = sharedKey
        initialize(context)
    }

    suspend fun initializeFromDataStore(context: Context) {
        // Retrieve service UUID and Key from DataStore
        serviceUUID = context.dataStore.data.map { settings ->
            settings[serviceUUIDKey] ?: ""
        }.collect().toString()

        sharedKey = context.dataStore.data.map { settings ->
            settings[sharedKeyKey] ?: ""
        }.collect().toString()

        if (serviceUUID == "") {
            println("Error: Service UUID not found in DataStore.")
            return
        }

        if (sharedKey == "") {
            println("Error: Shared Key not found in DataStore.")
            return
        }

        initialize(context)
    }

    private fun initialize(context: Context) {
        // Initialise context and BluetoothManager
        this.context = context
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        aes = AES(sharedKey!!)

        // Start BLE scan
        startScan()
    }

    private fun initialize2() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                println("Error: The Bluetooth Connect permission has not been granted.")
                return
            }
        }

        commandCharacteristic!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        commandCharacteristic!!.value = "0".toByteArray()
        gatt!!.writeCharacteristic(commandCharacteristic!!)
    }

    private fun startScan() {
        // Scan for BLE devices advertising this service UUID
        val filter =
            ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(serviceUUID)).build()
        val scanSettings = ScanSettings.Builder().apply {
            if (isFirstInitialising) {
                setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            } else {
                setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            }
        }
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            .build()

        // Start BLE scan
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                println("Error: The Bluetooth Scan permission has not been granted.")
                return
            }
        }

        bluetoothAdapter.bluetoothLeScanner.startScan(
            listOf(filter),
            scanSettings,
            bleScanCallback
        )
        isScanning = true
    }

    fun stopScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                println("Error: The Bluetooth Scan permission has not been granted.")
                return
            }
        }

        if (isScanning) {
            bluetoothAdapter.bluetoothLeScanner.stopScan(bleScanCallback)
        }
    }

    private fun getCharacteristicForDevice(characteristicUUID: String): BluetoothGattCharacteristic? {
        if (gatt == null) {
            println("Error: No GATT device initialised.")
            return null
        }

        with(gatt!!.services.find { it.uuid.toString() == serviceUUID }) {
            if (this == null) {
                println("Error: Cellular service could not be found")
                return null
            }

            return characteristics.find { it.uuid.toString() == characteristicUUID }
        }
    }

    fun shareHotspotDetails(
        ssid: String,
        password: String,
        onHotspotDetailsSharedCallback: () -> Unit
    ) {
        if (gatt == null) {
            println("Error: Device is unavailable.")
            return
        }
        if (commandCharacteristic == null) {
            println("Error: Command characteristic is unavailable")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                println("Error: The Bluetooth Connect permission has not been granted.")
                return
            }
        }

        enqueueOperation {
            startShareHotspotDetails(ssid, password, onHotspotDetailsSharedCallback)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startShareHotspotDetails(
        ssid: String,
        password: String,
        onHotspotDetailsSharedCallback: () -> Unit
    ) {
        isSharingHotspotDetails = true
        this.onHotspotDetailsSharedCallback = onHotspotDetailsSharedCallback

        startScan()
        shareHotspotDetails2 = {
            commandCharacteristic!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            val (cipherText, iv) = aes.encrypt("$ssid $password")
            commandCharacteristic!!.value = "1 $iv $cipherText".toByteArray()
            gatt!!.writeCharacteristic(commandCharacteristic!!)
        }
    }

    fun registerForErrorHandling(
        onScanFailedCallback: () -> Unit,
        onUnexpectedErrorCallback: () -> Unit,
        onHotspotDetailsShareFailedCallback: () -> Unit,
        onConnectFailedCallback: () -> Unit
    ) {
        this.onScanFailedCallback = onScanFailedCallback
        this.onUnexpectedErrorCallback = onUnexpectedErrorCallback
        this.onHotspotDetailsShareFailedCallback = onHotspotDetailsShareFailedCallback
        this.onConnectFailedCallback = onConnectFailedCallback
    }

    private fun reset() {
        stopScan()

        connectDevice = null
        gatt = null
        commandCharacteristic = null
        isScanning = false
        isConnecting = false
        isDisconnecting = false
        connectionRetryCount = 0
    }
}