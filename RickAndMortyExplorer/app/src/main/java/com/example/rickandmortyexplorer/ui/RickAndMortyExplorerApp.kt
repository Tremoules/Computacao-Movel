package com.example.rickandmortyexplorer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rickandmortyexplorer.R
import com.example.rickandmortyexplorer.ui.screens.HomeScreen
import com.example.rickandmortyexplorer.ui.screens.RickAndMortyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RickAndMortyExplorerApp() {
    Scaffold(topBar = { RickAndMortyTopAppBar() }) { contentPadding ->
        Surface(modifier = Modifier.fillMaxSize()) {
            val rickAndMortyViewModel: RickAndMortyViewModel = viewModel()
            HomeScreen(
                rickAndMortyUiState = rickAndMortyViewModel.rickAndMortyUiState,
                selectedCategory = rickAndMortyViewModel.selectedCategory,
                nameFilter = rickAndMortyViewModel.nameFilter,
                onCategorySelected = rickAndMortyViewModel::selectCategory,
                onNameFilterChanged = rickAndMortyViewModel::updateNameFilter,
                onRoll = rickAndMortyViewModel::rollRandomItem,
                onRetry = rickAndMortyViewModel::retry,
                contentPadding = contentPadding
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RickAndMortyTopAppBar(modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        title = { Text(text = stringResource(R.string.app_name)) },
        modifier = modifier
    )
}
