package com.example.rickandmortyexplorer.network

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RickAndMortySerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun characterResponseDecodesPaginationAndIgnoresUnusedFields() {
        val response = json.decodeFromString<RickAndMortyResponse<RickAndMortyCharacter>>(
            """{
                "info": {"count": 826, "pages": 42,
                    "next": "https://rickandmortyapi.com/api/character?page=2", "prev": null},
                "results": [{"id": 1, "name": "Rick Sanchez", "status": "Alive",
                    "species": "Human", "image": "https://example.com/rick.jpeg",
                    "origin": {"name": "Earth", "url": ""}, "episode": ["episode/1"]}]
            }"""
        )
        assertEquals(826, response.info.count)
        assertEquals(42, response.info.pages)
        assertNull(response.info.prev)
        assertEquals("https://rickandmortyapi.com/api/character?page=2", response.info.next)
        assertEquals("Rick Sanchez", response.results.single().name)
        assertEquals("Alive", response.results.single().status)
        assertEquals("Human", response.results.single().species)
        assertEquals("https://example.com/rick.jpeg", response.results.single().image)
    }

    @Test
    fun episodeResponseMapsAirDateAndRelatedCharacters() {
        val response = json.decodeFromString<RickAndMortyResponse<RickAndMortyEpisode>>(
            """{
                "info": {"count": 1, "pages": 1, "next": null, "prev": null},
                "results": [{"id": 1, "name": "Pilot", "air_date": "December 2, 2013",
                    "episode": "S01E01", "characters": ["https://rickandmortyapi.com/api/character/1"],
                    "created": "2017-11-10T12:56:33.798Z"}]
            }"""
        )
        assertEquals("December 2, 2013", response.results.single().airDate)
        assertEquals("S01E01", response.results.single().episode)
        assertEquals("https://rickandmortyapi.com/api/character/1", response.results.single().characters.single())
        assertNull(response.info.next)
    }

    @Test
    fun locationResponseAllowsNoRelatedResidents() {
        val response = json.decodeFromString<RickAndMortyResponse<RickAndMortyLocation>>(
            """{
                "info": {"count": 1, "pages": 1, "next": null, "prev": null},
                "results": [{"id": 1, "name": "Earth", "type": "Planet",
                    "dimension": "Dimension C-137", "residents": [], "url": "location/1"}]
            }"""
        )
        assertEquals("Planet", response.results.single().type)
        assertEquals("Dimension C-137", response.results.single().dimension)
        assertTrue(response.results.single().residents.isEmpty())
    }

    @Test
    fun emptyResultsDecodeWithoutInventingItems() {
        val response = json.decodeFromString<RickAndMortyResponse<RickAndMortyCharacter>>(
            """{"info": {"count": 0, "pages": 0, "next": null, "prev": null}, "results": []}"""
        )
        assertTrue(response.results.isEmpty())
        assertNull(response.results.randomOrNull())
    }
}
