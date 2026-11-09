package com.example.sensorexplorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.sensorexplorer.ui.SensorExplorerApp
import com.example.sensorexplorer.ui.theme.SensorExplorerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SensorExplorerTheme {
                SensorExplorerApp()
            }
        }
    }
}
