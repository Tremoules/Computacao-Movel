package com.example.remotestorage.ui.screens

sealed interface ExplorerUiState {
    data class Success(
        val totalItems: Int,
        val items: List<ExplorerItem>,
        val selectedItem: ExplorerItem?,
        val isRolling: Boolean = false,
        val isSavedSnapshot: Boolean = false
    ) : ExplorerUiState

    object Error : ExplorerUiState
    object Loading : ExplorerUiState
}
