package com.jemshit.sensorlogger.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File


const val EXPORT_FOLDER_NAME = "SensorLogger"

fun createActivityFolder(context: Context, activityName: String, devicePosition: String, deviceOrientation: String): Pair<File?, String> {
    val writePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    if (writePermission != PackageManager.PERMISSION_GRANTED)
        return Pair(null, "Write permission is not granted!")

    val state = Environment.getExternalStorageState()
    return when (state) {
        Environment.MEDIA_MOUNTED -> {
            try {
                val folder = File(Environment.getExternalStorageDirectory(), "$EXPORT_FOLDER_NAME/$activityName/$devicePosition/$deviceOrientation")
                if (!folder.exists())
                    folder.mkdirs()
                Pair(folder, "")
            } catch (e: Exception) {
                Pair(null, e.message ?: "Unknown Error while creating activity folder")
            }
        }

        Environment.MEDIA_MOUNTED_READ_ONLY -> Pair(null, "Storage directory is Read only!")

        else -> Pair(null, "Storage directory is not Mounted!")
    }
}

fun createLogFile(context: Context, folder: File, fileName: String): Pair<File?, String> {
    val writePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    if (writePermission != PackageManager.PERMISSION_GRANTED)
        return Pair(null, "Write permission is not granted!")

    val state = Environment.getExternalStorageState()
    return when (state) {
        Environment.MEDIA_MOUNTED -> {
            try {
                val fileCreated = File(folder, fileName)
                if (!folder.exists())
                    folder.mkdirs()
                if (!fileCreated.exists())
                    fileCreated.createNewFile()
                Pair(fileCreated, "")
            } catch (e: Exception) {
                Pair(null, e.message ?: "Unknown Error while creating log file")
            }
        }

        Environment.MEDIA_MOUNTED_READ_ONLY -> Pair(null, "Storage directory is Read only!")

        else -> Pair(null, "Storage directory is not Mounted!")
    }
}

fun deleteExportFolder(context: Context): Boolean {
    val writePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    if (writePermission != PackageManager.PERMISSION_GRANTED)
        return false

    val state = Environment.getExternalStorageState()
    return when (state) {
        Environment.MEDIA_MOUNTED -> {
            val folder = File(Environment.getExternalStorageDirectory(), EXPORT_FOLDER_NAME)
            deleteRecursively(folder)
        }

        Environment.MEDIA_MOUNTED_READ_ONLY -> false
        else -> false
    }
}

private fun deleteRecursively(fileOrDirectory: File): Boolean {
    return try {
        if (fileOrDirectory.isDirectory)
            for (child in fileOrDirectory.listFiles())
                deleteRecursively(child)

        fileOrDirectory.delete()
    } catch (e: Exception) {
        false
    }
}
