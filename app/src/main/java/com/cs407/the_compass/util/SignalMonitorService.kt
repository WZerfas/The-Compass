package com.cs407.the_compass.util

import android.app.Service
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cs407.the_compass.MainActivity
import com.cs407.the_compass.R

class SignalMonitorService : Service() {
    private var lastSignalState: Boolean = true // true means has signal
    private lateinit var telephonyManager: TelephonyManager
    private var telephonyCallback: Any? = null
    private var isMonitoring = false

    companion object {
        private const val TAG = "SignalMonitorService"

        fun startService(context: Context) {
            val intent = Intent(context, SignalMonitorService::class.java)
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SignalMonitorService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SignalMonitorService created")
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SignalMonitorService started")
        if (!isMonitoring) {
            setupSignalMonitoring()
            isMonitoring = true
        }
        return START_STICKY
    }

    private fun setupSignalMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.ServiceStateListener {
                override fun onServiceStateChanged(serviceState: ServiceState) {
                    handleSignalChange(serviceState.state == ServiceState.STATE_IN_SERVICE)
                }
            }
            telephonyCallback = callback
            try {
                telephonyManager.registerTelephonyCallback(mainExecutor, callback)
                Log.d(TAG, "Registered telephony callback for Android 12+")
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to register telephony callback: ${e.message}")
            }
        } else {
            val listener = object : PhoneStateListener() {
                override fun onServiceStateChanged(serviceState: ServiceState) {
                    handleSignalChange(serviceState.state == ServiceState.STATE_IN_SERVICE)
                }
            }
            telephonyCallback = listener
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_SERVICE_STATE)
            Log.d(TAG, "Registered phone state listener for pre-Android 12")
        }
    }

    private fun handleSignalChange(hasSignal: Boolean) {
        val prefs = getSharedPreferences("StoredPreferences", Context.MODE_PRIVATE)
        val receptionAlertEnabled = prefs.getBoolean("receptionAlertEnabled", false)

        Log.d(TAG, "Signal change detected. Has signal: $hasSignal, Alerts enabled: $receptionAlertEnabled")

        if (!receptionAlertEnabled) {
            Log.d(TAG, "Reception alerts disabled, skipping notification")
            return
        }

        if (hasSignal == lastSignalState) {
            Log.d(TAG, "Signal state unchanged, skipping notification")
            return
        }

        lastSignalState = hasSignal
        showNotification(if (hasSignal) "Cellular signal restored" else "Cellular signal lost")
    }

    private fun showNotification(message: String) {
        Log.d(TAG, "Showing notification: $message")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationUtils.SIGNAL_CHANNEL_ID)
            .setContentTitle("Signal Alert")
            .setContentText(message)
            .setSmallIcon(R.drawable.navigation_icon)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NotificationUtils.SIGNAL_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SignalMonitorService destroyed")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let {
                if (it is TelephonyCallback) {
                    telephonyManager.unregisterTelephonyCallback(it)
                }
            }
        } else {
            telephonyCallback?.let {
                if (it is PhoneStateListener) {
                    telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
                }
            }
        }
        isMonitoring = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}