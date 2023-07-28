package com.xuanhan.cellularcompanion.viewmodels

import com.xuanhan.cellularcompanion.bluetoothModel

class HomePageViewModel {
    fun enableHotspot() {
        bluetoothModel.enableHotspot()
    }

    fun disableHotspot() {
        bluetoothModel.disableHotspot()
    }
}