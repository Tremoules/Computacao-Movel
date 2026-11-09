package com.example.sensordice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.sensordice.navigation.NavGraph
import com.example.sensordice.ui.theme.SensorDiceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SensorDiceTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
