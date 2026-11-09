# Assignment 5, Part 2 — Sensor Explorer

Sensor Explorer adds a camera screen for Rick and Morty locations. You can take
or replace a location's photo, see it with the device's latitude and longitude,
and reopen that location later to see the saved capture. The coordinates describe
where the device took the photo. The Rick and Morty location is the item the app
associates with that capture.

## Run and use the app

1. Open `Assignment5/SensorExplorer` as its own project in Android Studio, sync
   Gradle, and run the `app` configuration on a phone or emulator with a camera.
2. On the explorer screen, select **Location**. Use the name filter to find an
   item and scroll to its result card. Browsing the Rick and Morty API requires
   internet access.
3. Tap **Add photo**. On the camera screen, tap **Allow access** if prompted.
   Allow camera and location access while using the app. Choose **Precise** for
   a GPS fix, especially on an emulator with simulated GPS.
4. Frame the live preview and tap **Capture and save**. The app takes the photo,
   obtains a current location reading, and returns to the result card. The card
   displays the photo and coordinates.
5. Tap **Replace photo** to save a newer capture for the same location. Use
   **Back** before capturing to return to the explorer without changing the photo.
   After a restart, find the same location again to see its saved photo.

If capture cannot get a location reading within 12 seconds, turn on device
location and try again. On an emulator, send a test position. The prior photo
remains available if a replacement fails. If Android stops offering a permission
prompt, grant it under **Settings → Apps → Sensor Explorer → Permissions**.

## Files changed

| File | Change |
|---|---|
| [SensorExplorerApp.kt](app/src/main/java/com/example/sensorexplorer/ui/SensorExplorerApp.kt) | Selects the current location, launches the camera activity with its ID and name, and reads the saved photo for the card. A successful capture result triggers a reload. |
| [HomeScreen.kt](app/src/main/java/com/example/sensorexplorer/ui/screens/HomeScreen.kt) | Shows the saved image and coordinates in the location card. Adds the **Add photo** and **Replace photo** actions. |
| [LocationCameraActivity.kt](app/src/main/java/com/example/sensorexplorer/ui/camera/LocationCameraActivity.kt) | Adds the camera screen, permission requests, lifecycle binding, live preview, shutter, progress, and error handling. |
| [CaptureLocation.kt](app/src/main/java/com/example/sensorexplorer/ui/camera/CaptureLocation.kt) | Adds the permission check and a cancellable, one-time location request. |
| [LocationPhotoStore.kt](app/src/main/java/com/example/sensorexplorer/data/LocationPhotoStore.kt) | Saves one JPEG per location ID with embedded coordinates, and reads the photo and coordinates for display. |
| [AndroidManifest.xml](app/src/main/AndroidManifest.xml) | Registers the internal camera activity, declares camera and foreground location permissions, and marks the related hardware features optional. |
| [strings.xml](app/src/main/res/values/strings.xml) | Adds capture labels, instructions, the image description, formatted coordinates, and error messages. |
| [libs.versions.toml](gradle/libs.versions.toml), [app/build.gradle.kts](app/build.gradle.kts) | Declares and adds CameraX and ExifInterface dependencies. |
| [Sensor Explorer README](README.md), [Assignment 5 README](../README.md), [course README](../../README.md) | Updates the feature and assignment status and links to this guide. |

To see the complete patch from the Sensor Dice commit, run this at the
`Computacao-Movel` repository root:

```bash
git diff 627893d..HEAD -- Assignment5/SensorExplorer Assignment5/README.md README.md
```

## How the code works

### From the result card to the camera

`SensorExplorerApp` reads the current `ExplorerUiState.Success` item and checks
that the selected category is `Location`. That check matters because character,
episode, and location IDs can overlap. The camera action also stays disabled
while a roll or saved-item load is in progress.

The app launches `LocationCameraActivity` with the location ID and name in an
`Intent`. The name is for the screen title. The ID stays fixed for the lifetime
of that capture screen and determines where the JPEG is saved. The same path
works for locations found through the API and locations restored from Firebase.
The camera activity has `android:exported="false"` so it is launched internally.

On return with `RESULT_OK`, the app increments `photoRevision`. The photo read
is keyed by both the selected location ID and that revision. Selecting a
different location starts a new read, and replacing the same location's photo
refreshes the card. `HomeScreen` uses Coil's existing `AsyncImage` to display the
local JPEG, with a memory cache key that includes its modification time. Until a
photo exists, the card shows the explorer's original image or placeholder. The
coordinates appear below the photo with six decimal places. The number of
printed decimal places does not indicate the precision of the location fix.

### Camera preview and permissions

The **Allow access** button requests only the missing permissions. The app
requests `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` together so Android
can offer the choice of precise or approximate access. It also requests `CAMERA`
if needed. The preview can run with camera permission alone. Saving requires
camera and foreground location access.

