package com.xuanhan.cellularcompanion.viewmodels

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import kotlinx.coroutines.flow.MutableStateFlow

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
                        isSpecialPermissionGranted.value = Settings.System.canWrite(it)
                    }
                }
            }
        }
    }

    fun grantPermission(context: Context) {
        if (isSpecialPermission) {
            when (status.permission) {
                Manifest.permission.WRITE_SETTINGS -> {
                    if (!Settings.System.canWrite(context)) {
                        val writeSettingsIntent = Intent().apply {
                            action = Settings.ACTION_MANAGE_WRITE_SETTINGS
                            data = Uri.parse("package:" + context.packageName)
                        }
                        specialPermissionLauncher?.launch(writeSettingsIntent)
                    }
                }
            }
        } else {
            status.launchPermissionRequest()
        }
    }
}