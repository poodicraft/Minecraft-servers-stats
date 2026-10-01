package io.github.poodicraft.serverscope.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.poodicraft.serverscope.data.AddressResult
import io.github.poodicraft.serverscope.data.ServerAddress
import io.github.poodicraft.serverscope.data.StatusError
import io.github.poodicraft.serverscope.data.StatusRepository
import io.github.poodicraft.serverscope.data.StatusResult
import io.github.poodicraft.serverscope.data.local.HistoryEntry
import io.github.poodicraft.serverscope.data.local.SavedServer
import io.github.poodicraft.serverscope.data.local.SavedServersStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Last known status of a favorite, plus whether a check is running right now. */
data class FavoriteStatus(
    val checking: Boolean,
    val online: Boolean?,
    val players: Int?,
    val maxPlayers: Int?,
    val error: StatusError?,
)

data class FavoriteItem(val server: SavedServer, val status: FavoriteStatus)

data class HomeUiState(
    val favorites: List<FavoriteItem> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val isRefreshing: Boolean = false,
)

class HomeViewModel(
    private val repository: StatusRepository,
    private val store: SavedServersStore,
) : ViewModel() {

    private sealed interface Check {
        data object Running : Check
        data class Failed(val error: StatusError) : Check
    }

    private val checks = MutableStateFlow<Map<String, Check>>(emptyMap())
    private val refreshing = MutableStateFlow(false)
    private var refreshJob: Job? = null
    private var lastRefreshAt = 0L

    val state: StateFlow<HomeUiState> =
        combine(store.favorites, store.history, checks, refreshing) { favorites, history, checkMap, isRefreshing ->
            HomeUiState(
                favorites = favorites.map { server ->
                    val check = checkMap[server.key]
                    FavoriteItem(
                        server = server,
                        status = FavoriteStatus(
                            checking = check == Check.Running,
                            online = server.lastOnline,
                            players = server.lastPlayers,
                            maxPlayers = server.lastMaxPlayers,
                            error = (check as? Check.Failed)?.error,
                        ),
                    )
                },
                history = history,
                isRefreshing = isRefreshing,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Re-checks favorites when the screen comes back and the data is older than a minute. */
    fun onScreenResumed() {
        if (System.currentTimeMillis() - lastRefreshAt > STALE_AFTER_MILLIS) refreshFavorites(userInitiated = false)
    }

    fun refreshFavorites(userInitiated: Boolean = true) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            val favorites = store.favorites.first()
            if (favorites.isEmpty()) {
                lastRefreshAt = System.currentTimeMillis()
                return@launch
            }
            refreshing.value = userInitiated
            checks.update { current -> current + favorites.associate { it.key to Check.Running } }
            try {
                val permits = Semaphore(MAX_PARALLEL_CHECKS)
                favorites.map { server ->
                    launch { permits.withPermit { check(server) } }
                }.forEach { it.join() }
                lastRefreshAt = System.currentTimeMillis()
            } finally {
                refreshing.value = false
            }
        }
    }

    private suspend fun check(server: SavedServer) {
        val address = (ServerAddress.parse(server.address) as? AddressResult.Valid)?.address
        val result = if (address == null) {
            StatusResult.Failure(StatusError.InvalidAddress)
        } else {
            repository.fetch(address, server.edition)
        }
        when (result) {
            is StatusResult.Success -> {
                val status = result.status
                store.updateSnapshot(
                    key = server.key,
                    online = status.online,
                    players = status.players?.online,
                    maxPlayers = status.players?.max,
                    checkedAt = System.currentTimeMillis(),
                )
                checks.update { it - server.key }
            }
            is StatusResult.Failure -> checks.update { it + (server.key to Check.Failed(result.error)) }
        }
    }

    fun removeFavorite(server: SavedServer) {
        viewModelScope.launch { store.removeFavorite(server.key) }
    }

    fun restoreFavorite(server: SavedServer, index: Int) {
        viewModelScope.launch { store.restoreFavorite(server, index) }
    }

    fun clearHistory() {
        viewModelScope.launch { store.clearHistory() }
    }

    private companion object {
        const val STALE_AFTER_MILLIS = 60_000L
        const val MAX_PARALLEL_CHECKS = 4
    }
}
