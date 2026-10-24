package com.example.remotestorage.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RickAndMortyEpisode(
    val id: Int,
    val name: String,
    @SerialName("air_date")
    val airDate: String,
    val episode: String,
    val characters: List<String>
)
