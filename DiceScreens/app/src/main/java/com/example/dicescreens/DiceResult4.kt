package com.example.dicescreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dicescreens.navigation.Screens

@Composable
fun DiceResult4(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column (
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Die Result: 4",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select a screen to navigate to:",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        for (screenNumber in 1..6) {
            Button(
                onClick = {
                    navController.navigate(
                        Screens.DiceResult.createRoute(screenNumber)
                    )
                },
                modifier = Modifier
                    .width(220.dp)
                    .height(40.dp)
            ) {
                Text(
                    text = "Go to Screen $screenNumber",
                    fontSize = 16.sp
                )
            }

            if (screenNumber < 6) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button (
            onClick = { navController.navigate(Screens.Roll.route) },
            modifier = Modifier.width(220.dp)
        ) {
            Text(text = stringResource(R.string.back), fontSize = 20.sp)
        }
    }
}