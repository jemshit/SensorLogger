package com.jemshit.sensorlogger.helper

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.util.TypedValue

val Int.toDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.toPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
val Int.toSp: Int
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this.toFloat(), Resources.getSystem().displayMetrics).toInt()

fun Int.toDelimeterString(delimeter: String = ","): String {
    val iterator = toString().reversed().iterator()
    val finalString = StringBuilder()

    var counter = 1
    while (iterator.hasNext()) {
        val next = iterator.next()
        if (counter == 3) {
            counter = 1
            finalString.append(next)
            if (iterator.hasNext())
                finalString.append(delimeter)
        } else {
            finalString.append(next)
            counter += 1
        }
    }

    return finalString.reverse().toString()
}

fun Long.toDelimeterString(delimeter: String = ","): String {
    val iterator = toString().reversed().iterator()
    val finalString = StringBuilder()

    var counter = 1
    while (iterator.hasNext()) {
        val next = iterator.next()
        if (counter == 3) {
            counter = 1
            finalString.append(next)
            if (iterator.hasNext())
                finalString.append(delimeter)
        } else {
            finalString.append(next)
            counter += 1
        }
    }

    return finalString.reverse().toString()
}

fun Context.startAppropriateForegroundService(intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        startForegroundService(intent)
    else
        startService(intent)
}
