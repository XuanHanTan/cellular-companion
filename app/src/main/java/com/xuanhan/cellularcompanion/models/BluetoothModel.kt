package com.xuanhan.cellularcompanion.models

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xuanhan.cellularcompanion.broadcastreceivers.BondStateBroadcastReceiver
import com.xuanhan.cellularcompanion.isSetupComplete
import com.xuanhan.cellularcompanion.isSetupCompleteKey
import com.xuanhan.cellularcompanion.utilities.AES
import com.xuanhan.cellularcompanion.utilities.HotspotOnStartTetheringCallback
import com.xuanhan.cellularcompanion.utilities.WifiHotspotManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.regex.Pattern

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * This class handles all things related to connecting to and communicating with the Cellular app on the Mac.
 * */
class BluetoothModel {
    private val operationQueue = ConcurrentLinkedQueue<() -> Unit>()
    private var isRunningOperation = false
    private val serviceUUIDKey = stringPreferencesKey("serviceUUID")
    private val sharedKeyKey = stringPreferencesKey("sharedKey")
    private val deviceAddressKey = stringPreferencesKey("deviceAddress")

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var context: Context
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var aes: AES
    private lateinit var wifiHotspotManager: WifiHotspotManager
    private val commandCharacteristicUUID = "00000001-0000-1000-8000-00805f9b34fb"
    private val notificationCharacteristicUUID = "00000002-0000-1000-8000-00805f9b34fb"

    private var serviceUUID: String? = null
    private var sharedKey: String? = null
    private var deviceAddress: String? = null
    private var connectDevice: BluetoothDevice? = null
    private var gatt: BluetoothGatt? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var notificationCharacteristic: BluetoothGattCharacteristic? = null
    private var isScanning = false
    private var isConnecting = false
    private var isDisconnecting = false
    private var connectionRetryCount = 0
    private var hotspotShareRetryCount = 0

    private var isFirstInitializing = false
    private var isInitializing = false
    private var initOnConnectCallback: (() -> Unit)? = null
    private var isSharingHotspotDetails = false
    private var shareHotspotDetails2: (() -> Unit)? = null
    private var onHotspotDetailsSharedCallback: (() -> Unit)? = null
    private var isSharingPhoneInfo = false
    private var sharePhoneInfo2: (() -> Unit)? = null
    private var isConnectingToHotspot = false
    private var connectHotspot2: (() -> Unit)? = null
    private var isDisconnectingFromHotspot = false
    private var disconnectHotspot2: (() -> Unit)? = null

    private var onScanFailedCallback: ((() -> Unit) -> Unit)? = null
    private var onUnexpectedErrorCallback: ((() -> Unit) -> Unit)? = null
    private var onConnectFailedCallback: ((() -> Unit) -> Unit)? = null
    private var onBondFailedCallback: ((() -> Unit) -> Unit)? = null
    private var onHotspotDetailsShareFailedCallback: ((() -> Unit) -> Unit)? = null
    private var onHotspotFailedCallback: ((() -> Unit) -> Unit)? = null

    private var hotspotDetailsRetryCallback: (() -> Unit)? = null

    private val _connectStatus: MutableStateFlow<ConnectStatus> = MutableStateFlow(ConnectStatus.Disconnected)
    val connectStatus: StateFlow<ConnectStatus> = _connectStatus.asStateFlow()
    private val _isSeePhoneInfoEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isSeePhoneInfoEnabled: StateFlow<Boolean> = _isSeePhoneInfoEnabled.asStateFlow()

    class NotificationType {
        companion object {
            const val EnableHotspot = "0"
            const val DisableHotspot = "1 0"
            const val DisableHotspotIndicateOnly = "1 1"
            const val IndicateConnectedHotspot = "2"
            const val EnableSeePhoneInfo = "3"
            const val DisableSeePhoneInfo = "4"
        }
    }

    class CommandType {
        companion object {
            const val HelloWorld = "0"
            const val ShareHotspotDetails = "1"
            const val SharePhoneInfo = "2"
            const val ConnectToHotspot = "3"
            const val DisconnectFromHotspot = "4"
        }
    }

    companion object {
        enum class ConnectStatus {
            Disconnected,
            Idle,
            Connecting,
            Connected
        }
    }

