package com.example.rickandmortyexplorer.ui.screens

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
import com.example.rickandmortyexplorer.R
import com.example.rickandmortyexplorer.ui.theme.RickAndMortyExplorerTheme

@Composable
fun HomeScreen(
    rickAndMortyUiState: RickAndMortyUiState,
    selectedCategory: ExplorerCategory,
    nameFilter: String,
    onCategorySelected: (ExplorerCategory) -> Unit,
    onNameFilterChanged: (String) -> Unit,
    onRoll: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
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
            modifier = Modifier.fillMaxWidth()
        )
        when (rickAndMortyUiState) {
            is RickAndMortyUiState.Loading -> LoadingScreen()
            is RickAndMortyUiState.Error -> ErrorScreen(onRetry = onRetry)
            is RickAndMortyUiState.Success -> ResultScreen(
                result = rickAndMortyUiState,
                category = selectedCategory,
                onRoll = onRoll
            )
        }
    }
}

@Composable
fun ResultScreen(
    result: RickAndMortyUiState.Success,
    category: ExplorerCategory,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = result.selectedItem
    val imageCount = (result.items.mapNotNull { it.image } + listOfNotNull(item?.image)).distinct().size
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.api_stats), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.total_items, result.totalItems))
                Text(stringResource(R.string.retrieved_items, result.items.size))
                Text(stringResource(R.string.retrieved_images, imageCount))
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
            enabled = result.items.size > 1 && !result.isRolling,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(if (result.isRolling) R.string.rolling else R.string.roll_random_item))
        }
        if (result.items.size == 1) Text(stringResource(R.string.only_one_result))
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
    RickAndMortyExplorerTheme {
        HomeScreen(
            rickAndMortyUiState = RickAndMortyUiState.Success(826, listOf(item), item),
            selectedCategory = ExplorerCategory.Character,
            nameFilter = "",
            onCategorySelected = {}, onNameFilterChanged = {}, onRoll = {}, onRetry = {}
        )
    }
}
