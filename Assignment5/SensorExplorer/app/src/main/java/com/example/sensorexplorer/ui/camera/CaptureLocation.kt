package com.example.sensorexplorer.ui.camera

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

internal fun Context.hasPermission(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission") // Both possible location grants are checked below.
internal suspend fun currentCaptureLocation(context: Context): Location? {
    val precise = context.hasPermission(ACCESS_FINE_LOCATION)
    if (!precise && !context.hasPermission(ACCESS_COARSE_LOCATION)) return null
    val manager = context.getSystemService(LocationManager::class.java) ?: return null
    val providers = manager.getProviders(true).filter { provider ->
        provider == LocationManager.NETWORK_PROVIDER ||
            (precise && provider == LocationManager.GPS_PROVIDER)
    }
    val executor = ContextCompat.getMainExecutor(context)
    val request = LocationRequestCompat.Builder(500L).build()
    val requestedAt = SystemClock.elapsedRealtimeNanos()
    return withTimeoutOrNull(12_000L) {
        callbackFlow<Location> {
            val listener = LocationListenerCompat { location ->
                if (location.elapsedRealtimeNanos >= requestedAt) trySend(location)
            }
            var registered = false
            for (provider in providers) {
                try {
                    LocationManagerCompat.requestLocationUpdates(manager, provider, request, executor, listener)
                    registered = true
                } catch (_: SecurityException) {
                    // Permission may have changed since the request started.
                } catch (_: IllegalArgumentException) {
                    // Provider may have disappeared since discovery.
                }
            }
            if (!registered) close()
            awaitClose { if (registered) LocationManagerCompat.removeUpdates(manager, listener) }
        }.firstOrNull()
    }
}
