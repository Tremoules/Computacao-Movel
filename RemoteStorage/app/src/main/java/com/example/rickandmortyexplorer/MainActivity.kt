package com.example.rickandmortyexplorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.rickandmortyexplorer.ui.RickAndMortyExplorerApp
import com.example.rickandmortyexplorer.ui.theme.RickAndMortyExplorerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RickAndMortyExplorerTheme {
                RickAndMortyExplorerApp()
            }
        }
    }
}
