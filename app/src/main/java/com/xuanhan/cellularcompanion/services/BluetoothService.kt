package com.xuanhan.cellularcompanion.services

import android.Manifest
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.xuanhan.cellularcompanion.bluetoothModel
import com.xuanhan.cellularcompanion.broadcastreceivers.BluetoothStateBroadcastReceiver
import com.xuanhan.cellularcompanion.createBluetoothNotification
import com.xuanhan.cellularcompanion.isBluetoothEnabled
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import kotlin.math.floor


class BluetoothService : Service() {
    private val binder = BluetoothServiceBinder()
    private val btStateBroadcastReceiver = BluetoothStateBroadcastReceiver()
    private val batteryLevelTimer = Timer()
    private val getBatteryInfoCoroutineScope = CoroutineScope(Dispatchers.IO + CoroutineName("GetBatteryInfo"))
    private val getBluetoothStateCoroutineScope = CoroutineScope(Dispatchers.IO + CoroutineName("GetBluetoothState"))
    private val getNetworkInfoCoroutineScope = CoroutineScope(Dispatchers.IO + CoroutineName("GetNetworkInfo"))
    private val updateStatusNotificationCoroutineScope = CoroutineScope(Dispatchers.IO + CoroutineName("UpdateStatusNotification"))
    private var prevModifiedSignalStrengthLevel = -1
    private var prevNetworkType = ""
    private var prevBatteryPercentage = -1
    private lateinit var telephonyManager: TelephonyManager
    private val telephonyCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener,
            TelephonyCallback.DisplayInfoListener {
            override fun onSignalStrengthsChanged(p0: SignalStrength) {
                handleSignalStrengthLevelChanged(p0.level)
            }

            override fun onDisplayInfoChanged(p0: TelephonyDisplayInfo) {
                handleNetworkTypeChanged(p0.networkType, p0.overrideNetworkType)
            }
        }
    else
        object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(p0: SignalStrength) {
                handleSignalStrengthLevelChanged(p0.level)

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    if (ActivityCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.READ_PHONE_STATE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        println("Error: Read phone state permission not granted")
                        return
                    }

