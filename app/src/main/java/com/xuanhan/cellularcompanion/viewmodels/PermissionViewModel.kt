package com.xuanhan.cellularcompanion.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * This class is the view model for the Permission screen.
 */
@OptIn(ExperimentalPermissionsApi::class)
class PermissionViewModel @OptIn(ExperimentalPermissionsApi::class) constructor(
    val title: String,
    val description: String?,
    val status: PermissionState,
    val isOptionalPermission: Boolean = false,
    val isSpecialPermission: Boolean = false,
    val specialPermissionLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>? = null,
    val context: Context? = null
) {
    val isSpecialPermissionGranted = MutableStateFlow(false)

    init {
        if (isSpecialPermission) {
            when (status.permission) {
                Manifest.permission.WRITE_SETTINGS -> {
                    context?.let {
                        isSpecialPermissionGranted.value = getWriteSettingsGranted(it)
                    }
                }

                Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> {
                    context?.let {
                        isSpecialPermissionGranted.value = getIgnoreBatteryOptimizationsGranted(it)
                    }
                }
            }
        }
    }

    companion object {
        fun getWriteSettingsGranted(context: Context): Boolean {
            return Settings.System.canWrite(context)
        }

        fun getIgnoreBatteryOptimizationsGranted(context: Context): Boolean {
            return (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(
                context.packageName
            )
        }
    }

    @SuppressLint("BatteryLife")
    fun grantPermission(context: Context) {
        if (isSpecialPermission) {
            println(status.permission)
            when (status.permission) {
                Manifest.permission.WRITE_SETTINGS -> {
                    if (!getWriteSettingsGranted(context)) {
                        val writeSettingsIntent = Intent().apply {
                            action = Settings.ACTION_MANAGE_WRITE_SETTINGS
                            data = Uri.parse("package:" + context.packageName)
                        }
                        specialPermissionLauncher?.launch(writeSettingsIntent)
                    }
                }
                Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> {
                    if (!getIgnoreBatteryOptimizationsGranted(context)) {
                        val ignoreBatteryOptimizationIntent = Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            data = Uri.parse("package:" + context.packageName)
                        }
                        specialPermissionLauncher?.launch(ignoreBatteryOptimizationIntent)
                    }
                }
            }
        } else {
            status.launchPermissionRequest()
        }
    }
}