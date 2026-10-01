package io.github.poodicraft.serverscope.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.poodicraft.serverscope.data.StatusParser
import io.github.poodicraft.serverscope.data.model.Edition
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

val Context.serverScopeDataStore: DataStore<Preferences> by preferencesDataStore(name = "serverscope")

fun serverKey(address: String, edition: Edition) = "${edition.name}:$address"

/** A pinned server plus the last status we saw for it, so the list renders instantly. */
@Serializable
data class SavedServer(
    val address: String,
    val edition: Edition,
    val addedAt: Long,
    val lastOnline: Boolean? = null,
    val lastPlayers: Int? = null,
    val lastMaxPlayers: Int? = null,
    val lastCheckedAt: Long? = null,
) {
    val key: String get() = serverKey(address, edition)
}

@Serializable
data class HistoryEntry(val address: String, val edition: Edition, val checkedAt: Long) {
    val key: String get() = serverKey(address, edition)
}

/** Favorites and search history, stored as JSON in Preferences DataStore. */
class SavedServersStore(private val dataStore: DataStore<Preferences>) {

    private val favoritesSerializer = ListSerializer(SavedServer.serializer())
    private val historySerializer = ListSerializer(HistoryEntry.serializer())

    private val preferences: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val favorites: Flow<List<SavedServer>> = preferences
        .map { decode(it[FAVORITES], favoritesSerializer) }
        .distinctUntilChanged()

    val history: Flow<List<HistoryEntry>> = preferences
        .map { decode(it[HISTORY], historySerializer) }
        .distinctUntilChanged()

    suspend fun addFavorite(server: SavedServer) = editFavorites { list ->
        listOf(server) + list.filterNot { it.key == server.key }
    }

    suspend fun removeFavorite(key: String) = editFavorites { list -> list.filterNot { it.key == key } }

    /** Puts a removed favorite back at its old position (for "Undo"). */
    suspend fun restoreFavorite(server: SavedServer, index: Int) = editFavorites { list ->
        if (list.any { it.key == server.key }) {
            list
        } else {
            list.toMutableList().apply { add(index.coerceIn(0, size), server) }
        }
    }

    suspend fun updateSnapshot(key: String, online: Boolean, players: Int?, maxPlayers: Int?, checkedAt: Long) =
        editFavorites { list ->
            list.map {
                if (it.key == key) {
                    it.copy(lastOnline = online, lastPlayers = players, lastMaxPlayers = maxPlayers, lastCheckedAt = checkedAt)
                } else {
                    it
                }
            }
        }

    suspend fun recordSearch(address: String, edition: Edition, checkedAt: Long) {
        val entry = HistoryEntry(address, edition, checkedAt)
        dataStore.edit { prefs ->
            val list = decode(prefs[HISTORY], historySerializer)
            val updated = (listOf(entry) + list.filterNot { it.key == entry.key }).take(MAX_HISTORY)
            prefs[HISTORY] = StatusParser.json.encodeToString(historySerializer, updated)
        }
    }

    suspend fun clearHistory() {
        dataStore.edit { it.remove(HISTORY) }
    }

    private suspend fun editFavorites(transform: (List<SavedServer>) -> List<SavedServer>) {
        dataStore.edit { prefs ->
            val updated = transform(decode(prefs[FAVORITES], favoritesSerializer))
            prefs[FAVORITES] = StatusParser.json.encodeToString(favoritesSerializer, updated)
        }
    }

    private fun <T> decode(raw: String?, serializer: kotlinx.serialization.KSerializer<List<T>>): List<T> {
        if (raw.isNullOrEmpty()) return emptyList()
        return try {
            StatusParser.json.decodeFromString(serializer, raw)
        } catch (e: IllegalArgumentException) {
            emptyList()
        }
    }

    private companion object {
        val FAVORITES = stringPreferencesKey("favorites_json")
        val HISTORY = stringPreferencesKey("history_json")
        const val MAX_HISTORY = 12
    }
}
