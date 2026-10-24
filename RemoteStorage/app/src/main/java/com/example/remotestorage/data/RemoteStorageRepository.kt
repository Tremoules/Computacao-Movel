package com.example.remotestorage.data

import kotlinx.coroutines.flow.Flow

/** The ViewModel depends on this contract rather than Firebase callbacks. */
interface RemoteStorageRepository {
    fun observeRollCount(): Flow<Long>
    fun observeHistory(): Flow<List<SavedExplorerItem>>
    suspend fun save(item: SavedExplorerItem)
    suspend fun loadLast(): SavedExplorerItem?
    suspend fun incrementRollCount()
}
