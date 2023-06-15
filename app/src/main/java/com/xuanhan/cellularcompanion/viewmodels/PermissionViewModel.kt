package com.xuanhan.cellularcompanion.viewmodels

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState

class PermissionViewModel @OptIn(ExperimentalPermissionsApi::class) constructor(
    val title: String,
    val description: String?,
    val status: PermissionState,
) {
    @OptIn(ExperimentalPermissionsApi::class)
    fun grantPermission() {
        status.launchPermissionRequest()
    }
}