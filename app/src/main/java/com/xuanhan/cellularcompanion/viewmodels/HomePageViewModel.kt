package com.xuanhan.cellularcompanion.viewmodels

import android.content.Context
import android.os.Parcelable
import com.xuanhan.cellularcompanion.MainActivity
import com.xuanhan.cellularcompanion.bluetoothModel
import kotlinx.parcelize.Parcelize

@Parcelize
class HomePageViewModel: Parcelable {
    suspend fun prepareBluetooth(context: Context) {
        bluetoothModel.initializeFromDataStore({
            val mainActivity = context.findActivity() as MainActivity
            mainActivity.connectService()
        }, context.applicationContext)
    }
}