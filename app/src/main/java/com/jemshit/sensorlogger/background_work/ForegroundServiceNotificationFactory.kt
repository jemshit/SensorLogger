package com.jemshit.sensorlogger.background_work

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.navigation.NavDeepLinkBuilder
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.helper.random

const val NOTIFICATION_ID_FOREGROUND_SERVICE = 1040

fun createAndStartNotification(service: Service) {
    val contentPendingIntent = NavDeepLinkBuilder(service)
            .setGraph(R.navigation.main_navigation)
            .setDestination(R.id.recordingInfoFragment)
            .createPendingIntent()

    val serviceIntent = createServiceIntent(service,
            SensorLoggerService::class.java,
            null,
            ServiceCommand.STOP
    )
    serviceIntent.putExtras(Intent())   // Make intent of onStartCommand() non null so only process death has null intent

    val stopServicePendingIntent = PendingIntent.getService(service, 0, serviceIntent, 0)

    val stopAction =
            NotificationCompat.Action
                    .Builder(
                            R.drawable.ic_stop_24dp_black,
                            service.getString(R.string.stop_recording),
                            stopServicePendingIntent)
                    .build()

    val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationO(service, contentPendingIntent, stopAction)
    } else {
        createNotificationPreO(service, contentPendingIntent, stopAction)
    }

    service.startForeground(NOTIFICATION_ID_FOREGROUND_SERVICE, notification)
}

@TargetApi(Build.VERSION_CODES.N)
fun createNotificationPreO(service: Service, contentIntent: PendingIntent, stopAction: NotificationCompat.Action): Notification {
    return NotificationCompat.Builder(service)
            .setContentTitle(service.getString(R.string.notification_title))
            .setContentText(service.getString(R.string.notification_content))
            .setSmallIcon(R.drawable.ic_run_notification)
            .setContentIntent(contentIntent)
            .addAction(stopAction)
            .setVisibility(VISIBILITY_PUBLIC)   // For lock screen
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // In O, it is channel importance
            .setStyle(NotificationCompat.BigTextStyle())
            .setAutoCancel(false) // don't remove notification when user taps
            .build()
}

@TargetApi(Build.VERSION_CODES.O)
private fun createNotificationO(service: Service, contentIntent: PendingIntent, stopAction: NotificationCompat.Action): Notification {
    val channelId = (0..100000).random().toString()
    createChannelO(service, channelId)

    return NotificationCompat.Builder(service, channelId)
            .setContentTitle(service.getString(R.string.notification_title))
            .setContentText(service.getString(R.string.notification_content))
            .setSmallIcon(R.drawable.ic_run_notification)
            .setContentIntent(contentIntent)
            .addAction(stopAction)
            .setVisibility(VISIBILITY_PUBLIC)
            .setStyle(NotificationCompat.BigTextStyle())
            .setAutoCancel(false)   // don't remove notification when user taps
            .build()
}

@RequiresApi(Build.VERSION_CODES.O)
private fun createChannelO(service: Service, channelId: String) {
    val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelName = "Sensor Channel"
    val importance = NotificationManager.IMPORTANCE_HIGH
    val notificationChannel = NotificationChannel(channelId, channelName, importance)
    notificationManager.createNotificationChannel(notificationChannel)
}