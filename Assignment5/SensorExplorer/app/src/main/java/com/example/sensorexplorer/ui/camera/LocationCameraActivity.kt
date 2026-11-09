package com.example.sensorexplorer.ui.camera

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CAMERA
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.sensorexplorer.R
import com.example.sensorexplorer.data.LocationPhotoStore
import com.example.sensorexplorer.ui.theme.SensorExplorerTheme
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

internal class LocationCameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(LOCATION_ID, 0)
        if (id <= 0) {
            finish()
            return
        }
        val name = intent.getStringExtra(LOCATION_NAME).orEmpty()
        enableEdgeToEdge()
        setContent {
            SensorExplorerTheme {
                Surface(Modifier.fillMaxSize()) { CameraScreen(id, name) }
            }
        }
    }

    @Composable
    private fun CameraScreen(locationId: Int, locationName: String) {
        var cameraAllowed by remember { mutableStateOf(hasPermission(CAMERA)) }
        var locationAllowed by remember { mutableStateOf(hasLocationPermission()) }
        var capture by remember { mutableStateOf<ImageCapture?>(null) }
        var busy by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<Int?>(null) }
        val scope = rememberCoroutineScope()
        val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            cameraAllowed = hasPermission(CAMERA)
            locationAllowed = hasLocationPermission()
        }
        BackHandler(enabled = busy) { }

        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.camera_title, locationName), style = MaterialTheme.typography.titleLarge)
            if (!cameraAllowed || !locationAllowed) {
                Text(stringResource(R.string.camera_permissions))
                Button(onClick = {
                    permissions.launch(buildList {
                        if (!cameraAllowed) add(CAMERA)
                        if (!locationAllowed) addAll(listOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
                    }.toTypedArray())
                }) { Text(stringResource(R.string.camera_allow_permissions)) }
            }
            if (cameraAllowed) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CameraPreview(
                        onReady = { capture = it },
                        onError = { message = R.string.camera_unavailable }
                    )
                    if (capture == null && message == null) CircularProgressIndicator()
                }
            }
            message?.let { Text(stringResource(it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { finish() }, enabled = !busy) { Text(stringResource(R.string.camera_back)) }
                Button(
                    enabled = capture != null && cameraAllowed && locationAllowed && !busy,
                    onClick = {
                        val activeCapture = capture ?: return@Button
                        busy = true
                        message = R.string.camera_saving
                        scope.launch {
                            var temporary: File? = null
                            try {
                                val file = File.createTempFile("location-photo-", ".jpg", cacheDir)
                                temporary = file
                                activeCapture.captureTo(file, ContextCompat.getMainExecutor(this@LocationCameraActivity))
                                val location = currentCaptureLocation(this@LocationCameraActivity)
                                if (location == null) {
                                    message = R.string.camera_no_location
                                } else {
                                    LocationPhotoStore(this@LocationCameraActivity).save(
                                        locationId, file, location.latitude, location.longitude
                                    )
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                }
                            } catch (_: ImageCaptureException) {
                                message = R.string.camera_capture_failed
                            } catch (_: IOException) {
                                message = R.string.camera_save_failed
                            } catch (_: SecurityException) {
                                cameraAllowed = hasPermission(CAMERA)
                                locationAllowed = hasLocationPermission()
                                message = R.string.camera_permissions
                            } finally {
                                temporary?.delete()
                                busy = false
                            }
                        }
                    }
                ) { Text(stringResource(R.string.camera_capture)) }
            }
        }
    }

    @Composable
    private fun CameraPreview(onReady: (ImageCapture?) -> Unit, onError: () -> Unit) {
        val previewView = remember { PreviewView(this).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        DisposableEffect(previewView) {
            val future = ProcessCameraProvider.getInstance(this@LocationCameraActivity)
            val preview = Preview.Builder().build().apply { surfaceProvider = previewView.surfaceProvider }
            val imageCapture = ImageCapture.Builder().build()
            var provider: ProcessCameraProvider? = null
            var disposed = false
            future.addListener({
                if (!disposed) {
                    try {
                        val cameraProvider = future.get()
                        provider = cameraProvider
                        val selector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        }
                        cameraProvider.bindToLifecycle(this@LocationCameraActivity, selector, preview, imageCapture)
                        previewView.previewStreamState.observe(this@LocationCameraActivity) { state ->
                            onReady(imageCapture.takeIf { state == PreviewView.StreamState.STREAMING })
                        }
                    } catch (_: ExecutionException) {
                        onError()
                    } catch (_: RuntimeException) {
                        onError()
                    }
                }
            }, ContextCompat.getMainExecutor(this@LocationCameraActivity))
            onDispose {
                disposed = true
                previewView.previewStreamState.removeObservers(this@LocationCameraActivity)
                provider?.unbind(preview, imageCapture)
                onReady(null)
            }
        }
    }

    private fun hasLocationPermission() = hasPermission(ACCESS_FINE_LOCATION) || hasPermission(ACCESS_COARSE_LOCATION)

    companion object {
        private const val LOCATION_ID = "location_id"
        private const val LOCATION_NAME = "location_name"

        fun intent(context: Context, id: Int, name: String): Intent =
            Intent(context, LocationCameraActivity::class.java)
                .putExtra(LOCATION_ID, id)
                .putExtra(LOCATION_NAME, name)
    }
}

private suspend fun ImageCapture.captureTo(file: File, executor: Executor) =
    suspendCancellableCoroutine<Unit> { continuation ->
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        takePicture(options, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                if (continuation.isActive) continuation.resume(Unit) else file.delete()
            }

            override fun onError(exception: ImageCaptureException) {
                if (continuation.isActive) continuation.resumeWithException(exception) else file.delete()
            }
        })
    }
