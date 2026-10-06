package com.example.dicescreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dicescreens.navigation.Screens

@Composable
fun DiceResult2(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var currentValue by remember { mutableStateOf(2) }

    val diceImage = when (currentValue) {
        1 -> R.drawable.dice_1
        2 -> R.drawable.dice_2
        3 -> R.drawable.dice_3
        4 -> R.drawable.dice_4
        5 -> R.drawable.dice_5
        else -> R.drawable.dice_6
    }

    Column (
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Die Result: 2",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(diceImage),
            contentDescription = "Die showing $currentValue",
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Current Value: $currentValue",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                currentValue = when (currentValue) {
                    2 -> 4
                    4 -> 6
                    else -> 2
                }
            },
            modifier = Modifier.width(220.dp)
        ) {
            Text(
                text = "Increment by +2",
                fontSize = 12.sp
            )
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