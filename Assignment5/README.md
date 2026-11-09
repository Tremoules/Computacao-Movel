# Assignment 5 - Drag, Shake & Camera

Two independent Android apps based on the earlier assignments.
Open each app directory separately in Android Studio. There is no shared Gradle
project, source directory or dependency on the earlier assignment directories.

| App | Based on | Namespace / application ID | Launcher label |
|---|---|---|---|
| [SensorDice](./SensorDice) | Assignment 2 - Dice Screens | `com.example.sensordice` | Sensor Dice |
| [SensorExplorer](./SensorExplorer) | Assignment 4 - Remote Storage | `com.example.sensorexplorer` | Sensor Explorer |

Both can be installed alongside their original apps. Project names, packages,
imports, source directories, themes, launcher labels and instrumentation package
assertions have been updated. Original assignment directories are unchanged.
Generated files, IDE state, machine-specific SDK paths and Firebase configuration
are excluded from the copies.

## Firebase

SensorExplorer requires a **new Firebase Android app registration** for
`com.example.sensorexplorer` and its downloaded configuration at
`SensorExplorer/app/google-services.json` (ignored by Git). A renamed package
cannot reuse the original registration. The configuration is not included here.

Use a separate Firebase project for complete backend isolation. Alternatively,
register the new app in the existing Firebase project: SensorExplorer reads and
writes only `/assignment5/sensorExplorer`, leaving Assignment 4's `/remoteStorage`
branch untouched. Separate branches isolate app data, but share project services,
quotas and security rules. See [SensorExplorer setup](./SensorExplorer/README.md).

Without Firebase configuration, the explorer still runs with its existing
setup-required message. Firebase operations become available after configuration
and a rebuild. No local substitute for Firebase is introduced.

## Scope

[Sensor Dice](./SensorDice/IMPLEMENTATION.md) supports shake, drag, and flick on
the roll screen. [Sensor Explorer](./SensorExplorer/IMPLEMENTATION.md) adds live
camera preview and stores a replaceable photo with coordinates for each location.

Run `./gradlew testDebugUnitTest assembleDebug lintDebug` in each app directory
(`gradlew.bat` on Windows) with the same SDK/JDK prerequisites as the originals.

## Setup verification

Both apps passed `testDebugUnitTest assembleDebug lintDebug` during setup:
SensorDice ran 1 unit test and SensorExplorer ran 27, with no failures. Lint
completed with no errors (warnings and hints remain). Generated APK metadata
confirms the two application IDs listed above. No emulator/device or live
Firebase verification was performed; Firebase configuration is still required.
