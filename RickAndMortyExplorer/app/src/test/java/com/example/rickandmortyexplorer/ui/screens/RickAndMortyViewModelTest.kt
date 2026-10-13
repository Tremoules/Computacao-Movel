package com.example.rickandmortyexplorer.ui.screens

import com.example.rickandmortyexplorer.network.RickAndMortyApiService
import com.example.rickandmortyexplorer.network.RickAndMortyCharacter
import com.example.rickandmortyexplorer.network.RickAndMortyEpisode
import com.example.rickandmortyexplorer.network.RickAndMortyInfo
import com.example.rickandmortyexplorer.network.RickAndMortyLocation
import com.example.rickandmortyexplorer.network.RickAndMortyResponse
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
class RickAndMortyViewModelTest {
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
        val model = RickAndMortyViewModel(api)
        assertEquals(RickAndMortyUiState.Loading, model.rickAndMortyUiState)
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
        val model = RickAndMortyViewModel(api)
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
        val model = RickAndMortyViewModel(api)
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
        val model = RickAndMortyViewModel(api)
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
        val model = RickAndMortyViewModel(api)
        model.selectCategory(ExplorerCategory.Episode)
        advanceUntilIdle()
        model.updateNameFilter("pilot")
        advanceUntilIdle()
        assertEquals("pilot", api.episodeNames.last())
    }

    @Test
    fun noMatch404IsEmptySuccessButServerErrorsOfferRetry() = runTest(dispatcher) {
        val model = RickAndMortyViewModel(api)
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
        assertEquals(RickAndMortyUiState.Error, model.rickAndMortyUiState)
        api.characterFailure = null
        model.retry()
        advanceUntilIdle()
        assertEquals(2, model.success().items.size)
    }

    @Test
    fun networkFailureCanBeRetried() = runTest(dispatcher) {
        api.characterFailure = IOException("offline")
        val model = RickAndMortyViewModel(api)
        advanceUntilIdle()
        assertEquals(RickAndMortyUiState.Error, model.rickAndMortyUiState)
        api.characterFailure = null
        model.retry()
        advanceUntilIdle()
        assertEquals(2, model.success().items.size)
    }

    @Test
    fun rollAlwaysChangesItemAndDoesNotRefetchCharacterList() = runTest(dispatcher) {
        val model = RickAndMortyViewModel(api)
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
        val model = RickAndMortyViewModel(api)
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
        val model = RickAndMortyViewModel(api)
        model.selectCategory(ExplorerCategory.Episode)
        advanceUntilIdle()
        assertEquals("Pilot", model.success().selectedItem!!.name)
        assertNull(model.success().selectedItem!!.image)
    }

    @Test
    fun olderRequestCannotReplaceNewCategoryEvenIfItFinishesAfterCancellation() = runTest(dispatcher) {
        api.slowCharacters = true
        val model = RickAndMortyViewModel(api)
        runCurrent()
        model.selectCategory(ExplorerCategory.Location)
        advanceUntilIdle()
        assertEquals(ExplorerCategory.Location, model.selectedCategory)
        assertEquals("Earth", model.success().selectedItem!!.name)
    }

    @Test
    fun switchingCategoryDuringRelatedImageLookupCannotRestoreOldResult() = runTest(dispatcher) {
        api.slowRelatedImage = true
        val model = RickAndMortyViewModel(api)
        model.selectCategory(ExplorerCategory.Episode)
        runCurrent()
        model.selectCategory(ExplorerCategory.Location)
        advanceUntilIdle()
        assertEquals("Earth", model.success().selectedItem!!.name)
    }

    private fun RickAndMortyViewModel.success() = rickAndMortyUiState as RickAndMortyUiState.Success

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
