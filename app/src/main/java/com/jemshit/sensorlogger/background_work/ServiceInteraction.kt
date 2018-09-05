package com.jemshit.sensorlogger.background_work

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle


const val ARG_BUNDLE = "arg_bundle"
const val ARG_COMMAND = "arg_command"

fun <T> createServiceIntent(context: Context,
                            serviceClass: Class<T>,
                            extraBundle: Bundle? = null,
                            serviceCommand: ServiceCommand? = null): Intent {

    val intent = Intent(context, serviceClass)
    extraBundle?.let { intent.putExtra(ARG_BUNDLE, it) }
    serviceCommand?.let { intent.putExtra(ARG_COMMAND, it.id) }
    return intent
}

enum class ServiceCommand(open val id: Int) {
    START(1),
    STOP(2),
    STOP_AND_IGNORE(3)
}

fun <T> isServiceRunningInForeground(context: Context, serviceClass: Class<T>): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        // Check notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val activeNotifications = notificationManager.activeNotifications
        return activeNotifications.firstOrNull { it.id == NOTIFICATION_ID_FOREGROUND_SERVICE } != null
    } else {
        // Check service list
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return service.foreground
            }
        }
        return false
    }
}