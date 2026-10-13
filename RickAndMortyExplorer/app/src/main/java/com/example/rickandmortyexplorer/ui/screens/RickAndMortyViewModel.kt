package com.example.rickandmortyexplorer.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmortyexplorer.network.RickAndMortyApi
import com.example.rickandmortyexplorer.network.RickAndMortyApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.coroutineContext

sealed interface RickAndMortyUiState {
    data class Success(
        val totalItems: Int,
        val items: List<ExplorerItem>,
        val selectedItem: ExplorerItem?,
        val isRolling: Boolean = false
    ) : RickAndMortyUiState
    object Error : RickAndMortyUiState
    object Loading : RickAndMortyUiState
}

class RickAndMortyViewModel(
    private val apiService: RickAndMortyApiService = RickAndMortyApi.retrofitService
) : ViewModel() {
    var rickAndMortyUiState: RickAndMortyUiState by mutableStateOf(RickAndMortyUiState.Loading)
        private set
    var selectedCategory by mutableStateOf(ExplorerCategory.Character)
        private set
    var nameFilter by mutableStateOf("")
        private set

    private var requestJob: Job? = null

    init {
        loadItems()
    }

    fun selectCategory(category: ExplorerCategory) {
        if (category == selectedCategory) return
        selectedCategory = category
        nameFilter = ""
        loadItems()
    }

    fun updateNameFilter(name: String) {
        nameFilter = name
        loadItems(debounce = true)
    }

    fun retry() = loadItems()

    private fun loadItems(debounce: Boolean = false) {
        requestJob?.cancel()
        val category = selectedCategory
        val name = nameFilter.trim().ifBlank { null }
        rickAndMortyUiState = RickAndMortyUiState.Loading
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
                            ExplorerItem(it.id, it.name, it.episode, it.airDate,
                                relatedCharacterUrl = it.characters.firstOrNull())
                        }
                    }
                    ExplorerCategory.Location -> {
                        val response = apiService.getLocations(name = name)
                        response.info.count to response.results.map {
                            ExplorerItem(it.id, it.name, it.type, it.dimension,
                                relatedCharacterUrl = it.residents.firstOrNull())
                        }
                    }
                }
                RickAndMortyUiState.Success(total, items, items.randomOrNull()?.withRelatedImage())
            } catch (e: HttpException) {
                // The API returns 404 when a name filter has no matches.
                if (e.code() == 404) RickAndMortyUiState.Success(0, emptyList(), null)
                else RickAndMortyUiState.Error
            } catch (e: IOException) {
                RickAndMortyUiState.Error
            } catch (e: SerializationException) {
                RickAndMortyUiState.Error
            }
            coroutineContext.ensureActive()
            rickAndMortyUiState = state
        }
    }

    fun rollRandomItem() {
        val current = rickAndMortyUiState as? RickAndMortyUiState.Success ?: return
        if (current.isRolling) return
        val next = current.items.filter { it.id != current.selectedItem?.id }.randomOrNull() ?: return
        requestJob?.cancel()
        rickAndMortyUiState = current.copy(isRolling = true)
        requestJob = viewModelScope.launch {
            val selected = next.withRelatedImage()
            coroutineContext.ensureActive()
            rickAndMortyUiState = current.copy(selectedItem = selected)
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
