package com.xuanhan.cellularcompanion.utilities

import android.content.Context
import android.net.ConnectivityManager
import android.os.Handler
import android.util.Log
import com.android.dx.stock.ProxyBuilder
import java.lang.reflect.Method

/**
 * Created by jonro on 19/03/2018.
 */
class WifiHotspotManager(private val mContext: Context) {
    private val mConnectivityManager: ConnectivityManager = mContext.getSystemService(ConnectivityManager::class.java) as ConnectivityManager

    var isHotspotStartedByUs = false
        private set
    val isTetherActive: Boolean
        /**
         * Checks where tethering is on.
         * This is determined by the getTetheredIfaces() method,
         * that will return an empty array if not devices are tethered
         *
         * @return true if a tethered device is found, false if not found
         */
        get() {
            try {
                val method = mConnectivityManager.javaClass.getDeclaredMethod("getTetheredIfaces")
                val res = method.invoke(mConnectivityManager) as Array<String>
                Log.d(TAG, "getTetheredIfaces invoked")
                Log.d(TAG, res.contentToString())
                if (res.isNotEmpty()) {
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in getTetheredIfaces")
                e.printStackTrace()
            }
            return false
        }

    /**
     * This enables tethering using the ssid/password defined in Settings App>Hotspot & tethering
     * Does not require app to have system/privileged access
     * Credit: Vishal Sharma - https://stackoverflow.com/a/52219887
     */
    fun startTethering(callback: HotspotOnStartTetheringCallback): Boolean {

        // On Pie if we try to start tethering while it is already on, it will
        // be disabled. This is needed when startTethering() is called programmatically.
        if (isTetherActive) {
            Log.d(TAG, "Tether already active, returning")
            return false
        }
        val outputDir = mContext.codeCacheDir
        val proxy = try {
            ProxyBuilder.forClass(onStartTetheringCallback())
                .dexCache(outputDir).handler { proxy, method, args ->
                    when (method.name) {
                        "onTetheringStarted" -> callback.onTetheringStarted()
                        "onTetheringFailed" -> callback.onTetheringFailed()
                        else -> ProxyBuilder.callSuper(proxy, method, *args)
                    }
                    null
                }.build()
        } catch (e: Exception) {
            Log.e(TAG, "Error in enableTethering ProxyBuilder")
            e.printStackTrace()
            return false
        }
        val method: Method?
        try {
            method = mConnectivityManager.javaClass.getDeclaredMethod(
                "startTethering",
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                onStartTetheringCallback(),
                Handler::class.java
            )
            if (method == null) {
                Log.e(TAG, "startTetheringMethod is null")
            } else {
                method.invoke(
                    mConnectivityManager,
                    ConnectivityManager.TYPE_MOBILE,
                    false,
                    proxy,
                    null
                )
                Log.d(TAG, "startTethering invoked")
            }
            isHotspotStartedByUs = true
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error in enableTethering")
            e.printStackTrace()
        }
        return false
    }

    fun stopTethering() {
        try {
            val method = mConnectivityManager.javaClass.getDeclaredMethod(
                "stopTethering",
                Int::class.javaPrimitiveType
            )
            method.invoke(mConnectivityManager, ConnectivityManager.TYPE_MOBILE)
            isHotspotStartedByUs = false
            Log.d(TAG, "stopTethering invoked")
        } catch (e: Exception) {
            Log.e(TAG, "stopTethering error: $e")
            e.printStackTrace()
        }
    }

    private fun onStartTetheringCallback(): Class<*>? {
        try {
            return Class.forName("android.net.ConnectivityManager\$OnStartTetheringCallback")
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "OnStartTetheringCallbackClass error: $e")
            e.printStackTrace()
        }
        return null
    }

    companion object {
        private val TAG = WifiHotspotManager::class.java.simpleName
    }
}