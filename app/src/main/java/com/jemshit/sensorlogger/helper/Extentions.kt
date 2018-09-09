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

// Todo: month+hour deyince 13 ay=360 gun ediyor. (daysInMonth 30 oldugu icin)
fun Long.toTimeString(enableDay: Boolean = false,
                      enableMonth: Boolean = false,
                      daysInMonths: Int = 30,
                      enableYear: Boolean = false,
                      delimiter: String = " ",

                      yearSuffix: String = "y",
                      monthSuffix: String = "mon",
                      daySuffix: String = "d",
                      hourSuffix: String = "h",
                      minuteSuffix: String = "m",
                      secondSuffix: String = "s",
                      millisecondSuffix: String = "ms"): String {

    val finalString = StringBuilder()

    val years = this / 1000 / 60 / 60 / 24L / 365

    val months = if (enableYear)
        this / 1000 / 60 / 60 / 24L / daysInMonths % (12 * 1)
    else
        this / 1000 / 60 / 60 / 24L / daysInMonths

    val days = if (enableMonth && enableYear)
        this / 1000 / 60 / 60 / 24L % (365 * 1) % (daysInMonths * 1)
    else if (enableYear)
        this / 1000 / 60 / 60 / 24L % 365
    else if (enableMonth)
        this / 1000 / 60 / 60 / 24L % (daysInMonths * 1)
    else
        this / 1000 / 60 / 60 / 24L

    val hours = if (enableDay && enableMonth && enableYear)
        this / 1000 / 60 / 60L % (365 * 24) % (daysInMonths * 24) % (1 * 24)
    else if (enableMonth && enableYear)
        this / 1000 / 60 / 60L % (365 * 24) % (daysInMonths * 24)
    else if (enableMonth && enableDay)
        this / 1000 / 60 / 60L % (daysInMonths * 24) % (1 * 24)
    else if (enableYear && enableDay)
        this / 1000 / 60 / 60L % (365 * 24) % (1 * 24)
    else if (enableYear)
        this / 1000 / 60 / 60L % (365 * 24)
    else if (enableMonth)
        this / 1000 / 60 / 60L % (daysInMonths * 24)
    else if (enableDay)
        this / 1000 / 60 / 60L % (1 * 24)
    else
        this / 1000 / 60 / 60L

    val minutes = this / 1000 / 60 % 60L
    val seconds = this / 1000 % 60L
    val milliseconds = this % 1000L

    if (years > 0 && enableYear) {
        finalString.append(years.toString())
        finalString.append(yearSuffix)
        finalString.append(delimiter)
    }
    if (months > 0 && enableMonth) {
        finalString.append(months.toString())
        finalString.append(monthSuffix)
        finalString.append(delimiter)
    }
    if (days > 0 && enableDay) {
        finalString.append(days.toString())
        finalString.append(daySuffix)
        finalString.append(delimiter)
    }
    if (hours > 0) {
        finalString.append(hours.toString())
        finalString.append(hourSuffix)
        finalString.append(delimiter)
    }
    if (minutes > 0) {
        finalString.append(minutes.toString())
        finalString.append(minuteSuffix)
        finalString.append(delimiter)
    }
    if (seconds > 0) {
        finalString.append(seconds.toString())
        finalString.append(secondSuffix)
        finalString.append(delimiter)
    }
    if (milliseconds > 0) {
        finalString.append(milliseconds.toString())
        finalString.append(millisecondSuffix)
        finalString.append(delimiter)
    }

    finalString.removeSuffix(delimiter)

    return finalString.toString()
}

fun Context.startAppropriateForegroundService(intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        startForegroundService(intent)
    else
        startService(intent)
}
