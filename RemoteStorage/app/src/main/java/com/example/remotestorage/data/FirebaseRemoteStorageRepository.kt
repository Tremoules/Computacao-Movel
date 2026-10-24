package com.example.remotestorage.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseRemoteStorageRepository(
    private val root: DatabaseReference
) : RemoteStorageRepository {
    override fun observeRollCount(): Flow<Long> =
        root.child("rollCount").snapshots().map { it.getValue(Long::class.java) ?: 0L }

    // Keep every save in Firebase; only download the most recent 50 for the screen.
    override fun observeHistory(): Flow<List<SavedExplorerItem>> =
        root.child("history").orderByChild("savedAt").limitToLast(50).snapshots().map { snapshot ->
            snapshot.children.mapNotNull { it.getValue(SavedExplorerItem::class.java) }
                .reversed()
        }

    override suspend fun save(item: SavedExplorerItem) {
        val key = checkNotNull(root.child("history").push().key)
        val values = mapOf(
            "id" to item.id,
            "category" to item.category,
            "name" to item.name,
            "firstField" to item.firstField,
            "secondField" to item.secondField,
            "imageUrl" to item.imageUrl,
            "relatedCharacterName" to item.relatedCharacterName,
            "savedAt" to ServerValue.TIMESTAMP
        )
        // One atomic write: lastItem and its history entry either both succeed or both fail.
        root.updateChildren(mapOf("lastItem" to values, "history/$key" to values)).await()
    }

    override suspend fun loadLast(): SavedExplorerItem? =
        root.child("lastItem").get().await().getValue(SavedExplorerItem::class.java)

    override suspend fun incrementRollCount() {
        // Increment on the server so two devices cannot overwrite each other's rolls.
        root.child("rollCount").setValue(ServerValue.increment(1)).await()
    }

    private fun Query.snapshots(): Flow<DataSnapshot> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        addValueEventListener(listener)
        // Cancelling the ViewModel collection also detaches the Firebase listener.
        awaitClose { removeEventListener(listener) }
    }
}
