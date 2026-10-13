package com.example.rickandmortyexplorer.ui.screens

import androidx.annotation.StringRes
import com.example.rickandmortyexplorer.R

enum class ExplorerCategory(
    @get:StringRes val label: Int,
    @get:StringRes val firstFieldLabel: Int,
    @get:StringRes val secondFieldLabel: Int
) {
    Character(R.string.category_character, R.string.status, R.string.species),
    Episode(R.string.category_episode, R.string.episode_code, R.string.air_date),
    Location(R.string.category_location, R.string.location_type, R.string.dimension)
}

// The network models retain their own JSON schemas; this holds the fields shown by the screen.
data class ExplorerItem(
    val id: Int,
    val name: String,
    val firstField: String,
    val secondField: String,
    val image: String? = null,
    val relatedCharacterUrl: String? = null,
    val relatedCharacterName: String? = null
)
