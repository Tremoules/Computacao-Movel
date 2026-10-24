package com.example.remotestorage.ui.screens

import androidx.annotation.StringRes
import com.example.remotestorage.data.SavedExplorerItem

data class RemoteStorageUiState(
    val isConfigured: Boolean = false,
    val rollCount: Long? = null,
    val history: List<SavedExplorerItem> = emptyList(),
    val isHistoryLoading: Boolean = true,
    val hasSyncError: Boolean = false,
    val isSaving: Boolean = false,
    val isLoadingLast: Boolean = false,
    @get:StringRes val message: Int? = null
)
