package com.example.sensordice

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect

private const val SHAKE_THRESHOLD_G = 2.5f
private const val SHAKE_RESET_G = 1.4f
private const val SHAKE_COOLDOWN_NS = 750_000_000L

@Composable
internal fun RollOnShake(onShake: () -> Unit) {
    if (LocalInspectionMode.current) return

    val context = LocalContext.current
    val currentOnShake by rememberUpdatedState(onShake)
    val manager = remember(context) { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val accelerometer = remember(manager) { manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    if (manager == null || accelerometer == null) return

    LifecycleResumeEffect(manager, accelerometer) {
        var armed = true
        var lastShakeAt: Long? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                val gravity = SensorManager.GRAVITY_EARTH
                val magnitudeSquared = (x * x + y * y + z * z) / (gravity * gravity)
                if (magnitudeSquared <= SHAKE_RESET_G * SHAKE_RESET_G) {
                    armed = true
                }
                if (armed && magnitudeSquared >= SHAKE_THRESHOLD_G * SHAKE_THRESHOLD_G) {
                    armed = false
                    val previous = lastShakeAt
                    if (previous == null || event.timestamp - previous >= SHAKE_COOLDOWN_NS) {
                        lastShakeAt = event.timestamp
                        currentOnShake()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onPauseOrDispose { manager.unregisterListener(listener) }
    }
}

internal fun Modifier.flickToRoll(onFlick: (Offset) -> Unit): Modifier = pointerInput(onFlick) {
    val minimumDistance = 48.dp.toPx()
    val minimumSpeed = 650.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown()
        do {
            val change = awaitPointerEvent().changes.singleOrNull() ?: break
            if (change.id != down.id || change.isConsumed) break
            if (change.changedToUp()) {
                val displacement = change.position - down.position
                val distance = displacement.getDistance()
                val elapsedMillis = (change.uptimeMillis - down.uptimeMillis).coerceAtLeast(1L)
                if (distance >= minimumDistance && elapsedMillis <= 250 &&
                    distance * 1000 / elapsedMillis >= minimumSpeed
                ) {
                    change.consume()
                    onFlick(displacement / distance)
                }
            }
        } while (change.pressed)
    }
}
