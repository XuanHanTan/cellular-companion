package com.xuanhan.cellularcompanion.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

/**
 * This class is the view model for the Start screen.
 */
class StartViewModel {
    /**
     * This function checks that the required permissions have been granted.
     */
    fun checkPermissions(context: Context): Boolean {
        val permissions = ArrayList<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.READ_PHONE_STATE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        if (!PermissionViewModel.getWriteSettingsGranted(context)) {
            return false
        }

        if (!PermissionViewModel.getIgnoreBatteryOptimizationsGranted(context)) {
            return false
        }

        return true
    }
}