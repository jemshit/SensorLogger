package com.jemshit.sensorlogger.model

class DeviceInfoModel {
    val sdkInt: Int
        get() = android.os.Build.VERSION.SDK_INT

    val device: String
        get() = android.os.Build.DEVICE

    val deviceModel: String
        get() = android.os.Build.MODEL

    val product: String
        get() = android.os.Build.PRODUCT

    val manufacturer: String
        get() = android.os.Build.MANUFACTURER

    val brand: String
        get() = android.os.Build.BRAND

    override fun toString(): String {
        return "sdkInt: $sdkInt; device: $device; deviceModel: $deviceModel; product: $product; manufacturer: $manufacturer; brand:$brand;"
    }
}