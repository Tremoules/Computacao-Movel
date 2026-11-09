# Assignment 5, Part 1 — Sensor Dice

Sensor Dice extends the Assignment 2 dice roller with the three interactions
required in TP5: shake to roll, drag the **Roll** button, and flick the die in a
direction. The setup commit `2cd9006` is the starting point for this guide.

## Run and use the app

1. Open the `Assignment5/SensorDice` directory in Android Studio and let Gradle sync.
2. Choose the `app` run configuration and a phone or an Android virtual device
   with an accelerometer. Press **Run**.
3. On the starting screen, try the controls below. The displayed value and die
   image always use the same result.

| Control | How to use it | What happens |
|---|---|---|
| Tap | Tap **Roll** wherever it is positioned. | The app picks a value from 1 to 6. |
| Shake | Give the phone a short shake while the roll screen is open. | The app picks a value from 1 to 6 when the accelerometer crosses the threshold. |
| Drag | Start on **Roll**, hold, and move your finger around the play area. | The button follows your finger within the play area. The die keeps its value. |
| Flick | Start on the die or the play area beside it and swipe quickly in any direction. | The die rolls, moves in the swipe direction, and returns to the center. |
| Result Screen | Tap **Result Screen** below the play area. | The app opens the original result screen corresponding to the current value. |

Each roll is independent, so it can land on the same face twice. The toss
animation belongs to flicks. For tap and shake, watch **Current value** under the
die. A slow swipe across the play area is ignored.

## Files changed

| File | Change |
|---|---|
| [MainActivity.kt](app/src/main/java/com/example/sensordice/MainActivity.kt) | Moved the roll screen and its preview out of the activity. The activity still enables edge-to-edge rendering and opens `NavGraph` inside `SensorDiceTheme`. |
| [RollScreen.kt](app/src/main/java/com/example/sensordice/RollScreen.kt) | Added the roll screen, shared roll action, saveable die state, draggable button, directional toss animation, and screen preview. |
| [RollGestures.kt](app/src/main/java/com/example/sensordice/RollGestures.kt) | Added lifecycle-aware accelerometer input and the flick detector. |
| [strings.xml](app/src/main/res/values/strings.xml) | Added `roll_gesture_hint`, `die_face`, and `current_value`. |
| [libs.versions.toml](gradle/libs.versions.toml) | Added a version catalog alias for AndroidX Lifecycle Runtime Compose. |
| [app/build.gradle.kts](app/build.gradle.kts) | Added the Lifecycle Runtime Compose dependency to the app module. |
| [Sensor Dice README](README.md), [Assignment 5 README](../README.md) | Updated the usage and assignment progress. |

To see the complete patch, including imports, removed code, and the Gradle lines,
run this from the `Computacao-Movel` repository root:

```bash
git diff 2cd9006..HEAD -- Assignment5/SensorDice Assignment5/README.md
```

## How the code works

### One roll action for all inputs

The roll destination in [NavGraph.kt](app/src/main/java/com/example/sensordice/navigation/NavGraph.kt)
calls `DiceWithRollerAndImage`. Its implementation now lives in `RollScreen.kt`.
The screen owns the value and defines a single action used by tap, shake, and flick:

```kotlin
var result by rememberSaveable { mutableIntStateOf(1) }
val roll = { result = (1..6).random() }
RollOnShake(onShake = roll)
```

The first face is `1`. `rememberSaveable` lets Compose restore the value after a
configuration change. A `when` expression maps the value to the existing
`dice_1` through `dice_6` drawables. The result button passes the current value to
`Screens.DiceResult.createRoute(result)`, and `NavGraph` selects the matching
`DiceResult1` through `DiceResult6` destination.

### Shake detection

`RollOnShake` in `RollGestures.kt` obtains the default `TYPE_ACCELEROMETER` from
Android's `SensorManager`. It skips sensor registration in the Compose Preview
and when the device has no accelerometer. Tap and flick still work in that case.
`LifecycleResumeEffect` registers the listener at `SENSOR_DELAY_GAME` when the
roll destination is resumed. `onPauseOrDispose` unregisters it when that
destination pauses or leaves composition. `rememberUpdatedState` gives the
listener the current roll callback without recreating the subscription on each
recomposition.

The sensor reports acceleration along three axes, including gravity. The
listener compares the squared magnitude in units of gravity (`g`) so a shake can
come from any direction without computing a square root:

```kotlin
val magnitudeSquared = (x * x + y * y + z * z) / (gravity * gravity)
```

