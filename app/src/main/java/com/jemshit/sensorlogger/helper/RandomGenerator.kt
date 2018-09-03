package com.jemshit.sensorlogger.helper

import android.os.Build
import java.util.*
import java.util.concurrent.ThreadLocalRandom

fun ClosedRange<Int>.random() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        ThreadLocalRandom.current().nextInt((endInclusive + 1) - start) + start
    } else
        Random().nextInt((endInclusive + 1) - start) + start
}