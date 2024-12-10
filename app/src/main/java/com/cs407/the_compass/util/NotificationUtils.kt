package com.cs407.the_compass.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationUtils {
    const val CHANNEL_ID = "navigation_channel"
    private const val CHANNEL_NAME = "Navigation Service"
    private const val CHANNEL_DESC = "Shows distance and direction during navigation."

    //alert notification
    const val SIGNAL_CHANNEL_ID = "signal_alert_channel"
    private const val SIGNAL_CHANNEL_NAME = "Signal Alerts"
    private const val SIGNAL_CHANNEL_DESC = "Notifications for cellular signal changes"
    const val SIGNAL_NOTIFICATION_ID = 2

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            //alert
            val signalChannel = NotificationChannel(
                SIGNAL_CHANNEL_ID,
                SIGNAL_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = SIGNAL_CHANNEL_DESC
            }
            manager.createNotificationChannel(signalChannel)
        }
    }
}
