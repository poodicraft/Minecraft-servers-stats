@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.poodicraft.serverscope.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.poodicraft.serverscope.BuildConfig
import io.github.poodicraft.serverscope.data.AddressResult
import io.github.poodicraft.serverscope.data.ServerAddress
import io.github.poodicraft.serverscope.data.local.HistoryEntry
import io.github.poodicraft.serverscope.data.local.SavedServer
import io.github.poodicraft.serverscope.data.model.Edition
import io.github.poodicraft.serverscope.ui.components.EditionTag
import io.github.poodicraft.serverscope.ui.components.GrassBanner
import io.github.poodicraft.serverscope.ui.components.PixelDot
import io.github.poodicraft.serverscope.ui.components.SectionHeader
import io.github.poodicraft.serverscope.ui.components.rememberAppContainer
import io.github.poodicraft.serverscope.ui.theme.Mc
import io.github.poodicraft.serverscope.ui.theme.PixelLabel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onOpenServer: (address: String, edition: Edition) -> Unit) {
    val container = rememberAppContainer()
    val viewModel: HomeViewModel = viewModel { HomeViewModel(container.statusRepository, container.savedServers) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var input by rememberSaveable { mutableStateOf("") }
    var edition by rememberSaveable { mutableStateOf(Edition.JAVA) }
    var inputError by rememberSaveable { mutableStateOf<String?>(null) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenResumed()
        onPauseOrDispose { }
    }

    fun submit() {
        when (val result = ServerAddress.parse(input)) {
            is AddressResult.Valid -> {
                inputError = null
                focusManager.clearFocus()
                onOpenServer(result.address.query, edition)
            }
            is AddressResult.Invalid -> inputError = result.message
        }
    }

    val onRemove: (FavoriteItem, Int) -> Unit = { item, index ->
        viewModel.removeFavorite(item.server)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Removed ${item.server.address}",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreFavorite(item.server, index)
        }
    }
    val openSaved: (SavedServer) -> Unit = { onOpenServer(it.address, it.edition) }
    val openHistory: (HistoryEntry) -> Unit = { onOpenServer(it.address, it.edition) }

    val searchCard: @Composable () -> Unit = {
        SearchCard(
            input = input,
            onInputChange = {
                input = it
                inputError = null
            },
            edition = edition,
            onEditionChange = { edition = it },
            error = inputError,
            onSubmit = ::submit,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ServerScope", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { showAbout = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = "About ServerScope")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refreshFavorites() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().imePadding()) {
                if (maxWidth >= 720.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            searchCard()
                            if (state.history.isNotEmpty()) {
                                RecentSearches(state.history, openHistory, viewModel::clearHistory)
                            }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            favoritesSection(state.favorites, openSaved, onRemove)
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(max = 640.dp),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item(key = "search") { searchCard() }
                            favoritesSection(state.favorites, openSaved, onRemove)
                            if (state.history.isNotEmpty()) {
                                item(key = "history") {
                                    RecentSearches(state.history, openHistory, viewModel::clearHistory)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
}

@Composable
private fun SearchCard(
    input: String,
    onInputChange: (String) -> Unit,
    edition: Edition,
    onEditionChange: (Edition) -> Unit,
    error: String?,
    onSubmit: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            GrassBanner()
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Check any server", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "See if it's online, who's playing, its version, MOTD, plugins and more.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Server address") },
                    placeholder = { Text("play.example.net") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = if (input.isNotEmpty()) {
                        {
                            IconButton(onClick = { onInputChange("") }) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Clear address")
                            }
                        }
                    } else {
                        null
                    },
                    supportingText = { Text(error ?: "IP or domain. Port is optional (default ${edition.defaultPort}).") },
                    isError = error != null,
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Edition.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = option == edition,
                            onClick = { onEditionChange(option) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = Edition.entries.size),
                            label = { Text("${option.label} Edition") },
                        )
                    }
                }
                Button(
                    onClick = onSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text("CHECK", style = PixelLabel.copy(fontSize = 13.sp))
                }
            }
        }
    }
}

private fun LazyListScope.favoritesSection(
    favorites: List<FavoriteItem>,
    onOpen: (SavedServer) -> Unit,
    onRemove: (FavoriteItem, Int) -> Unit,
) {
    item(key = "favorites-header") { SectionHeader("Favorites") }
    if (favorites.isEmpty()) {
        item(key = "favorites-empty") {
            Text(
                "No favorites yet. Check a server and tap the heart to pin it here with its live status.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    } else {
        itemsIndexed(favorites, key = { _, item -> item.server.key }) { index, item ->
            FavoriteRow(
                item = item,
                onClick = { onOpen(item.server) },
                onRemove = { onRemove(item, index) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun FavoriteRow(item: FavoriteItem, onClick: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val status = item.status
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                if (status.checking && status.online == null) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    val color = when (status.online) {
                        true -> Mc.Emerald
                        false -> Mc.Redstone
                        null -> Mc.Stone
                    }
                    PixelDot(color = color, pulsing = status.online == true || status.checking, size = 12.dp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.server.address,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EditionTag(item.server.edition)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = favoriteStatusText(status),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Remove ${item.server.address} from favorites",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun favoriteStatusText(status: FavoriteStatus): String {
    val base = when (status.online) {
        true -> "${status.players ?: "?"} / ${status.maxPlayers ?: "?"} online"
        false -> "Offline"
        null -> when {
            status.checking -> "Checking..."
            status.error != null -> "Couldn't check: ${status.error.title.lowercase()}"
            else -> "Not checked yet"
        }
    }
    return if (status.online != null && status.error != null) "$base (last known)" else base
}

@Composable
private fun RecentSearches(history: List<HistoryEntry>, onOpen: (HistoryEntry) -> Unit, onClear: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Recent", action = { TextButton(onClick = onClear) { Text("Clear") } })
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            history.forEach { entry ->
                AssistChip(
                    onClick = { onOpen(entry) },
                    label = { Text(entry.address, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        PixelDot(color = if (entry.edition == Edition.JAVA) Mc.Gold else Mc.Diamond, size = 8.dp)
                    },
                )
            }
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        title = { Text("ServerScope", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelLarge)
                Text("Server status comes from the free mcstatus.io API. Results are cached there for about a minute.")
                Text("Player heads and skins come from mc-heads.net, with minotar.net as a fallback.")
                Text(
                    "Per-player stats such as playtime, kills or inventories aren't part of the public " +
                        "server status protocol, so ServerScope doesn't show them.",
                )
                Text("Pixel font: Press Start 2P (SIL Open Font License 1.1).")
                Text(
                    "Not affiliated with Mojang or Microsoft.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
