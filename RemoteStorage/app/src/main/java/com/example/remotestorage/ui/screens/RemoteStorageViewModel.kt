package com.example.remotestorage.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.remotestorage.R
import com.example.remotestorage.data.RemoteStorageRepository
import com.example.remotestorage.data.SavedExplorerItem
import com.example.remotestorage.network.RickAndMortyApi
import com.example.remotestorage.network.RickAndMortyApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.milliseconds

class RemoteStorageViewModel(
    private val apiService: RickAndMortyApiService = RickAndMortyApi.retrofitService,
    private val storageRepository: RemoteStorageRepository? = null
) : ViewModel() {
    var explorerUiState: ExplorerUiState by mutableStateOf(ExplorerUiState.Loading)
        private set
    var selectedCategory by mutableStateOf(ExplorerCategory.Character)
        private set
    var nameFilter by mutableStateOf("")
        private set

    private var requestJob: Job? = null
    private var rollCountJob: Job? = null
    private var historyJob: Job? = null

    var storageUiState by mutableStateOf(RemoteStorageUiState(isConfigured = storageRepository != null))
        private set

    init {
        loadItems()
        retryStorageSync()
    }

    fun selectCategory(category: ExplorerCategory) {
        if (storageUiState.isLoadingLast) return
        if (category == selectedCategory) return
        selectedCategory = category
        nameFilter = ""
        loadItems()
    }

    fun updateNameFilter(name: String) {
        if (storageUiState.isLoadingLast) return
        nameFilter = name
        loadItems(debounce = true)
    }

    fun retry() {
        if (!storageUiState.isLoadingLast) loadItems()
    }

    private fun loadItems(debounce: Boolean = false) {
        requestJob?.cancel()
        val category = selectedCategory
        val name = nameFilter.trim().ifBlank { null }
        explorerUiState = ExplorerUiState.Loading
        requestJob = viewModelScope.launch {
            if (debounce) delay(300)
            val state = try {
                // Fetch one page, matching the original app's retrieved-page behavior.
                val (total, items) = when (category) {
                    ExplorerCategory.Character -> {
                        val response = apiService.getCharacters(name = name)
                        response.info.count to response.results.map {
                            ExplorerItem(it.id, it.name, it.status, it.species, image = it.image)
                        }
                    }

                    ExplorerCategory.Episode -> {
                        val response = apiService.getEpisodes(name = name)
                        response.info.count to response.results.map {
                            ExplorerItem(
                                it.id,
                                it.name,
                                it.episode,
                                it.airDate,
                                relatedCharacterUrl = it.characters.firstOrNull()
                            )
                        }
                    }

                    ExplorerCategory.Location -> {
                        val response = apiService.getLocations(name = name)
                        response.info.count to response.results.map {
                            ExplorerItem(
                                it.id,
                                it.name,
                                it.type,
                                it.dimension,
                                relatedCharacterUrl = it.residents.firstOrNull()
                            )
                        }
                    }
                }
                ExplorerUiState.Success(total, items, items.randomOrNull()?.withRelatedImage())
            } catch (e: HttpException) {
                // The API returns 404 when a name filter has no matches.
                if (e.code() == 404) ExplorerUiState.Success(0, emptyList(), null)
                else ExplorerUiState.Error
            } catch (e: IOException) {
                ExplorerUiState.Error
            } catch (e: SerializationException) {
                ExplorerUiState.Error
            }
            coroutineContext.ensureActive()
            explorerUiState = state
        }
    }

    fun rollRandomItem() {
        if (storageUiState.isLoadingLast) return
        val current = explorerUiState as? ExplorerUiState.Success ?: return
        if (current.isRolling) return
        val next =
            current.items.filter { it.id != current.selectedItem?.id }.randomOrNull() ?: return
        requestJob?.cancel()
        explorerUiState = current.copy(isRolling = true)
        // This job is independent of API requests: changing category must not lose a roll.
        storageRepository?.let { repository ->
            viewModelScope.launch {
                storageOperation(R.string.roll_sync_failed) {
                    repository.incrementRollCount()
                }
            }
        }
        requestJob = viewModelScope.launch {
            val selected = next.withRelatedImage()
            coroutineContext.ensureActive()
            explorerUiState = current.copy(selectedItem = selected)
        }
    }

    fun retryStorageSync() {
        val repository = storageRepository ?: return
        rollCountJob?.cancel()
        historyJob?.cancel()
        storageUiState = storageUiState.copy(hasSyncError = false, isHistoryLoading = true)
        rollCountJob = viewModelScope.launch {
            repository.observeRollCount().catch {
                storageUiState = storageUiState.copy(hasSyncError = true)
            }.collect { count -> storageUiState = storageUiState.copy(rollCount = count) }
        }
        historyJob = viewModelScope.launch {
            repository.observeHistory().catch {
                storageUiState = storageUiState.copy(hasSyncError = true, isHistoryLoading = false)
            }.collect { items ->
                storageUiState = storageUiState.copy(history = items, isHistoryLoading = false)
            }
        }
    }

    fun saveCurrentItem() {
        val repository = storageRepository ?: return
        val current = explorerUiState as? ExplorerUiState.Success ?: return
        val item = current.selectedItem ?: return
        if (storageUiState.isSaving || storageUiState.isLoadingLast || current.isRolling) return
        // Capture the category and displayed values now, before any suspension or navigation.
        val snapshot = SavedExplorerItem(
            id = item.id,
            category = selectedCategory.name,
            name = item.name,
            firstField = item.firstField,
            secondField = item.secondField,
            imageUrl = item.image,
            relatedCharacterName = item.relatedCharacterName
        )
        storageUiState = storageUiState.copy(isSaving = true, message = null)
        viewModelScope.launch {
            try {
                storageOperation(R.string.save_failed) {
                    repository.save(snapshot)
                    storageUiState = storageUiState.copy(message = R.string.item_saved)
                }
            } finally {
                storageUiState = storageUiState.copy(isSaving = false)
            }
        }
    }

    fun loadLastSavedItem() {
        val repository = storageRepository ?: return
        if (storageUiState.isSaving || storageUiState.isLoadingLast) return
        storageUiState = storageUiState.copy(isLoadingLast = true, message = null)
        viewModelScope.launch {
            try {
                storageOperation(R.string.load_failed) {
                    val saved = repository.loadLast()
                    if (saved == null) {
                        storageUiState = storageUiState.copy(message = R.string.no_saved_item)
                        return@storageOperation
                    }
                    val category =
                        ExplorerCategory.entries.firstOrNull { it.name == saved.category }
                    require(category != null && saved.id > 0 && saved.name.isNotBlank())
                    requestJob?.cancel()
                    selectedCategory = category
                    nameFilter = ""
                    val item = ExplorerItem(
                        saved.id,
                        saved.name,
                        saved.firstField,
                        saved.secondField,
                        image = saved.imageUrl,
                        relatedCharacterName = saved.relatedCharacterName
                    )
                    explorerUiState = ExplorerUiState.Success(
                        1, listOf(item), item, isSavedSnapshot = true
                    )
                    storageUiState = storageUiState.copy(message = R.string.item_loaded)
                }
            } finally {
                storageUiState = storageUiState.copy(isLoadingLast = false)
            }
        }
    }

    fun browseApiItems() {
        if (!storageUiState.isLoadingLast) loadItems()
    }

    fun dismissStorageMessage() {
        storageUiState = storageUiState.copy(message = null)
    }

    private suspend fun storageOperation(errorMessage: Int, action: suspend () -> Unit) {
        try {
            // Firebase can queue writes while offline. A timeout ends the spinner, not the write.
            val completed = withTimeoutOrNull(15_000.milliseconds) { action(); true } ?: false
            if (!completed) storageUiState = storageUiState.copy(message = R.string.storage_timeout)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            storageUiState = storageUiState.copy(message = errorMessage)
        }
    }

    private suspend fun ExplorerItem.withRelatedImage(): ExplorerItem {
        val id = relatedCharacterUrl?.substringAfterLast('/')?.toIntOrNull() ?: return this
        return try {
            val character = apiService.getCharacter(id)
            copy(image = character.image, relatedCharacterName = character.name)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            this
        } catch (e: HttpException) {
            this
        } catch (e: SerializationException) {
            this
        }
    }
}
