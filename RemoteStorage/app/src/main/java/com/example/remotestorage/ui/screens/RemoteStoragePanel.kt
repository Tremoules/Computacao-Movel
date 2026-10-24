package com.example.remotestorage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.remotestorage.R
import java.text.DateFormat
import java.util.Date

@Composable
fun RemoteStoragePanel(
    state: RemoteStorageUiState,
    canSave: Boolean,
    onSave: () -> Unit,
    onLoadLast: () -> Unit,
    onRetrySync: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHistory by rememberSaveable { mutableStateOf(false) }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.remote_storage), style = MaterialTheme.typography.titleMedium)
            if (!state.isConfigured) {
                Text(stringResource(R.string.firebase_setup_required))
            } else {
                Text(state.rollCount?.let { stringResource(R.string.total_rolls, it) }
                    ?: stringResource(R.string.roll_count_loading))
                if (state.hasSyncError) {
                    Text(stringResource(R.string.storage_sync_failed), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetrySync) { Text(stringResource(R.string.retry_sync)) }
                }
            }
            val busy = state.isSaving || state.isLoadingLast
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, enabled = state.isConfigured && canSave && !busy,
                    modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (state.isSaving) R.string.saving else R.string.save_item))
                }
                OutlinedButton(onClick = onLoadLast, enabled = state.isConfigured && !busy,
                    modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (state.isLoadingLast) R.string.loading_saved else R.string.load_last))
                }
            }
            state.message?.let { message ->
                Text(stringResource(message), modifier = Modifier.fillMaxWidth())
                TextButton(onClick = onDismissMessage) { Text(stringResource(R.string.dismiss)) }
            }
            if (state.isConfigured) {
                TextButton(onClick = { showHistory = !showHistory }) {
                    Text(stringResource(if (showHistory) R.string.hide_history else R.string.show_history))
                }
                if (showHistory) {
                    Text(stringResource(R.string.history_recent), style = MaterialTheme.typography.bodySmall)
                    when {
                        state.isHistoryLoading -> Text(stringResource(R.string.history_loading))
                        state.hasSyncError -> Text(stringResource(R.string.history_may_be_stale))
                        state.history.isEmpty() -> Text(stringResource(R.string.history_empty))
                    }
                    state.history.forEach { item ->
                        val category = ExplorerCategory.entries.firstOrNull { it.name == item.category }
                        val categoryLabel = category?.let { stringResource(it.label) } ?: item.category
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                            Text(categoryLabel)
                            Text(stringResource(R.string.item_field,
                                category?.let { stringResource(it.firstFieldLabel) } ?: "", item.firstField))
                            Text(stringResource(R.string.item_field,
                                category?.let { stringResource(it.secondFieldLabel) } ?: "", item.secondField))
                            if (item.savedAt > 0) {
                                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                    .format(Date(item.savedAt)), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
