# Assignment 5 - Sensor Dice (setup)

Independent copy of Assignment 2 (Dice Screens). Open this directory directly in
Android Studio; it has its own Gradle wrapper and app module.

- Project: `SensorDice`
- Launcher label: `Sensor Dice`
- Namespace and application ID: `com.example.sensordice`
- Main, unit-test and instrumentation-test packages use the new namespace.

The original dice screens and navigation are preserved. TP5 shake, draggable
Roll button and flick gesture features are not implemented yet.

Build and verify with `./gradlew testDebugUnitTest assembleDebug lintDebug`
(or `gradlew.bat` on Windows). Requires the same Android SDK and JDK as the
original project. No Firebase configuration is needed.
