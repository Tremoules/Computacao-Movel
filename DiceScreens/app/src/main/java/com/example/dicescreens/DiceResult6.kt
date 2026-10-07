package com.example.dicescreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dicescreens.navigation.Screens
import com.example.dicescreens.ui.theme.DiceScreensTheme

fun diceImageFor(value: Int): Int {
    return when (value) {
        1 -> R.drawable.dice_1
        2 -> R.drawable.dice_2
        3 -> R.drawable.dice_3
        4 -> R.drawable.dice_4
        5 -> R.drawable.dice_5
        else -> R.drawable.dice_6
    }
}

@Preview(showBackground = true)
@Composable
fun DiceResult6Preview() {
    DiceScreensTheme {
        DiceResult6(navController = rememberNavController())
    }
}

@Composable
fun DiceResult6(
    navController: NavController,
    modifier: Modifier = Modifier
) {

    val currentValue = 6

    var secondRoll by remember { mutableStateOf<Int?>(null) }

    Column (
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Die Result: 6 (Dice Game)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row (
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Current Die",
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Image(
                    painter = painterResource(
                        diceImageFor(currentValue)
                    ),
                    contentDescription = "Current die showing $currentValue",
                    modifier = Modifier.size(96.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Second Die",
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (secondRoll != null) {
                    Image(
                        painter = painterResource(
                            diceImageFor(secondRoll!!)
                        ),
                        contentDescription = "Second die showing $secondRoll",
                        modifier = Modifier.size(96.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                secondRoll = (1..6).random()
            },
            modifier = Modifier.width(220.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Text(
                text = "Roll Second Die",
                fontSize = 20.sp
            )
        }

        secondRoll?.let { rolledValue ->
            val playerWon = rolledValue >= currentValue

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (playerWon) {
                    "You Won! ($currentValue ≤ $rolledValue)"
                } else {
                    "You Lost! ($currentValue > $rolledValue)"
                },
                fontWeight = FontWeight.Bold,
                color = if (playerWon) {
                    Color(0xFF2E7D32)
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }

        secondRoll?.let { rolledValue ->
            if (rolledValue >= currentValue) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        navController.navigate(
                            Screens.DiceResult.createRoute(rolledValue)
                        )
                    },
                    modifier = Modifier.width(220.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        text = "Go to Result",
                        fontSize = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button (
            onClick = { navController.navigate(Screens.Roll.route) },
            modifier = Modifier.width(220.dp)
        ) {
            Text(text = stringResource(R.string.back), fontSize = 20.sp)
        }
    }
}
