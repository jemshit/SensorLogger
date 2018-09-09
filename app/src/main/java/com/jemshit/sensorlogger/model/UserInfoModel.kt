package com.jemshit.sensorlogger.model

class UserInfoModel(val age: String, val weight: String, val height: String, val gender: String) {
    override fun toString(): String {
        return "gender: $gender; age: $age; weight: $weight; height: $height;"
    }
}