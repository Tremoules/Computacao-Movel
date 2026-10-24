package com.example.remotestorage.data

/** A snapshot, independent of the API: loading it never needs another API request. */
data class SavedExplorerItem(
    val id: Int = 0,
    val category: String = "",
    val name: String = "",
    val firstField: String = "",
    val secondField: String = "",
    val imageUrl: String? = null,
    val relatedCharacterName: String? = null,
    val savedAt: Long = 0
)