- **Trigger at 2.5 g.** Crossing this threshold while the detector is armed
  disarms it and can roll the die.
- **Reset at 1.4 g or below.** The reading has to fall back before another peak
  can count. Holding acceleration high produces a single trigger.
- **Cooldown of 750 ms.** A second peak within this interval cannot roll again.
  The comparison uses the sensor event timestamp in nanoseconds.

### Dragging the button

The play area in `RollScreen.kt` is a `BoxWithConstraints`. It gives the screen
its available width and height, so the code can limit movement on different
screen sizes. The button starts at the bottom center of the play area. Its width
is up to `220.dp` and its height is `52.dp`.

`pointerInput` calls `detectDragGestures` on the button. Each drag delta updates
`buttonPosition`, an `Offset` in pixels. The horizontal offset is limited to
half the remaining width on either side. The vertical offset ranges from the top
of the play area back down to the starting position. `absoluteOffset` applies the
pixel position to the button. The position is remembered while this screen is
composed.

The button's `onClick` still calls `roll()`. Once a drag crosses Compose's drag
threshold, the drag detector consumes the movement, so releasing after a drag
repositions the button without clicking it. The flick detector sits on the play
area behind the button, so the two gestures have separate touch targets.

### Flick detection and directional movement

`Modifier.flickToRoll` in `RollGestures.kt` tracks a single pointer from touch down
to release. It cancels if another control consumes the touch or if the pointer
sequence changes. It uses the straight-line displacement from start to finish
and the time elapsed. All three conditions must pass:

| Measure | Threshold |
|---|---|
| Distance | At least `48.dp` |
| Time | At most `250 ms` |
| Average speed | At least `650 dp/s` |

The distance and speed thresholds are converted to pixels for the current screen
density. A valid flick consumes the release and sends `displacement / distance`
to the screen. That gives the direction as a vector of length one, so a diagonal
flick keeps both its horizontal and vertical components.

The screen calls `roll()` on release, then animates a two-axis `Offset`. The die
moves out over `150 ms` and returns over `220 ms`. Travel is capped at `76.dp`
and at 20% of the play area's width and height. `graphicsLayer` applies the
translation to the image while **Current value** stays in place. Safe drawing
insets and clipping keep the play area clear of the system bars.

## Resources and build configuration

`strings.xml` supplies the gesture instructions, the text below the die, and the
image description announced by accessibility services. The last two use `%1$d`
to insert the current face. The app reuses the original drawable assets and the
existing **Roll** and **Result Screen** labels.

The version catalog declares the new library with the existing Lifecycle version
`2.11.0`:

```toml
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
```

The app module opts into it for `LifecycleResumeEffect`:

```kotlin
implementation(libs.androidx.lifecycle.runtime.compose)
```

The rest of the build configuration comes from the setup commit. That includes
Gradle `9.6.0`, Android Gradle Plugin `9.4.1`, the Gradle daemon JDK `25`, compile
and target SDK `37`, and min SDK `24`. The package is `com.example.sensordice`.
The accelerometer uses the Android SDK directly at the standard game sampling
rate, so this feature requires zero manifest permissions.

## Verify shake on an Android emulator

Keep Sensor Dice on its roll screen. On macOS, open **View → Tool Windows →
Terminal** in Android Studio and list the connected devices:

```bash
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
"$ADB" devices -l
```

If more than one emulator is running, check which one has Sensor Dice in the
foreground. Replace the serial below as needed. The output should contain
`com.example.sensordice`:

```bash
"$ADB" -s emulator-5556 shell dumpsys window | grep mCurrentFocus
```

Send a resting reading, a sharp acceleration, then reset to rest. Use the serial
for the emulator running the app:

```bash
SERIAL=emulator-5556
"$ADB" -s "$SERIAL" emu sensor set acceleration 0:0:9.81
sleep 1
"$ADB" -s "$SERIAL" emu sensor set acceleration 0:0:38
sleep 0.3
"$ADB" -s "$SERIAL" emu sensor set acceleration 0:0:9.81
```

The values are in metres per second squared. `38` exceeds the trigger threshold
and `9.81` brings the detector back below the reset threshold. `OK` means the
emulator accepted the input. Watch **Current value** and repeat if the roll lands
on the same face. To try the graphical controls, use the emulator toolbar's
**⋯ → Virtual sensors → Device Pose → Move**.

## Verification completed

`./gradlew testDebugUnitTest assembleDebug lintDebug` passed. The emulator checks
confirmed tap-to-roll, repositioning the button without rolling, a quick flick,
a simulated shake, and opening the result screen for the displayed face.
