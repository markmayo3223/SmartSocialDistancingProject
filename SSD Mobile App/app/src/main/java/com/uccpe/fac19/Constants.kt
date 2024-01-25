package com.uccpe.fac19

object Constants {

    const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
    const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"
    const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
    const val ACTION_SHOW_MAPS_FRAGMENT = "ACTION_SHOW_MAPS_FRAGMENT"

    const val NOTIFICATION_CHANNEL_ID = "location_channel"
    const val NOTIFICATION_CHANNEL_NAME = "location"
    const val NOTIFICATION_ID = 1

    const val WARNING_NOTIFICATION_CHANNEL_ID = "warning_channel"
    const val WARNING_NOTIFICATION_CHANNEL_NAME = "warning"
    const val WARNING_MESSAGE = "Warning: Maintain social distancing! An instance violating 1-meter distance rule has been added to your contacts list."
    const val WARNING_NOTIFICATION_ID = 11

    const val INTERVAL: Long = 2000
    const val FASTEST_INTERVAL: Long = 1000

}