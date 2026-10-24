package com.example.remotestorage.network

import kotlinx.serialization.Serializable

@Serializable
data class RickAndMortyCharacter(
    val id: Int,
    val name: String,
    val image: String,
    val status: String,
    val species: String
)