                    val networkType = telephonyManager.networkType
                    handleNetworkTypeChanged(networkType, 0)
                }
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onDisplayInfoChanged(p0: TelephonyDisplayInfo) {
                handleNetworkTypeChanged(p0.networkType, p0.overrideNetworkType)
            }
        }
    private var started = false
    private var btStateBroadcastReceiverRegistered = false
    private var isSeePhoneInfoEnabled = false

    private fun handleSignalStrengthLevelChanged(signalStrengthLevel: Int) {
        val modifiedSignalStrengthLevel =
            floor(((signalStrengthLevel.toDouble() + 1) / 5 * 4) + 0.5).toInt() - 1
        if (modifiedSignalStrengthLevel != prevModifiedSignalStrengthLevel) {
            println("Modified level $modifiedSignalStrengthLevel")

            bluetoothModel.sharePhoneInfo(modifiedSignalStrengthLevel, "-1", -1)
        }
        prevModifiedSignalStrengthLevel = modifiedSignalStrengthLevel
    }

    private fun handleNetworkTypeChanged(networkType: Int, overrideNetworkType: Int) {
        val networkTypeString = if (overrideNetworkType == 0) {
            when (networkType) {
                TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_1xRTT -> "GPRS"

                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_CDMA,
                TelephonyManager.NETWORK_TYPE_IDEN,
                TelephonyManager.NETWORK_TYPE_GSM -> "E"

                TelephonyManager.NETWORK_TYPE_UMTS,
                TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_EVDO_B,
                TelephonyManager.NETWORK_TYPE_EHRPD,
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "3G"

                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyManager.NETWORK_TYPE_IWLAN, 19 -> "4G"

                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                else -> ""
            }
        } else {
            when (overrideNetworkType) {
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> "4G"
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> "5Ge"
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> "5G"
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> "5G+"
                else -> ""
            }
        }

        if (networkTypeString != prevNetworkType) {
            println("Network type $networkTypeString")
            bluetoothModel.sharePhoneInfo(-1, networkTypeString, -1)
        }

        prevNetworkType = networkTypeString
    }

    private fun handleBatteryPercentageChanged(batteryPercentage: Int, immediate: Boolean) {
        if (isSeePhoneInfoEnabled) {
            if (batteryPercentage != prevBatteryPercentage || immediate) {
                println("Updating battery level $batteryPercentage")
                bluetoothModel.sharePhoneInfo(-1, "-1", batteryPercentage)
            }
        }

        prevBatteryPercentage = batteryPercentage
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class BluetoothServiceBinder : Binder() {
        fun getService(): BluetoothService {
            return this@BluetoothService
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!started) {
            println("Service started")

            telephonyManager =
                applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            if (isBluetoothEnabled.value) {
                getBatteryInfoCoroutineScope.launch {
                    println("Initialising Bluetooth model now...")
                    initBluetoothModel()
                }
            }

            getBluetoothStateCoroutineScope.launch {
                var prevBluetoothEnabled = isBluetoothEnabled.value
                isBluetoothEnabled.collect {
                    if (it && !prevBluetoothEnabled) {
                        println("Reinitialising Bluetooth model now...")
                        initBluetoothModel()
                    } else if (!it && prevBluetoothEnabled) {
                        bluetoothModel.disconnect()
                    }
                    prevBluetoothEnabled = it
                }
            }

            getNetworkInfoCoroutineScope.launch {
                bluetoothModel.isSeePhoneInfoEnabled.collect {
                    isSeePhoneInfoEnabled = it

                    if (isSeePhoneInfoEnabled) {
                        startSharePhoneInfo()
                        getBatteryPercentage(immediate = true)
                    } else {
                        prevModifiedSignalStrengthLevel = -1
                        prevNetworkType = ""
                        prevBatteryPercentage = -1
                        disposePhoneStateListeners()
                    }
                }
            }

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                updateStatusNotificationCoroutineScope.launch {
                    bluetoothModel.connectStatus.collect {
                        val notificationManager = applicationContext
                            .getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                        notificationManager.notify(1, createBluetoothNotification(connectStatus = it, context = applicationContext))
                    }
                }
            }

            startForeground(1, createBluetoothNotification(connectStatus = bluetoothModel.connectStatus.value, context = applicationContext))

            if (btStateBroadcastReceiverRegistered) {
                unregisterReceiver(btStateBroadcastReceiver)
            }
            registerReceiver(btStateBroadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

            btStateBroadcastReceiverRegistered = true
            started = true
        }

        return START_STICKY
    }

    private suspend fun initBluetoothModel() {
        bluetoothModel.initializeFromDataStore(
            {
                batteryLevelTimer.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        getBatteryPercentage()
                    }
                }, 0, 300000)

                if (isSeePhoneInfoEnabled) {
                    startSharePhoneInfo()
                }
            }, applicationContext
        )
    }

    private fun getBatteryPercentage(immediate: Boolean = false) {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let {
            applicationContext.registerReceiver(null, it)
        }
        val batteryPct: Int? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            (level * 100 / scale.toFloat()).toInt()
        }
        println("Battery percentage $batteryPct")
        handleBatteryPercentageChanged(batteryPct ?: 100, immediate)
    }

    private fun startSharePhoneInfo() {
        println("Start share phone info")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.registerTelephonyCallback(
                applicationContext.mainExecutor,
                telephonyCallback as TelephonyCallback
            )
        } else {
            telephonyManager.listen(
                telephonyCallback as PhoneStateListener,
                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
            )
        }
    }

    private fun disposePhoneStateListeners() {
        println("Stop share phone info")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.unregisterTelephonyCallback(telephonyCallback as TelephonyCallback)
        } else {
            telephonyManager.listen(
                telephonyCallback as PhoneStateListener,
                PhoneStateListener.LISTEN_NONE
            )
        }
    }

    override fun onDestroy() {
        if (started) {
            batteryLevelTimer.cancel()
            disposePhoneStateListeners()
            this.unregisterReceiver(btStateBroadcastReceiver)
            getBatteryInfoCoroutineScope.cancel()
            getBluetoothStateCoroutineScope.cancel()
            getNetworkInfoCoroutineScope.cancel()
            updateStatusNotificationCoroutineScope.cancel()
            prevModifiedSignalStrengthLevel = -1
            prevNetworkType = ""
            prevBatteryPercentage = -1
            isSeePhoneInfoEnabled = false
            started = false

            println("Service stopped")
        }

        super.onDestroy()
    }
}