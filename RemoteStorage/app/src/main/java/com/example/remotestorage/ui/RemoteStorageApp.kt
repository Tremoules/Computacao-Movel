package com.example.remotestorage.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.remotestorage.R
import com.example.remotestorage.data.RemoteStorageProvider
import com.example.remotestorage.ui.screens.HomeScreen
import com.example.remotestorage.ui.screens.RemoteStorageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteStorageApp() {
    Scaffold(topBar = { RemoteStorageTopAppBar() }) { contentPadding ->
        Surface(modifier = Modifier.fillMaxSize()) {
            val context = LocalContext.current.applicationContext
            val remoteStorageViewModel: RemoteStorageViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        RemoteStorageViewModel(
                            storageRepository = RemoteStorageProvider.create(context)
                        )
                    }
                })
            HomeScreen(
                explorerUiState = remoteStorageViewModel.explorerUiState,
                selectedCategory = remoteStorageViewModel.selectedCategory,
                nameFilter = remoteStorageViewModel.nameFilter,
                onCategorySelected = remoteStorageViewModel::selectCategory,
                onNameFilterChanged = remoteStorageViewModel::updateNameFilter,
                onRoll = remoteStorageViewModel::rollRandomItem,
                onRetry = remoteStorageViewModel::retry,
                storageState = remoteStorageViewModel.storageUiState,
                onSave = remoteStorageViewModel::saveCurrentItem,
                onLoadLast = remoteStorageViewModel::loadLastSavedItem,
                onRetryStorageSync = remoteStorageViewModel::retryStorageSync,
                onDismissStorageMessage = remoteStorageViewModel::dismissStorageMessage,
                onBrowseApi = remoteStorageViewModel::browseApiItems,
                contentPadding = contentPadding
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteStorageTopAppBar(modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        title = { Text(text = stringResource(R.string.app_name)) }, modifier = modifier
    )
}
