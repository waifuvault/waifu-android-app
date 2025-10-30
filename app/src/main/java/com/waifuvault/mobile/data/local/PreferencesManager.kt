package com.waifuvault.mobile.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "waifuvault_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        private val BUCKET_TOKEN_KEY = stringPreferencesKey("bucket_token")
        private val CHUNK_SIZE_KEY = stringPreferencesKey("chunk_size")
    }

    val bucketToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[BUCKET_TOKEN_KEY]
    }

    val chunkSize: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CHUNK_SIZE_KEY]?.toIntOrNull() ?: (5 * 1024 * 1024) // Default 5MB
    }

    suspend fun saveBucketToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[BUCKET_TOKEN_KEY] = token
        }
    }

    suspend fun saveChunkSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[CHUNK_SIZE_KEY] = size.toString()
        }
    }

    suspend fun clearBucketToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(BUCKET_TOKEN_KEY)
        }
    }
}
