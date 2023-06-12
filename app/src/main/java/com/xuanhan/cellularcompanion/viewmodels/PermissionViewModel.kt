package com.xuanhan.cellularcompanion.viewmodels

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted

class PermissionViewModel @OptIn(ExperimentalPermissionsApi::class) constructor(
    val title: String,
    val description: String?,
    val status: PermissionState,
    val altStatus: PermissionState? = null,
) {
    @OptIn(ExperimentalPermissionsApi::class)
    fun grantPermission() {
        status.launchPermissionRequest()
        if (status.status.isGranted && altStatus != null) {
            altStatus.launchPermissionRequest()
        }
    }
}