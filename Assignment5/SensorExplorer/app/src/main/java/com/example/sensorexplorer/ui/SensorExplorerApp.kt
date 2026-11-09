package com.example.sensorexplorer.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sensorexplorer.R
import com.example.sensorexplorer.data.LocationPhoto
import com.example.sensorexplorer.data.LocationPhotoStore
import com.example.sensorexplorer.data.RemoteStorageProvider
import com.example.sensorexplorer.ui.camera.LocationCameraActivity
import com.example.sensorexplorer.ui.screens.ExplorerCategory
import com.example.sensorexplorer.ui.screens.ExplorerUiState
import com.example.sensorexplorer.ui.screens.HomeScreen
import com.example.sensorexplorer.ui.screens.RemoteStorageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorExplorerApp() {
    Scaffold(topBar = { SensorExplorerTopAppBar() }) { contentPadding ->
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
            val photoStore = remember(context) { LocationPhotoStore(context) }
            var photoRevision by remember { mutableIntStateOf(0) }
            val camera = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) photoRevision++
            }
            val explorerState = remoteStorageViewModel.explorerUiState
            val category = remoteStorageViewModel.selectedCategory
            val location = (explorerState as? ExplorerUiState.Success)?.selectedItem
                ?.takeIf { category == ExplorerCategory.Location }
            val photo by key(location?.id, photoRevision) {
                produceState<LocationPhoto?>(null) {
                    value = location?.id?.let { photoStore.read(it) }
                }
            }
            HomeScreen(
                explorerUiState = explorerState,
                selectedCategory = category,
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
                locationPhoto = photo,
                onCaptureLocationPhoto = {
                    location?.let { camera.launch(LocationCameraActivity.intent(context, it.id, it.name)) }
                },
                contentPadding = contentPadding
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorExplorerTopAppBar(modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        title = { Text(text = stringResource(R.string.app_name)) }, modifier = modifier
    )
}
