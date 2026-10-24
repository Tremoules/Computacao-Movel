package com.example.remotestorage.ui.screens

import com.example.remotestorage.network.RickAndMortyApiService
import com.example.remotestorage.R
import com.example.remotestorage.data.RemoteStorageRepository
import com.example.remotestorage.data.SavedExplorerItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.example.remotestorage.network.RickAndMortyCharacter
import com.example.remotestorage.network.RickAndMortyEpisode
import com.example.remotestorage.network.RickAndMortyInfo
import com.example.remotestorage.network.RickAndMortyLocation
import com.example.remotestorage.network.RickAndMortyResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteStorageViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var api: FakeApi

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        api = FakeApi()
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoadShowsCountsAndRandomCharacterWithItsFields() = runTest(dispatcher) {
        val model = RemoteStorageViewModel(api)
        assertEquals(ExplorerUiState.Loading, model.explorerUiState)
        advanceUntilIdle()
        val state = model.success()
        assertEquals(826, state.totalItems)
        assertEquals(2, state.items.size)
        assertTrue(state.items.contains(state.selectedItem))
        assertEquals("Alive", state.selectedItem!!.firstField)
        assertEquals("Human", state.selectedItem.secondField)
        assertNotNull(state.selectedItem.image)
    }

    @Test
    fun rapidTypingOnlyRequestsLatestTrimmedNameAndClearingRemovesFilter() = runTest(dispatcher) {
        val model = RemoteStorageViewModel(api)
        advanceUntilIdle()
        model.updateNameFilter("r")
        advanceTimeBy(100)
        model.updateNameFilter("  rick  ")
        advanceUntilIdle()
        assertEquals(listOf(null, "rick"), api.characterNames)
        model.updateNameFilter("   ")
        advanceUntilIdle()
        assertNull(api.characterNames.last())
    }

    @Test
    fun categorySwitchClearsNameAndUsesEpisodeSchemaAndRelatedImage() = runTest(dispatcher) {
        val model = RemoteStorageViewModel(api)
        advanceUntilIdle()
        model.updateNameFilter("rick")
        model.selectCategory(ExplorerCategory.Episode)
        advanceUntilIdle()
        assertEquals("", model.nameFilter)
        assertEquals(listOf<String?>(null), api.episodeNames)
        assertEquals(listOf<Int>(1), api.relatedIds)
        val item = model.success().selectedItem!!
        assertEquals("Pilot", item.name)
        assertEquals("S01E01", item.firstField)
        assertEquals("December 2, 2013", item.secondField)
        assertEquals("Rick Sanchez", item.relatedCharacterName)
        assertEquals("https://example.com/1.jpeg", item.image)
    }

    @Test
    fun locationWithoutResidentsShowsItsFieldsWithoutFetchingAnImage() = runTest(dispatcher) {
        val model = RemoteStorageViewModel(api)
        model.selectCategory(ExplorerCategory.Location)
        advanceUntilIdle()
        val item = model.success().selectedItem!!
        assertEquals("Planet", item.firstField)
        assertEquals("Dimension C-137", item.secondField)
        assertNull(item.image)
        assertTrue(api.relatedIds.isEmpty())
        model.updateNameFilter("earth")
        advanceUntilIdle()
        assertEquals("earth", api.locationNames.last())
    }

    @Test
    fun episodeNameFilterReachesEpisodeEndpoint() = runTest(dispatcher) {
        val model = RemoteStorageViewModel(api)
        model.selectCategory(ExplorerCategory.Episode)
        advanceUntilIdle()
        model.updateNameFilter("pilot")
        advanceUntilIdle()
        assertEquals("pilot", api.episodeNames.last())
    }

    @Test
    fun noMatch404IsEmptySuccessButServerErrorsOfferRetry() = runTest(dispatcher) {
        val model = RemoteStorageViewModel(api)
        advanceUntilIdle()
        api.characterFailure = httpError(404)
        model.updateNameFilter("nonexistent")
        advanceUntilIdle()
        assertEquals(0, model.success().totalItems)
        assertTrue(model.success().items.isEmpty())
        assertNull(model.success().selectedItem)
        api.characterFailure = httpError(500)
        model.retry()
        advanceUntilIdle()
        assertEquals(ExplorerUiState.Error, model.explorerUiState)
        api.characterFailure = null
        model.retry()
        advanceUntilIdle()
        assertEquals(2, model.success().items.size)
    }

    @Test
    fun networkFailureCanBeRetried() = runTest(dispatcher) {
        api.characterFailure = IOException("offline")
        val model = RemoteStorageViewModel(api)
        advanceUntilIdle()
        assertEquals(ExplorerUiState.Error, model.explorerUiState)
        api.characterFailure = null
        model.retry()
        advanceUntilIdle()
        assertEquals(2, model.success().items.size)
    }

    @Test
    fun rollAlwaysChangesItemAndDoesNotRefetchCharacterList() = runTest(dispatcher) {
        val model = RemoteStorageViewModel(api)
        advanceUntilIdle()
        repeat(10) {
            val previous = model.success().selectedItem!!.id
            model.rollRandomItem()
            advanceUntilIdle()
            assertNotEquals(previous, model.success().selectedItem!!.id)
        }
        assertEquals(1, api.characterNames.size)
    }

    @Test
    fun singletonRollLeavesCurrentItemUnchanged() = runTest(dispatcher) {
        val model = RemoteStorageViewModel(api)
        model.selectCategory(ExplorerCategory.Location)
        advanceUntilIdle()
        val previous = model.success()
        model.rollRandomItem()
        advanceUntilIdle()
        assertEquals(previous, model.success())
    }

    @Test
    fun failedRelatedImageLookupPreservesEpisodeResult() = runTest(dispatcher) {
        api.relatedFailure = IOException("image lookup offline")
        val model = RemoteStorageViewModel(api)
        model.selectCategory(ExplorerCategory.Episode)
        advanceUntilIdle()
        assertEquals("Pilot", model.success().selectedItem!!.name)
        assertNull(model.success().selectedItem!!.image)
    }

    @Test
    fun olderRequestCannotReplaceNewCategoryEvenIfItFinishesAfterCancellation() = runTest(dispatcher) {
        api.slowCharacters = true
        val model = RemoteStorageViewModel(api)
        runCurrent()
        model.selectCategory(ExplorerCategory.Location)
        advanceUntilIdle()
        assertEquals(ExplorerCategory.Location, model.selectedCategory)
        assertEquals("Earth", model.success().selectedItem!!.name)
    }

    @Test
    fun switchingCategoryDuringRelatedImageLookupCannotRestoreOldResult() = runTest(dispatcher) {
        api.slowRelatedImage = true
        val model = RemoteStorageViewModel(api)
        model.selectCategory(ExplorerCategory.Episode)
        runCurrent()
        model.selectCategory(ExplorerCategory.Location)
        advanceUntilIdle()
        assertEquals("Earth", model.success().selectedItem!!.name)
    }

    @Test
    fun savingAllCategoriesKeepsTheDisplayedSnapshotAndAppendsRepeatedSaves() = runTest(dispatcher) {
        val storage = FakeStorage()
        val model = RemoteStorageViewModel(api, storage)
        for (category in ExplorerCategory.entries) {
            model.selectCategory(category)
            advanceUntilIdle()
            val displayed = model.success().selectedItem!!
            model.saveCurrentItem()
            advanceUntilIdle()
            val saved = storage.saved.last()
            assertEquals(category.name, saved.category)
            assertEquals(displayed.id, saved.id)
            assertEquals(displayed.name, saved.name)
            assertEquals(displayed.firstField, saved.firstField)
            assertEquals(displayed.secondField, saved.secondField)
            assertEquals(displayed.image, saved.imageUrl)
            assertEquals(R.string.item_saved, model.storageUiState.message)
        }
        model.saveCurrentItem()
        advanceUntilIdle()
        assertEquals(4, storage.saved.size)
        assertEquals(storage.saved.asReversed(), model.storageUiState.history)
    }

    @Test
    fun loadingLastRestoresCategoryAndFieldsWithoutCallingTheApiOrAddingHistory() = runTest(dispatcher) {
        val storage = FakeStorage()
        storage.last = SavedExplorerItem(9, "Episode", "Saved pilot", "S01E01", "December 2, 2013",
            "https://example.com/saved.jpg", "Rick Sanchez", 1000)
        val model = RemoteStorageViewModel(api, storage)
        advanceUntilIdle()
        model.updateNameFilter("rick")
        model.loadLastSavedItem()
        advanceUntilIdle()
        assertEquals(ExplorerCategory.Episode, model.selectedCategory)
        assertEquals("", model.nameFilter)
        assertEquals("Saved pilot", model.success().selectedItem!!.name)
        assertEquals("S01E01", model.success().selectedItem!!.firstField)
        assertEquals("https://example.com/saved.jpg", model.success().selectedItem!!.image)
        assertTrue(model.success().isSavedSnapshot)
        assertTrue(api.episodeNames.isEmpty())
        assertTrue(storage.saved.isEmpty())
        assertEquals(0L, model.storageUiState.rollCount)
        model.browseApiItems()
        advanceUntilIdle()
        assertFalse(model.success().isSavedSnapshot)
        assertEquals(listOf<String?>(null), api.episodeNames)
    }

    @Test
    fun rollsAreStoredOncePerAcceptedClickAndReadFromDatabase() = runTest(dispatcher) {
        val storage = FakeStorage()
        storage.count.value = 41
        val model = RemoteStorageViewModel(api, storage)
        advanceUntilIdle()
        assertEquals(41L, model.storageUiState.rollCount)
        model.rollRandomItem()
        model.rollRandomItem() // Ignored while the first roll is in progress.
        advanceUntilIdle()
        assertEquals(42L, storage.count.value)
        assertEquals(42L, model.storageUiState.rollCount)
        model.selectCategory(ExplorerCategory.Location)
        advanceUntilIdle()
        model.rollRandomItem() // No alternative item: not a roll.
        advanceUntilIdle()
        assertEquals(42L, storage.count.value)
        storage.count.value = 50 // A second client changed the shared value.
        advanceUntilIdle()
        assertEquals(50L, model.storageUiState.rollCount)
    }

    @Test
    fun loadingSavedItemCancelsAnOlderApiRequestEvenIfItIgnoresCancellation() = runTest(dispatcher) {
        api.slowCharacters = true
        val storage = FakeStorage().apply {
            last = SavedExplorerItem(9, "Episode", "Saved episode", "S01E09", "A date")
        }
        val model = RemoteStorageViewModel(api, storage)
        runCurrent()
        model.loadLastSavedItem()
        advanceUntilIdle()
        assertEquals(ExplorerCategory.Episode, model.selectedCategory)
        assertEquals("Saved episode", model.success().selectedItem!!.name)
        assertTrue(model.success().isSavedSnapshot)
    }

    @Test
    fun changingCategoryDoesNotCancelAnAcceptedRollWrite() = runTest(dispatcher) {
        val storage = FakeStorage().apply { slowWrite = true }
        val model = RemoteStorageViewModel(api, storage)
        advanceUntilIdle()
        model.rollRandomItem()
        model.selectCategory(ExplorerCategory.Location)
        advanceUntilIdle()
        assertEquals(1L, storage.count.value)
        assertEquals(ExplorerCategory.Location, model.selectedCategory)
    }

    @Test
    fun emptyOrInvalidSavedDataDoesNotReplaceTheCurrentScreen() = runTest(dispatcher) {
        val storage = FakeStorage()
        val model = RemoteStorageViewModel(api, storage)
        advanceUntilIdle()
        val previous = model.success()
        model.loadLastSavedItem()
        advanceUntilIdle()
        assertEquals(previous, model.success())
        assertEquals(R.string.no_saved_item, model.storageUiState.message)
        storage.last = SavedExplorerItem(1, "UnknownCategory", "Invalid")
        model.loadLastSavedItem()
        advanceUntilIdle()
        assertEquals(previous, model.success())
        assertEquals(R.string.load_failed, model.storageUiState.message)
        assertFalse(model.storageUiState.isLoadingLast)
    }

    @Test
    fun failedWritesAreReportedAndDoNotClaimSuccess() = runTest(dispatcher) {
        val storage = FakeStorage().apply { failWrite = true }
        val model = RemoteStorageViewModel(api, storage)
        advanceUntilIdle()
        model.saveCurrentItem()
        advanceUntilIdle()
        assertEquals(R.string.save_failed, model.storageUiState.message)
        assertFalse(model.storageUiState.isSaving)
        assertTrue(storage.saved.isEmpty())
        model.rollRandomItem()
        advanceUntilIdle()
        assertEquals(R.string.roll_sync_failed, model.storageUiState.message)
        assertEquals(0L, model.storageUiState.rollCount)
    }

    @Test
    fun duplicateSaveClicksAreIgnoredAndSnapshotSurvivesCategoryChanges() = runTest(dispatcher) {
        val storage = FakeStorage().apply { slowWrite = true }
        val model = RemoteStorageViewModel(api, storage)
        advanceUntilIdle()
        val item = model.success().selectedItem!!
        model.saveCurrentItem()
        model.saveCurrentItem()
        model.selectCategory(ExplorerCategory.Location)
        advanceUntilIdle()
        assertEquals(1, storage.saved.size)
        assertEquals("Character", storage.saved.single().category)
        assertEquals(item.name, storage.saved.single().name)
    }

    @Test
    fun databaseListenerFailureCanBeRetried() = runTest(dispatcher) {
        val storage = FakeStorage().apply { failRead = true }
        val model = RemoteStorageViewModel(api, storage)
        advanceUntilIdle()
        assertTrue(model.storageUiState.hasSyncError)
        storage.failRead = false
        model.retryStorageSync()
        advanceUntilIdle()
        assertFalse(model.storageUiState.hasSyncError)
        assertEquals(0L, model.storageUiState.rollCount)
    }

    @Test
    fun slowSaveStopsShowingBusyAndWarnsThatTheWriteMayStillComplete() = runTest(dispatcher) {
        val storage = FakeStorage().apply { writeDelay = 20_000 }
        val model = RemoteStorageViewModel(api, storage)
        advanceUntilIdle()
        model.saveCurrentItem()
        advanceUntilIdle()
        assertFalse(model.storageUiState.isSaving)
        assertEquals(R.string.storage_timeout, model.storageUiState.message)
    }

    private class FakeStorage : RemoteStorageRepository {
        val count = MutableStateFlow(0L)
        val history = MutableStateFlow<List<SavedExplorerItem>>(emptyList())
        val saved = mutableListOf<SavedExplorerItem>()
        var last: SavedExplorerItem? = null
        var failWrite = false
        var failRead = false
        var slowWrite = false
        var writeDelay = 0L

        override fun observeRollCount(): Flow<Long> =
            if (failRead) flow { throw IOException("permission denied") } else count
        override fun observeHistory(): Flow<List<SavedExplorerItem>> =
            if (failRead) flow { throw IOException("permission denied") } else history

        override suspend fun save(item: SavedExplorerItem) {
            delay(if (slowWrite) 1000 else writeDelay)
            if (failWrite) throw IOException("permission denied")
            saved += item
            last = item
            history.value = saved.asReversed().toList()
        }

        override suspend fun loadLast() = last

        override suspend fun incrementRollCount() {
            if (slowWrite) delay(1000)
            if (failWrite) throw IOException("permission denied")
            count.value++
        }
    }

    private fun RemoteStorageViewModel.success() = explorerUiState as ExplorerUiState.Success

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "{}".toResponseBody()))

    private class FakeApi : RickAndMortyApiService {
        val characterNames = mutableListOf<String?>()
        val episodeNames = mutableListOf<String?>()
        val locationNames = mutableListOf<String?>()
        val relatedIds = mutableListOf<Int>()
        var characterFailure: Exception? = null
        var relatedFailure: Exception? = null
        var slowCharacters = false
        var slowRelatedImage = false

        private val rick = RickAndMortyCharacter(1, "Rick Sanchez", "https://example.com/1.jpeg", "Alive", "Human")
        private val morty = RickAndMortyCharacter(2, "Morty Smith", "https://example.com/2.jpeg", "Alive", "Human")

        override suspend fun getCharacters(name: String?, page: Int?): RickAndMortyResponse<RickAndMortyCharacter> {
            characterNames += name
            if (slowCharacters) withContext(NonCancellable) { delay(1000) }
            characterFailure?.let { throw it }
            return RickAndMortyResponse(RickAndMortyInfo(826, 42, "character?page=2", null), listOf(rick, morty))
        }

        override suspend fun getEpisodes(name: String?, page: Int?): RickAndMortyResponse<RickAndMortyEpisode> {
            episodeNames += name
            return response(listOf(RickAndMortyEpisode(1, "Pilot", "December 2, 2013", "S01E01",
                listOf("https://rickandmortyapi.com/api/character/1"))))
        }

        override suspend fun getLocations(name: String?, page: Int?): RickAndMortyResponse<RickAndMortyLocation> {
            locationNames += name
            return response(listOf(RickAndMortyLocation(1, "Earth", "Planet", "Dimension C-137", emptyList())))
        }

        override suspend fun getCharacter(id: Int): RickAndMortyCharacter {
            relatedIds += id
            if (slowRelatedImage) withContext(NonCancellable) { delay(1000) }
            relatedFailure?.let { throw it }
            return rick
        }

        private fun <T> response(items: List<T>) =
            RickAndMortyResponse(RickAndMortyInfo(items.size, 1, null, null), items)
    }
}
