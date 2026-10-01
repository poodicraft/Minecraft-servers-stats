package io.github.poodicraft.serverscope.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.poodicraft.serverscope.data.AddressResult
import io.github.poodicraft.serverscope.data.ServerAddress
import io.github.poodicraft.serverscope.data.StatusError
import io.github.poodicraft.serverscope.data.StatusRepository
import io.github.poodicraft.serverscope.data.StatusResult
import io.github.poodicraft.serverscope.data.local.SavedServer
import io.github.poodicraft.serverscope.data.local.SavedServersStore
import io.github.poodicraft.serverscope.data.local.serverKey
import io.github.poodicraft.serverscope.data.model.Edition
import io.github.poodicraft.serverscope.data.model.ServerStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultUiState(
    val address: String,
    val edition: Edition,
    val status: ServerStatus? = null,
    /** How long the status API took to answer the last lookup. */
    val lookupMillis: Long? = null,
    val updatedAt: Long? = null,
    /** First load, nothing to show yet. */
    val isLoading: Boolean = true,
    /** User pulled to refresh while data is on screen. */
    val isRefreshing: Boolean = false,
    /** Error of the most recent lookup, if it failed. */
    val error: StatusError? = null,
    val isFavorite: Boolean = false,
    val autoRefresh: Boolean = false,
)

class ResultViewModel(
    addressText: String,
    private val edition: Edition,
    private val repository: StatusRepository,
    private val store: SavedServersStore,
) : ViewModel() {

    private val address: ServerAddress? = (ServerAddress.parse(addressText) as? AddressResult.Valid)?.address
    private val displayAddress = address?.query ?: addressText
    private val key = serverKey(displayAddress, edition)

    private val _state = MutableStateFlow(ResultUiState(address = displayAddress, edition = edition))
    val state: StateFlow<ResultUiState> = _state.asStateFlow()

    private val _refreshErrors = Channel<StatusError>(Channel.BUFFERED)

    /** Failures of refreshes while data is already shown (the old data stays on screen). */
    val refreshErrors: Flow<StatusError> = _refreshErrors.receiveAsFlow()

    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            store.favorites
                .map { list -> list.any { it.key == key } }
                .distinctUntilChanged()
                .collect { favorite -> _state.update { it.copy(isFavorite = favorite) } }
        }
        if (address != null) {
            viewModelScope.launch { store.recordSearch(address.query, edition, System.currentTimeMillis()) }
        }
        refresh(userInitiated = false)
    }

    fun refresh(userInitiated: Boolean = true) {
        if (fetchJob?.isActive == true) return
        val target = address
        if (target == null) {
            _state.update { it.copy(isLoading = false, isRefreshing = false, error = StatusError.InvalidAddress) }
            return
        }
        fetchJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.status == null, isRefreshing = userInitiated && it.status != null) }
            when (val result = repository.fetch(target, edition)) {
                is StatusResult.Success -> {
                    val now = System.currentTimeMillis()
                    _state.update {
                        it.copy(
                            status = result.status,
                            lookupMillis = result.lookupMillis,
                            updatedAt = now,
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                        )
                    }
                    if (_state.value.isFavorite) {
                        store.updateSnapshot(
                            key = key,
                            online = result.status.online,
                            players = result.status.players?.online,
                            maxPlayers = result.status.players?.max,
                            checkedAt = now,
                        )
                    }
                }
                is StatusResult.Failure -> {
                    val hadData = _state.value.status != null
                    _state.update { it.copy(isLoading = false, isRefreshing = false, error = result.error) }
                    if (hadData) _refreshErrors.send(result.error)
                }
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val current = _state.value
            if (current.isFavorite) {
                store.removeFavorite(key)
            } else {
                val status = current.status
                store.addFavorite(
                    SavedServer(
                        address = displayAddress,
                        edition = edition,
                        addedAt = System.currentTimeMillis(),
                        lastOnline = status?.online,
                        lastPlayers = status?.players?.online,
                        lastMaxPlayers = status?.players?.max,
                        lastCheckedAt = current.updatedAt,
                    ),
                )
            }
        }
    }

    fun setAutoRefresh(enabled: Boolean) {
        _state.update { it.copy(autoRefresh = enabled) }
    }
}
