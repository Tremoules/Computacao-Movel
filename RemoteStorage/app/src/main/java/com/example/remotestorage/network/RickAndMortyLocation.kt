package com.example.remotestorage.network

import kotlinx.serialization.Serializable

@Serializable
data class RickAndMortyLocation(
    val id: Int,
    val name: String,
    val type: String,
    val dimension: String,
    val residents: List<String>
)
