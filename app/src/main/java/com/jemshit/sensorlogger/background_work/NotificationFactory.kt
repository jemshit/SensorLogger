package com.jemshit.sensorlogger.background_work

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.helper.random


const val NOTIFICATION_ID_FOREGROUND_SERVICE = 1040
const val STOP_ACTION_REQUEST_CODE = 12093
const val STOP_IGNORE_ACTION_REQUEST_CODE = 46545
const val EXPORT_NOTIFICATION_ID = 17236

fun createAndStartNotification(service: Service) {
    val contentPendingIntent = NavDeepLinkBuilder(service)
            .setGraph(R.navigation.main_navigation)
            .setDestination(R.id.recordingInfoFragment)
            .createPendingIntent()

    // Stop Action
    val stopActionIntent = createServiceIntent(service,
            SensorLoggerService::class.java,
            null,
            ServiceCommand.STOP
    )
    stopActionIntent.action = "StopAction"
    val stopActionPendingIntent = PendingIntent.getService(service, STOP_ACTION_REQUEST_CODE, stopActionIntent, 0)
    val stopAction =
            NotificationCompat.Action
                    .Builder(
                            R.drawable.ic_stop_24dp_black,
                            service.getString(R.string.stop_recording),
                            stopActionPendingIntent)
                    .build()

    // Stop and Ignore Data Action
    val stopIgnoreActionIntent = createServiceIntent(service,
            SensorLoggerService::class.java,
            null,
            ServiceCommand.STOP_AND_IGNORE
    )
    stopIgnoreActionIntent.action = "StopIgnoreAction"
    val stopIgnoreActionPendingIntent = PendingIntent.getService(service, STOP_IGNORE_ACTION_REQUEST_CODE, stopIgnoreActionIntent, 0)
    val stopIgnoreAction =
            NotificationCompat.Action
                    .Builder(
                            R.drawable.ic_stop_24dp_black,
                            service.getString(R.string.stop_and_ignore),
                            stopIgnoreActionPendingIntent)
                    .build()


    val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationO(service,
                contentPendingIntent,
                "Sensor Channel",
                service.getString(R.string.record_notification_title),
                service.getString(R.string.record_notification_content),
                R.drawable.ic_run_notification,
                false,
                listOf(stopAction, stopIgnoreAction))
    } else {
        createNotificationPreO(
                service,
                contentPendingIntent,
                service.getString(R.string.record_notification_title),
                service.getString(R.string.record_notification_content),
                R.drawable.ic_run_notification,
                false,
                listOf(stopAction, stopIgnoreAction))
    }

    service.startForeground(NOTIFICATION_ID_FOREGROUND_SERVICE, notification)
}

fun createExportNotification(context: Context,
                             title: String = context.getString(R.string.export_notification_title),
                             content: String = context.getString(R.string.export_notification_content)) {
    val contentPendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.main_navigation)
            .setDestination(R.id.exportFragment)
            .createPendingIntent()

    val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createNotificationO(context,
                contentPendingIntent,
                "Sensor Channel",
                title,
                content,
                R.drawable.ic_export_notification,
                true)
    } else {
        createNotificationPreO(
                context,
                contentPendingIntent,
                title,
                content,
                R.drawable.ic_export_notification,
                true)
    }

    val notificationManager = NotificationManagerCompat.from(context)
    notificationManager.notify(EXPORT_NOTIFICATION_ID, notification)
}


@TargetApi(Build.VERSION_CODES.N)
fun createNotificationPreO(context: Context,
                           contentIntent: PendingIntent,
                           title: String,
                           content: String,
                           icon: Int,
                           autoCancelOnTap: Boolean,
                           actions: List<NotificationCompat.Action> = listOf()): Notification {

    val builder = NotificationCompat.Builder(context)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(icon)
            .setContentIntent(contentIntent)
            .setVisibility(VISIBILITY_PUBLIC)   // For lock screen
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // In O, it is channel importance
            .setStyle(NotificationCompat.BigTextStyle())
            .setAutoCancel(autoCancelOnTap)

    actions.forEach { builder.addAction(it) }

    return builder.build()
}

@TargetApi(Build.VERSION_CODES.O)
private fun createNotificationO(context: Context,
                                contentIntent: PendingIntent,
                                channelName: String,
                                title: String,
                                content: String,
                                icon: Int,
                                autoCancelOnTap: Boolean,
                                actions: List<NotificationCompat.Action> = listOf()): Notification {

    val channelId = (0..10000).random().toString()
    createChannelO(context, channelId, channelName)

    val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(icon)
            .setContentIntent(contentIntent)
            .setVisibility(VISIBILITY_PUBLIC)
            .setStyle(NotificationCompat.BigTextStyle())
            .setAutoCancel(autoCancelOnTap)

    actions.forEach { builder.addAction(it) }

    return builder.build()
}

@RequiresApi(Build.VERSION_CODES.O)
private fun createChannelO(context: Context, channelId: String, channelName: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val importance = NotificationManager.IMPORTANCE_HIGH
    val notificationChannel = NotificationChannel(channelId, channelName, importance)
    notificationManager.createNotificationChannel(notificationChannel)
}