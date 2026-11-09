package com.example.sensorexplorer.data

import android.content.Context
import android.util.AtomicFile
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class LocationPhoto(
    val file: File,
    val latitude: Double,
    val longitude: Double,
    val modifiedAt: Long
)

internal class LocationPhotoStore(context: Context) {
    private val directory = File(context.noBackupFilesDir, "location-photos")

    suspend fun read(locationId: Int): LocationPhoto? = withContext(Dispatchers.IO) {
        files.withLock {
            val file = fileFor(locationId)
            try {
                AtomicFile(file).openRead().use { input ->
                    val coordinates = ExifInterface(input).latLong ?: return@use null
                    LocationPhoto(file, coordinates[0], coordinates[1], file.lastModified())
                }
            } catch (_: IOException) {
                null
            }
        }
    }

    suspend fun save(locationId: Int, jpeg: File, latitude: Double, longitude: Double) =
        withContext(Dispatchers.IO) {
            ExifInterface(jpeg).apply {
                setLatLong(latitude, longitude)
                saveAttributes()
            }
            files.withLock {
                val target = AtomicFile(fileFor(locationId))
                val output = target.startWrite()
                try {
                    jpeg.inputStream().use { it.copyTo(output) }
                    target.finishWrite(output)
                } catch (exception: Exception) {
                    target.failWrite(output)
                    throw exception
                }
            }
        }

    private fun fileFor(locationId: Int) = File(directory, "$locationId.jpg")

    private companion object {
        // AtomicFile recovery and replacement must not run concurrently.
        val files = Mutex()
    }
}
