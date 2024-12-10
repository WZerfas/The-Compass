package com.cs407.the_compass.util

import android.app.Service
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.cs407.the_compass.MainActivity
import com.cs407.the_compass.R
import android.content.pm.ServiceInfo
import androidx.annotation.RequiresApi

class SignalMonitorService : Service() {
    // Signal-related fields
    private var lastSignalState: Boolean = true
    private lateinit var telephonyManager: TelephonyManager
    private var telephonyCallback: TelephonyCallback? = null
    private var isMonitoring = false

    companion object {
        private const val FOREGROUND_SERVICE_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, SignalMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SignalMonitorService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        startForegroundServiceWithNotification()
        setupSignalMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSignalMonitoring()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Foreground service handling
    private fun startForegroundServiceWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(FOREGROUND_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(FOREGROUND_SERVICE_ID, notification)
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, NotificationUtils.SIGNAL_CHANNEL_ID)
        .setContentTitle("Reception Alert is On")
        .setContentText("Notify When Signal Changed")
        .setSmallIcon(R.drawable.navigation_icon)
        .setContentIntent(createPendingIntent())
        .setSilent(true)
        .setOngoing(true)
        .build()

    private fun createPendingIntent(): PendingIntent {
        val notificationIntent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Signal monitoring setup and handling
    private fun setupSignalMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setupModernSignalMonitoring()
        } else {
            stopSelf()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupModernSignalMonitoring() {
        class SignalCallback : TelephonyCallback(), TelephonyCallback.ServiceStateListener {
            override fun onServiceStateChanged(state: ServiceState) {
                handleSignalChange(state.state == ServiceState.STATE_IN_SERVICE)
            }
        }

        val callback = SignalCallback()
        telephonyCallback = callback
        try {
            telephonyManager.registerTelephonyCallback(mainExecutor, callback)
            isMonitoring = true
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun stopSignalMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { callback ->
                telephonyManager.unregisterTelephonyCallback(callback)
            }
        }
        isMonitoring = false
    }

    // Signal change handling and notifications
    private fun handleSignalChange(hasSignal: Boolean) {
        val prefs = getSharedPreferences("StoredPreferences", Context.MODE_PRIVATE)
        val receptionAlertEnabled = prefs.getBoolean("receptionAlertEnabled", false)

        if (!receptionAlertEnabled || hasSignal == lastSignalState) {
            return
        }

        lastSignalState = hasSignal
        showSignalNotification(if (hasSignal) "Cellular signal restored" else "Cellular signal lost")
    }

    private fun showSignalNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, NotificationUtils.SIGNAL_CHANNEL_ID)
            .setContentTitle("Signal Alert")
            .setContentText(message)
            .setSmallIcon(R.drawable.navigation_icon)
            .setAutoCancel(true)
            .setContentIntent(createMainActivityPendingIntent())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NotificationUtils.SIGNAL_NOTIFICATION_ID, notification)
    }

    private fun createMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }
}