    /**
     * This callback handles results during BLE scans.
     * */
    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            if (isScanning) {
                with(result.device) {
                    // Check for Bluetooth Connect permission on Android 12+
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

                    // Stop scan
                    stopScan()
                    isScanning = false

                    println("Found Bluetooth device: ${result.scanRecord?.serviceUuids} $name $address")
                    println("Now attempting to connect to Bluetooth device.")

                    val address = result.device.address
                    if (!isFirstInitializing && address != deviceAddress) {
                        println("Error: Device address does not match paired device address.")
                        return
                    }

                    // Connect to the device
                    isConnecting = true
                    connectDevice = this
                    connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            when (errorCode) {
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> {
                    onScanFailedCallback?.invoke {
                        reset()
                        initialize(context)
                    }
                    println("Scan failed: restart Bluetooth and press Retry")
                }

                else -> {
                    onScanFailedCallback?.invoke {
                        reset()
                        initialize(context)
                    }
                    println("Scan failed with error code $errorCode")
                }
            }
        }
    }

    /**
     * This callback handles results during GATT connections.
     * */
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt?,
            status: Int,
            newState: Int
        ) {
            super.onConnectionStateChange(gatt, status, newState)

            // Check for Bluetooth Connect permission on Android 12+
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
                // Reset connection retry counter
                connectionRetryCount = 0

                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    // Save GATT device for later reference
                    this@BluetoothModel.gatt = gatt

                    println("Connected to device ${gatt!!.device.address}")

                    // Stop scan for devices
                    stopScan()

                    // Start discovering services of GATT device
                    gatt.discoverServices()
                } else {
                    if (isSetupComplete) {
                        // Set connect status to disconnected
                        _connectStatus.value = ConnectStatus.Disconnected

                        // Disable see phone info
                        _isSeePhoneInfoEnabled.value = false

                        // Start scan for devices if disconnected due to issues such as out of range, device powered off etc.
                        startScan()
                    }
                    println("Disconnected from device advertising: ${gatt!!.device.address}")
                }
            } else {
                if (isConnecting) {
                    // Retry connection if it fails (up to 3 times)
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
                        // Run connect failed callback only during setup
                        if (isFirstInitializing) {
                            onConnectFailedCallback?.invoke {
                                reset()
                                initialize(context)
                            }
                        }

                        // Start scan for devices again
                        startScan()

                        println("Error: GATT connection failed with status $status")
                    }
                } else if (isDisconnecting && connectionRetryCount < 3) {
                    // Retry disconnection if it fails (up to 3 times)
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

                // Check if the device has the Cellular service
                if (gatt.services.find { it.uuid.toString() == serviceUUID } != null) {
                    // Check for Bluetooth Connect permission on Android 12+
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

                    // Store the reference to the characteristics for the Cellular service
                    commandCharacteristic = getCharacteristicForDevice(commandCharacteristicUUID)
                    notificationCharacteristic =
                        getCharacteristicForDevice(notificationCharacteristicUUID)

                    // Request for a larger maximum transmission unit (MTU) size
                    gatt.requestMtu(517)
                } else {
                    onUnexpectedErrorCallback?.invoke {
                        reset()
                        initialize(context)
                    }
                    println("Could not find service.")
                }
            } else {
                onUnexpectedErrorCallback?.invoke {
                    reset()
                    initialize(context)
                }
                println("Service discovery failed due to internal error: $status")
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                println("MTU changed to $mtu")

                if (isFirstInitializing) {
                    // Check for Bluetooth Connect permission on Android 12+
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

                    // Pair with device if needed
                    if (connectDevice!!.bondState != BluetoothDevice.BOND_BONDED) {
                        connectDevice!!.createBond()

                        // Connect broadcast receiver to receive updates
                        val intentFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                        context.registerReceiver(BondStateBroadcastReceiver(), intentFilter)
                    } else {
                        onBonded()
                    }
                    return
                } else if (isSharingHotspotDetails) {
                    // Share hotspot details to device
                    shareHotspotDetails2?.invoke()
                    return
                } else if (isSharingPhoneInfo) {
                    // Share phone info to device
                    sharePhoneInfo2?.invoke()
                    return
                } else if (isConnectingToHotspot) {
                    // Request device to connect to hotspot
                    connectHotspot2?.invoke()
                    return
                } else if (isDisconnectingFromHotspot) {
                    // Request device to disconnect from hotspot
                    disconnectHotspot2?.invoke()
                    return
                }

                // Subscribe to notifications on the notification characteristic
                enableNotifications()
            } else {
                onUnexpectedErrorCallback?.invoke {
                    reset()
                    initialize(context)
                }
                println("Error: MTU change failed with status $status")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                println("Write to descriptor of characteristic ${descriptor!!.characteristic.uuid}")

                if (isInitializing) {
                    // Indicate that initialization is complete
                    initOnConnectCallback?.invoke()
                    indicateOperationComplete()
                }

                // Indicate that device is connected
                _connectStatus.value = ConnectStatus.Idle
            } else {
                println("Error: Failed to write to descriptor of characteristic ${descriptor!!.characteristic.uuid} with status $status")
                if (!isDisconnecting) {
                    // Indicate that device is disconnected
                    _connectStatus.value = ConnectStatus.Disconnected
                }
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

                // Run the next operation
                if (isFirstInitializing) {
                    // Store device address
                    deviceAddress = connectDevice!!.address
                    CoroutineScope(Dispatchers.IO).launch {
                        context.dataStore.edit { settings ->
                            settings[deviceAddressKey] = deviceAddress!!
                        }
                    }

                    // Indicate that initialization is complete
                    initOnConnectCallback?.invoke()
                } else if (isSharingHotspotDetails) {
                    onHotspotDetailsSharedCallback?.invoke()
                } else if (isConnectingToHotspot) {
                    // Indicate that hotspot is connected
                    _connectStatus.value = ConnectStatus.Connected
                }

                indicateOperationComplete()
            } else {
                println("Error: Failed to write to characteristic ${characteristic!!.uuid} with status $status")

                if (isFirstInitializing) {
                    val initOnConnectCallback = initOnConnectCallback
                    onConnectFailedCallback?.invoke {
                        reset()
                        isFirstInitializing = true
                        this@BluetoothModel.initOnConnectCallback = initOnConnectCallback
                        initialize(context)
                    }
                } else if (isSharingHotspotDetails) {
                    if (hotspotShareRetryCount < 5) {
                        // Retry sharing hotspot details to device
                        shareHotspotDetails2?.invoke()
                        hotspotShareRetryCount++
                    } else {
                        // Show error message
                        hotspotDetailsRetryCallback?.let {
                            onHotspotDetailsShareFailedCallback?.invoke(it)
                        }
                    }
                } else {
                    indicateOperationComplete()
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)

            if (characteristic != null) {
                val text = String(characteristic.value)
                println("Received data from characteristic ${characteristic.uuid}: $text")

                when (text) {
                    NotificationType.EnableHotspot -> {
                        println("Enabling hotspot...")
                        enableHotspot()
                    }

                    NotificationType.DisableHotspot -> {
                        println("Disabling hotspot...")
                        disableHotspot()
                    }

                    NotificationType.DisableHotspotIndicateOnly -> {
                        println("Disabling hotspot (no bluetooth)...")
                        disableHotspot(noBluetooth = true)
                    }

                    NotificationType.IndicateConnectedHotspot -> {
                        println("Indicating device connected to hotspot...")
                        indicateConnectedHotspot()
                    }

                    NotificationType.EnableSeePhoneInfo -> {
                        println("Enabling see phone info...")
                        _isSeePhoneInfoEnabled.value = true
                    }

                    NotificationType.DisableSeePhoneInfo -> {
                        println("Disabling see phone info...")
                        _isSeePhoneInfoEnabled.value = false
                    }

                    else -> {
                        println("Error: Received unknown payload from characteristic.")
                    }
                }
            }
        }
    }

    fun onBonded() {
        // Continue next part of setup (sending Hello World command)
        initialize2()
    }

    fun onBondingFailed() {
        println("Error: Failed to bond to device ${connectDevice!!.address}")

        val initOnConnectCallback = initOnConnectCallback
        onBondFailedCallback?.invoke {
            reset()
            isFirstInitializing = true
            this@BluetoothModel.initOnConnectCallback = initOnConnectCallback
            initialize(context)
        }
    }

    private val myHotspotOnStartTetheringCallback = object : HotspotOnStartTetheringCallback() {
        override fun onTetheringStarted() {
            this@BluetoothModel.onTetheringStarted()
        }

        override fun onTetheringFailed() {
            println("Hotspot failed to start")
            onHotspotFailedCallback?.invoke {
                enableHotspot()
            }
        }
    }

    private fun onTetheringStarted() {
        println("Hotspot started")

        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                if (gatt == null) {
                    println("Error: Device is unavailable.")
                    return
                }
                if (commandCharacteristic == null) {
                    println("Error: Command characteristic is unavailable")
                    return
                }

                // Check for Bluetooth Connect permission on Android 12+
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
                    isConnectingToHotspot = true

                    connectHotspot2 = {
                        commandCharacteristic!!.writeType =
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        commandCharacteristic!!.value = CommandType.ConnectToHotspot.toByteArray()
                        gatt!!.writeCharacteristic(commandCharacteristic!!)
                    }

                    if (bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
                            .find { it.address == connectDevice?.address } == null
                    ) {
                        startScan()
                    } else {
                        connectHotspot2!!.invoke()
                    }
                }
            }
        }, 3000)
    }

    private fun onTetheringStopped() {
        println("Hotspot stopped")

        if (gatt == null) {
            println("Error: Device is unavailable.")
            return
        }
        if (commandCharacteristic == null) {
            println("Error: Command characteristic is unavailable")
            return
        }

        // Check for Bluetooth Connect permission on Android 12+
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
            isDisconnectingFromHotspot = true

            disconnectHotspot2 = {
                commandCharacteristic!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                commandCharacteristic!!.value = CommandType.DisconnectFromHotspot.toByteArray()
                gatt!!.writeCharacteristic(commandCharacteristic!!)
            }

            if (bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
                    .find { it.address == connectDevice?.address } == null
            ) {
                startScan()
            } else {
                disconnectHotspot2!!.invoke()
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
        operationQueue.poll()?.let {
            isRunningOperation = true

            it.invoke()
        }
    }

    @Synchronized
    private fun indicateOperationComplete() {
        isRunningOperation = false

        // Reset indicators of commands
        isFirstInitializing = false
        isInitializing = false
        initOnConnectCallback = null
        isSharingHotspotDetails = false
        isSharingPhoneInfo = false
        isConnectingToHotspot = false
        isDisconnectingFromHotspot = false
        onHotspotDetailsSharedCallback = null

        doNextOperation()
    }

    /**
     * This function initialises a new Bluetooth connection with a device running the Cellular app using data from a QR code.
     * @param serviceUUID The BLE service UUID of the device running the Cellular app.
     * @param sharedKey The shared key of the device running the Cellular app.
     * @param initOnConnectCallback The callback to run when the connection is successfully initialised.
     * @param context The context of the application that calls this function.
     * */
    suspend fun initializeFromQR(
        serviceUUID: String,
        sharedKey: String,
        initOnConnectCallback: () -> (Unit),
        context: Context
    ) {
        // Validate service UUID and Key
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

        // Store service UUID and Key in DataStore
        context.dataStore.edit { settings ->
            settings[serviceUUIDKey] = serviceUUID.lowercase()
            settings[sharedKeyKey] = sharedKey
        }

        isFirstInitializing = true
        this.initOnConnectCallback = initOnConnectCallback
        this.serviceUUID = serviceUUID.lowercase()
        this.sharedKey = sharedKey
        initialize(context)
    }

    /**
     * This function initialises a Bluetooth connection with a device running the Cellular app using stored credentials.
     * @param context The context of the application that calls this function.
     * */
    suspend fun initializeFromDataStore(initOnConnectCallback: () -> (Unit), context: Context) {
        // Retrieve service UUID and Key from DataStore
        serviceUUID = context.dataStore.data.map { settings ->
            settings[serviceUUIDKey] ?: ""
        }.first()
        sharedKey = context.dataStore.data.map { settings ->
            settings[sharedKeyKey] ?: ""
        }.first()
        deviceAddress = context.dataStore.data.map { settings ->
            settings[deviceAddressKey] ?: ""
        }.first()
        isSetupComplete = context.dataStore.data.map { settings ->
            settings[isSetupCompleteKey] ?: false
        }.first()

        if (serviceUUID == "") {
            println("Error: Service UUID not found in DataStore.")
            return
        }

        if (sharedKey == "") {
            println("Error: Shared Key not found in DataStore.")
            return
        }

        if (deviceAddress == "") {
            println("Error: Device Address not found in DataStore.")
            return
        }

        isInitializing = true
        this.initOnConnectCallback = initOnConnectCallback
        initialize(context)
    }

    /**
     * This function starts the scan for BLE devices when initialising.
     * */
    private fun initialize(context: Context) {
        // Initialise context and BluetoothManager
        this.context = context
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        aes = AES(sharedKey!!)
        wifiHotspotManager = WifiHotspotManager(context)

        // Start BLE scan
        startScan()
    }

    /**
     * This function continues the second part of setup - sending the Hello World command to the device.
     * */
    private fun initialize2() {
        // Check for Bluetooth Connect permission on Android 12+
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

        // Write Hello World command to command characteristic
        commandCharacteristic!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        commandCharacteristic!!.value = CommandType.HelloWorld.toByteArray()
        gatt!!.writeCharacteristic(commandCharacteristic!!)
    }

    /**
     * This function starts the scan for BLE devices matching the set service UUID. The serviceUUID variable has to be set before calling this function.
     * */
    private fun startScan() {
        // Reset BLE connection before starting scan
        reset()

        // Scan for BLE devices advertising this service UUID
        val filter =
            ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(serviceUUID)).build()
        val scanSettings = ScanSettings.Builder().apply {
            if (isFirstInitializing) {
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

    /**
     * This function stops the scan for BLE devices if it is running.
     * */
    private fun stopScan() {
        if (isScanning) {
            // Check for Bluetooth Connect permission on Android 12+
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

            // Stop BLE scan
            bluetoothAdapter.bluetoothLeScanner.stopScan(bleScanCallback)
        }
    }

    /**
     * This function gets a characteristic reference based on its UUID from the GATT device. The GATT device's services must be discovered before calling this function.
     * */
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

    private fun enableNotifications() {
        if (gatt == null) {
            println("Error: Device is unavailable.")
            return
        }
        if (notificationCharacteristic == null) {
            println("Error: Notification characteristic is unavailable.")
            return
        }

        // Check for Bluetooth Connect permission on Android 12+
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

        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val descriptor = notificationCharacteristic!!.getDescriptor(cccdUuid)
        gatt!!.setCharacteristicNotification(notificationCharacteristic!!, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt!!.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
        } else {
            descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            gatt!!.writeDescriptor(descriptor)
        }
    }

    /**
     * This function shares the hotspot details with the device running the Cellular app. This is a public function to enqueue this operation.
     * @param ssid The SSID of the hotspot to connect to.
     * @param password The password of the hotspot to connect to.
     * @param onHotspotDetailsSharedCallback The callback function to be called when the hotspot details have been shared.
     * */
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

        // Check for Bluetooth Connect permission on Android 12+
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

        // Enqueue operation to share hotspot details
        enqueueOperation {
            startShareHotspotDetails(ssid, password, onHotspotDetailsSharedCallback)
        }
    }

    /**
     * This function shares the hotspot details with the device running the Cellular app.
     * @param ssid The SSID of the hotspot to connect to.
     * @param password The password of the hotspot to connect to.
     * @param onHotspotDetailsSharedCallback The callback function to be called when the hotspot details have been shared.
     * */
    @SuppressLint("MissingPermission")
    private fun startShareHotspotDetails(
        ssid: String,
        password: String,
        onHotspotDetailsSharedCallback: () -> Unit
    ) {
        isSharingHotspotDetails = true
        this.onHotspotDetailsSharedCallback = onHotspotDetailsSharedCallback

        shareHotspotDetails2 = {
            commandCharacteristic!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            val (cipherText, iv) = aes.encrypt("\"$ssid\" \"$password\"")
            commandCharacteristic!!.value =
                "${CommandType.ShareHotspotDetails} $iv $cipherText".toByteArray()
            gatt!!.writeCharacteristic(commandCharacteristic!!)
        }

        hotspotDetailsRetryCallback = {
            reset()
            startShareHotspotDetails(ssid, password, onHotspotDetailsSharedCallback)
        }

        if (bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
                .find { it.address == connectDevice?.address } == null
        ) {
            startScan()
        } else {
            shareHotspotDetails2!!.invoke()
        }
    }

    suspend fun markSetupComplete() {
        isSetupComplete = true
        context.dataStore.edit { settings ->
            settings[isSetupCompleteKey] = true
        }
    }

    fun sharePhoneInfo(signalLevel: Int, networkType: String, batteryPercentage: Int) {
        if (gatt == null) {
            println("Error: Device is unavailable.")
            return
        }
        if (commandCharacteristic == null) {
            println("Error: Command characteristic is unavailable")
            return
        }

        // Check for Bluetooth Connect permission on Android 12+
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

        // Enqueue operation to share hotspot details
        enqueueOperation {
            startSharePhoneInfo(signalLevel, networkType, batteryPercentage)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startSharePhoneInfo(signalLevel: Int, networkType: String, batteryPercentage: Int) {
        isSharingPhoneInfo = true

        sharePhoneInfo2 = {
            commandCharacteristic!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            commandCharacteristic!!.value =
                "${CommandType.SharePhoneInfo} $signalLevel $networkType $batteryPercentage".toByteArray()
            gatt!!.writeCharacteristic(commandCharacteristic!!)
        }

        if (bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
                .find { it.address == connectDevice?.address } == null
        ) {
            startScan()
        } else {
            sharePhoneInfo2!!.invoke()
        }
    }

    fun enableHotspot() {
        // Indicate that hotspot is connecting
        _connectStatus.value = ConnectStatus.Connecting

        if (wifiHotspotManager.isTetherActive) {
            onTetheringStarted()
        } else {
            wifiHotspotManager.startTethering(myHotspotOnStartTetheringCallback)
        }
    }

    fun disableHotspot(noBluetooth: Boolean = false) {
        // Indicate that hotspot is disconnected
        _connectStatus.value = ConnectStatus.Idle

        // Disconnect from hotspot
        if (wifiHotspotManager.isHotspotStartedByUs && wifiHotspotManager.isTetherActive) {
            wifiHotspotManager.stopTethering()
        }

        if (!noBluetooth) {
            onTetheringStopped()
        }
    }

    fun indicateConnectedHotspot() {
        _connectStatus.value = ConnectStatus.Connected
    }

    /**
     * This function registers callbacks from a view model for error handling.
     * @param onScanFailedCallback The callback function to be called when BLE scans fail.
     * @param onUnexpectedErrorCallback The callback function to be called when an unexpected error occurs.
     * @param onHotspotDetailsShareFailedCallback The callback function to be called when sharing hotspot details fails.
     * @param onConnectFailedCallback The callback function to be called when connecting to a device fails.
     * */
    fun registerForErrorHandling(
        onScanFailedCallback: (() -> Unit) -> Unit,
        onUnexpectedErrorCallback: (() -> Unit) -> Unit,
        onHotspotDetailsShareFailedCallback: (() -> Unit) -> Unit,
        onConnectFailedCallback: (() -> Unit) -> Unit,
        onBondFailedCallback: (() -> Unit) -> Unit,
        onHotspotFailedCallback: (() -> Unit) -> Unit
    ) {
        this.onScanFailedCallback = onScanFailedCallback
        this.onUnexpectedErrorCallback = onUnexpectedErrorCallback
        this.onConnectFailedCallback = onConnectFailedCallback
        this.onBondFailedCallback = onBondFailedCallback
        this.onHotspotDetailsShareFailedCallback = onHotspotDetailsShareFailedCallback
        this.onHotspotFailedCallback = onHotspotFailedCallback
    }

    /**
     * This function resets the BLE connection to allow reconnecting to a BLE device.
     * */
    private fun reset() {
        stopScan()

        connectDevice = null
        gatt = null
        commandCharacteristic = null
        notificationCharacteristic = null
        isScanning = false
        isConnecting = false
        isDisconnecting = false
        connectionRetryCount = 0
        hotspotShareRetryCount = 0
    }
}