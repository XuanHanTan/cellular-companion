package com.xuanhan.cellularcompanion.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
import com.xuanhan.cellularcompanion.MainActivity
import com.xuanhan.cellularcompanion.models.BluetoothModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

class BluetoothService : Service() {
    private val binder = BluetoothServiceBinder()
    private lateinit var bluetoothModel: BluetoothModel
    private val batteryLevelTimer = Timer()
    private var prevSignalStrengthLevel = -1
    private var prevNetworkType = ""
    private var prevBatteryPercentage = -1
    private val telephonyCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener,
            TelephonyCallback.DisplayInfoListener {
            override fun onSignalStrengthsChanged(p0: SignalStrength) {
                handleSignalStrengthLevelChanged(p0.level)
            }

            override fun onDisplayInfoChanged(p0: TelephonyDisplayInfo) {
                handleNetworkTypeChanged(p0.networkType)
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

                    val telephonyManager =
                        getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    val networkType = telephonyManager.networkType
                    handleNetworkTypeChanged(networkType)
                }
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onDisplayInfoChanged(p0: TelephonyDisplayInfo) {
                handleNetworkTypeChanged(p0.networkType)
            }
        }

    private fun handleSignalStrengthLevelChanged(signalStrengthLevel: Int) {
        if (signalStrengthLevel != prevSignalStrengthLevel) {
            println("Level $signalStrengthLevel")
            bluetoothModel.sharePhoneInfo(signalStrengthLevel, "", -1)
        }
        prevSignalStrengthLevel = signalStrengthLevel
    }

    private fun handleNetworkTypeChanged(networkType: Int) {
        val networkTypeString = when (networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
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

        if (networkTypeString != prevNetworkType) {
            println("Network type $networkTypeString")
            bluetoothModel.sharePhoneInfo(-1, networkTypeString, -1)
        }

        prevNetworkType = networkTypeString
    }

    private fun handleBatteryPercentageChanged(batteryPercentage: Int) {
        if (batteryPercentage != prevBatteryPercentage && (batteryPercentage - 5) % 10 == 0) {
            println("Updating battery level $batteryPercentage")
            bluetoothModel.sharePhoneInfo(-1, "", batteryPercentage)
        }

        prevBatteryPercentage = batteryPercentage
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class BluetoothServiceBinder : Binder() {
        fun getService(bluetoothModel: BluetoothModel): BluetoothService {
            this@BluetoothService.bluetoothModel = bluetoothModel

            CoroutineScope(Dispatchers.IO).launch {
                bluetoothModel.initializeFromDataStore({
                    startSharePhoneInfo()
                }, applicationContext)
            }

            return this@BluetoothService
        }
    }

    override fun onBind(intent: Intent): IBinder {
        val name = "Bluetooth Service"
        val descriptionText =
            "Allows Cellular Companion to communicate with your Mac in the background."
        val importance = NotificationManager.IMPORTANCE_LOW
        val mChannel = NotificationChannel("bluetooth_service", name, importance)
        mChannel.description = descriptionText
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        val notification: Notification = Notification.Builder(this, "bluetooth_service")
            .setContentTitle("Cellular Companion")
            .setContentText("The Bluetooth service is running. Tap to view details")
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        return binder
    }

    private fun startSharePhoneInfo() {
        val telephonyManager: TelephonyManager =
            applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.registerTelephonyCallback(
                applicationContext.mainExecutor,
                telephonyCallback as TelephonyCallback
            )
        } else {
            telephonyManager.listen(
                telephonyCallback as PhoneStateListener, /*PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED or*/
                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
            )
        }

        batteryLevelTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let {
                    applicationContext.registerReceiver(null, it)
                }
                val batteryPct: Int? = batteryStatus?.let { intent ->
                    val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    (level * 100 / scale.toFloat()).toInt()
                }
                println("Battery percentage $batteryPct")
                handleBatteryPercentageChanged(batteryPct ?: 100)
            }
        }, 0, 300000)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        batteryLevelTimer.cancel()
        return super.onUnbind(intent)
    }
}