`CameraPreview` uses Compose's `AndroidView` to host CameraX's `PreviewView`.
It obtains a `ProcessCameraProvider`, chooses the back camera if available,
and binds two use cases to the capture activity's lifecycle:

- `Preview` streams camera frames to the view.
- `ImageCapture` takes a JPEG and writes it to a temporary file.

The shutter becomes available once `PreviewView` reports that it is streaming
and location permission is granted. Leaving the screen removes the stream
observer and unbinds these use cases. While saving, the screen disables capture
and back to prevent duplicate or interrupted requests from its controls.

### How the app gets coordinates

After CameraX writes the JPEG, `currentCaptureLocation` checks the location
grant and asks the system for a one-time fix. With precise permission it can
request GPS and network providers. With approximate permission it can request
the network provider when enabled. `LocationManagerCompat` starts a short-lived
listener for the enabled providers. It accepts the first fix timestamped after
this request began, which prevents a recent earlier capture's coordinates from
being reused. The listener is removed after a fix, cancellation, or the
12-second timeout. The app requests foreground location only.

### How the photo and coordinates stay together

The capture starts in the app's cache directory. Once the app has a location,
`LocationPhotoStore.save` runs on `Dispatchers.IO` and tags the temporary JPEG:

```kotlin
ExifInterface(jpeg).apply {
    setLatLong(latitude, longitude)
    saveAttributes()
}
```

EXIF is metadata stored inside the image file. Here the image and the matching
coordinates are written together as a single JPEG at
`noBackupFilesDir/location-photos/<locationId>.jpg`. `AtomicFile` stages the copy
and replaces the previous image after the write finishes. A shared `Mutex`
keeps reads and replacements from interfering while `AtomicFile` manages the
staged file. The temporary capture is deleted when the capture flow finishes. The next visit opens that JPEG and
reads the coordinates back from its EXIF data.

Local storage was chosen from the two options in TP5. Each device has its own
captures, and clearing app data or uninstalling removes them. The photos live in
app-private storage outside Android backups and the app does not request gallery
or external storage access. Firebase **Save item**, **Load last**, roll count,
and history continue to use the existing `/assignment5/sensorExplorer` data.
Their snapshots store the API image URL. The captured JPEG and its coordinates
are managed separately by the local photo store.

## Gradle and manifest configuration

| Dependency | Version | Purpose |
|---|---|---|
| `androidx.camera:camera-camera2` | `1.5.3` | CameraX backend for Android's Camera2 API. |
| `androidx.camera:camera-lifecycle` | `1.5.3` | Binds the camera use cases to the capture activity. |
| `androidx.camera:camera-view` | `1.5.3` | Provides the `PreviewView` used in Compose. |
| `androidx.exifinterface:exifinterface` | `1.4.2` | Writes and reads coordinates in the JPEG. |

The app module uses the version-catalog aliases `libs.androidx.camera.camera2`,
`libs.androidx.camera.lifecycle`, `libs.androidx.camera.view`, and
`libs.androidx.exifinterface`. The existing AndroidX Core dependency provides
`LocationManagerCompat` and its cancellation support. The SDK levels and Gradle
wrapper are inherited from setup. The app ID is `com.example.sensorexplorer`.
Camera and location hardware are optional for installation, so the explorer can
still run on devices without those capabilities. See the
[CameraX preview guide](https://developer.android.com/media/camera/camerax/preview)
and [Android runtime location permissions](https://developer.android.com/develop/sensors-and-location/location/permissions/runtime).

## Try it on an emulator

Use a virtual device with a simulated back camera and location enabled. In the
emulator toolbar, open **⋯ → Location**, choose a point, and send it to the
emulator. Then take a photo from a location card. For a repeatable Lisbon test
from Android Studio's Terminal on macOS, list your emulators first:

```bash
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
"$ADB" devices -l
```

Replace the example serial with the one running Sensor Explorer. The `geo fix`
command takes **longitude first, then latitude**:

```bash
SERIAL=emulator-5556
"$ADB" -s "$SERIAL" emu geo fix -9.1393 38.7223
```

Tap **Capture and save**, then resend the command if the app is waiting for a
fix. The card should show latitude near `38.7223` and longitude near `-9.1393`.
The last digits can vary with the emulator’s reported fix. Send another point and tap **Replace photo** to check the update.
Close and reopen the app, then select the same Rick and Morty location to check
persistence. See the [emulator console location commands](https://developer.android.com/studio/run/emulator-console#geo).

For the build checks, run this from the Sensor Explorer directory:

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
```

The debug APK built successfully, all 27 existing unit tests passed, and lint
reported zero errors. On a Pixel 6 emulator, the preview streamed, a simulated
GPS fix appeared with the captured photo, and replacement used the second fix.
After a process restart, filtering for the same location restored the photo and
its replacement coordinates. The emulator had one saved JPEG for that location.
