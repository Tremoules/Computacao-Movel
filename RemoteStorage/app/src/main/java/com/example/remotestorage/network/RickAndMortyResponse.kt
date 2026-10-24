package com.example.remotestorage.network

import kotlinx.serialization.Serializable

@Serializable
data class RickAndMortyResponse<T>(
    val info: RickAndMortyInfo,
    val results: List<T>
)

@Serializable
data class RickAndMortyInfo(
    val count: Int,
    val pages: Int,
    val next: String?,
    val prev: String?
)
