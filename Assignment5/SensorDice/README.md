# Assignment 5 - Sensor Dice

Independent copy of Assignment 2 (Dice Screens). Open this directory directly in
Android Studio; it has its own Gradle wrapper and app module.

- Project: `SensorDice`
- Launcher label: `Sensor Dice`
- Namespace and application ID: `com.example.sensordice`
- Main, unit-test and instrumentation-test packages use the new namespace.

The roll screen supports three ways to roll the die:

- Tap **Roll**.
- Shake a device that has an accelerometer. Shake input is active while the roll
  screen is in the foreground.
- Flick quickly across the play area. The die moves in the direction of the
  flick, then returns to the center with the rolled face.

Drag **Roll** around the play area to reposition it. Use **Result Screen** to
open the corresponding result from the original Dice Screens app.

See the [implementation and testing guide](IMPLEMENTATION.md) for the file-by-file
changes, gesture logic, dependency, and emulator commands.

Build and verify with `./gradlew testDebugUnitTest assembleDebug lintDebug`
(or `gradlew.bat` on Windows). Requires the same Android SDK and JDK as the
original project. No Firebase configuration is needed.
