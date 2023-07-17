package com.xuanhan.cellularcompanion.utilities

abstract class HotspotOnStartTetheringCallback {
    /**
     * Called when tethering has been successfully started.
     */
    abstract fun onTetheringStarted()

    /**
     * Called when starting tethering failed.
     */
    abstract fun onTetheringFailed()
}