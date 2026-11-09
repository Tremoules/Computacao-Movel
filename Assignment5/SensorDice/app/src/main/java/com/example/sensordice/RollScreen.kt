package com.example.sensordice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.sensordice.navigation.Screens
import com.example.sensordice.ui.theme.SensorDiceTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DiceWithRollerAndImage(navController: NavController, modifier: Modifier = Modifier) {
    var result by rememberSaveable { mutableIntStateOf(1) }
    val roll = { result = (1..6).random() }
    RollOnShake(onShake = roll)

    val dieImage = when (result) {
        1 -> R.drawable.dice_1
        2 -> R.drawable.dice_2
        3 -> R.drawable.dice_3
        4 -> R.drawable.dice_4
        5 -> R.drawable.dice_5
        else -> R.drawable.dice_6
    }

    Column(
        modifier = modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.roll_gesture_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            val density = LocalDensity.current
            val scope = rememberCoroutineScope()
            val toss = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
            val tossDistance = with(density) { minOf(76.dp, maxWidth * 0.2f, maxHeight * 0.2f).toPx() }
            val dieSize = minOf(200.dp, maxWidth, maxHeight * 0.55f)
            val buttonWidth = minOf(220.dp, maxWidth)
            val buttonHeight = 52.dp
            val horizontalLimit = with(density) { (maxWidth - buttonWidth).toPx() / 2f }
            val upwardLimit = with(density) { (maxHeight - buttonHeight).coerceAtLeast(0.dp).toPx() }
            var buttonPosition by remember { mutableStateOf(Offset.Zero) }

            Column(
                modifier = Modifier.fillMaxSize().flickToRoll { direction ->
                    roll()
                    scope.launch {
                        toss.animateTo(direction * tossDistance, tween(150))
                        toss.animateTo(Offset.Zero, tween(220))
                    }
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(dieImage),
                    contentDescription = stringResource(R.string.die_face, result),
                    modifier = Modifier
                        .size(dieSize)
                        .graphicsLayer {
                            translationX = toss.value.x
                            translationY = toss.value.y
                        }
                )
                Text(
                    text = stringResource(R.string.current_value, result),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Button(
                onClick = roll,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .absoluteOffset {
                        IntOffset(
                            buttonPosition.x.coerceIn(-horizontalLimit, horizontalLimit).roundToInt(),
                            buttonPosition.y.coerceIn(-upwardLimit, 0f).roundToInt()
                        )
                    }
                    .width(buttonWidth)
                    .height(buttonHeight)
                    .pointerInput(horizontalLimit, upwardLimit) {
                        detectDragGestures { _, delta ->
                            val x = buttonPosition.x.coerceIn(-horizontalLimit, horizontalLimit)
                            val y = buttonPosition.y.coerceIn(-upwardLimit, 0f)
                            buttonPosition = Offset(
                                (x + delta.x).coerceIn(-horizontalLimit, horizontalLimit),
                                (y + delta.y).coerceIn(-upwardLimit, 0f)
                            )
                        }
                    }
            ) {
                Text(stringResource(R.string.roll), style = MaterialTheme.typography.titleMedium)
            }
        }

        Button(
            onClick = { navController.navigate(Screens.DiceResult.createRoute(result)) },
            modifier = Modifier.width(220.dp)
        ) {
            Text(stringResource(R.string.result_screen), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RollScreenPreview() {
    SensorDiceTheme {
        DiceWithRollerAndImage(navController = rememberNavController())
    }
}
