package com.example.dicescreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dicescreens.navigation.Screens
import com.example.dicescreens.ui.theme.DiceScreensTheme

@Preview(showBackground = true)
@Composable
fun DiceResult3Preview() {
    DiceScreensTheme {
        DiceResult3(navController = rememberNavController())
    }
}

@Composable
fun DiceResult3(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("3") } // An input value can temporarily be null so it needs to be a string
    val currentValue = input.toIntOrNull()

    val diceImage = when (currentValue) {
        1 -> R.drawable.dice_1
        2 -> R.drawable.dice_2
        3 -> R.drawable.dice_3
        4 -> R.drawable.dice_4
        5 -> R.drawable.dice_5
        6 -> R.drawable.dice_6
        else -> null
    }

    Column (
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Die Result: 3",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (diceImage != null) {
            Image(
                painter = painterResource(diceImage),
                contentDescription = "Die showing $currentValue"
            )
        }


        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (currentValue in 1..6) {
                "Current Value: $currentValue"
            } else {
                "Enter a number from 1 to 6"
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { newText ->
                if (
                    newText.isEmpty() ||
                    newText.toIntOrNull() in 1..6
                ) {
                    input = newText
                }
            },
            label = {
                Text("Enter a value")
            },
            singleLine = true,
            modifier = Modifier.width(220.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button (
            onClick = { navController.navigate(Screens.Roll.route) },
            modifier = Modifier.width(220.dp)
        ) {
            Text(text = stringResource(R.string.back), fontSize = 20.sp)
        }
    }
}
