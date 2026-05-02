package com.example.beaconattendance

import android.content.Context
import android.provider.Settings
fun createLogData(context: Context, action: String): LogData {
    return LogData(
        device_address = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ),
        action = action,
    )
}
