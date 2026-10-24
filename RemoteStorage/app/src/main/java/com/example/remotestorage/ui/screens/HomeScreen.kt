package com.example.remotestorage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.remotestorage.R
import com.example.remotestorage.ui.theme.RemoteStorageTheme

@Composable
fun HomeScreen(
    explorerUiState: ExplorerUiState,
    selectedCategory: ExplorerCategory,
    nameFilter: String,
    onCategorySelected: (ExplorerCategory) -> Unit,
    onNameFilterChanged: (String) -> Unit,
    onRoll: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    storageState: RemoteStorageUiState = RemoteStorageUiState(),
    onSave: () -> Unit = {},
    onLoadLast: () -> Unit = {},
    onRetryStorageSync: () -> Unit = {},
    onDismissStorageMessage: () -> Unit = {},
    onBrowseApi: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Column(
        modifier = modifier.fillMaxSize().padding(contentPadding)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.select_category), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExplorerCategory.entries.forEach { category ->
                FilterChip(
                    selected = category == selectedCategory,
                    enabled = !storageState.isLoadingLast,
                    onClick = { onCategorySelected(category) },
                    label = { Text(stringResource(category.label)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        OutlinedTextField(
            value = nameFilter,
            onValueChange = onNameFilterChanged,
            label = { Text(stringResource(R.string.filter_by_name, stringResource(selectedCategory.label))) },
            singleLine = true,
            enabled = !storageState.isLoadingLast,
            modifier = Modifier.fillMaxWidth()
        )
        val result = explorerUiState as? ExplorerUiState.Success
        RemoteStoragePanel(
            state = storageState,
            canSave = result?.selectedItem != null && !result.isRolling,
            onSave = onSave,
            onLoadLast = onLoadLast,
            onRetrySync = onRetryStorageSync,
            onDismissMessage = onDismissStorageMessage
        )
        when (explorerUiState) {
            is ExplorerUiState.Loading -> LoadingScreen()
            is ExplorerUiState.Error -> ErrorScreen(onRetry = onRetry)
            is ExplorerUiState.Success -> ResultScreen(
                result = explorerUiState,
                category = selectedCategory,
                onRoll = onRoll,
                allowRoll = !storageState.isLoadingLast,
                onBrowseApi = onBrowseApi
            )
        }
    }
}

@Composable
fun ResultScreen(
    result: ExplorerUiState.Success,
    category: ExplorerCategory,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
    allowRoll: Boolean = true,
    onBrowseApi: () -> Unit = {}
) {
    val item = result.selectedItem
    val imageCount = (result.items.mapNotNull { it.image } + listOfNotNull(item?.image)).distinct().size
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (result.isSavedSnapshot) {
                    Text(stringResource(R.string.saved_snapshot), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.saved_snapshot_description))
                    Button(onClick = onBrowseApi, enabled = allowRoll) {
                        Text(stringResource(R.string.browse_api))
                    }
                } else {
                    Text(stringResource(R.string.api_stats), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.total_items, result.totalItems))
                    Text(stringResource(R.string.retrieved_items, result.items.size))
                    Text(stringResource(R.string.retrieved_images, imageCount))
                }
            }
        }
        if (item == null) {
            Text(stringResource(R.string.no_results))
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val placeholder = painterResource(R.drawable.image_placeholder)
                    AsyncImage(
                        model = item.image,
                        contentDescription = item.relatedCharacterName?.let {
                            stringResource(R.string.related_character_image, it)
                        } ?: stringResource(R.string.item_image, item.name),
                        placeholder = placeholder,
                        error = placeholder,
                        fallback = placeholder,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(200.dp)
                    )
                    if (item.image == null) {
                        Text(stringResource(R.string.image_unavailable))
                    } else if (item.relatedCharacterName != null) {
                        Text(stringResource(R.string.related_character_image, item.relatedCharacterName),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text(item.name, style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.item_field, stringResource(category.firstFieldLabel),
                        item.firstField.ifBlank { stringResource(R.string.unknown) }))
                    Text(stringResource(R.string.item_field, stringResource(category.secondFieldLabel),
                        item.secondField.ifBlank { stringResource(R.string.unknown) }))
                }
            }
        }
        Button(
            onClick = onRoll,
            enabled = allowRoll && result.items.size > 1 && !result.isRolling,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(if (result.isRolling) R.string.rolling else R.string.roll_random_item))
        }
        if (result.items.size == 1 && !result.isSavedSnapshot) Text(stringResource(R.string.only_one_result))
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.loading))
    }
}

@Composable
fun ErrorScreen(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.loading_failed))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val item = ExplorerItem(1, "Rick Sanchez", "Alive", "Human")
    RemoteStorageTheme {
        HomeScreen(
            explorerUiState = ExplorerUiState.Success(826, listOf(item), item),
            selectedCategory = ExplorerCategory.Character,
            nameFilter = "",
            onCategorySelected = {}, onNameFilterChanged = {}, onRoll = {}, onRetry = {}
        )
    }
}
