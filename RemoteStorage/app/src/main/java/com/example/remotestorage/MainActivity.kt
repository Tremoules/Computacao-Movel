package com.example.remotestorage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.remotestorage.ui.RemoteStorageApp
import com.example.remotestorage.ui.theme.RemoteStorageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RemoteStorageTheme {
                RemoteStorageApp()
            }
        }
    }
}
