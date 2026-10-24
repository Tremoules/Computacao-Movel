package com.example.remotestorage.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

object RemoteStorageProvider {
    fun create(context: Context): RemoteStorageRepository? {
        // The normal tutorial setup supplies these options through google-services.json.
        val app = FirebaseApp.initializeApp(context) ?: return null
        val url = app.options.databaseUrl?.takeIf { it.isNotBlank() } ?: return null
        return FirebaseRemoteStorageRepository(
            FirebaseDatabase.getInstance(app, url).getReference("remoteStorage")
        )
    }
}
