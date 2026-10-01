@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.poodicraft.serverscope.ui.result

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.poodicraft.serverscope.data.StatusError
import io.github.poodicraft.serverscope.data.model.Addon
import io.github.poodicraft.serverscope.data.model.Edition
import io.github.poodicraft.serverscope.data.model.Player
import io.github.poodicraft.serverscope.data.model.PlayerListVisibility
import io.github.poodicraft.serverscope.data.model.ServerStatus
import io.github.poodicraft.serverscope.ui.components.EditionTag
import io.github.poodicraft.serverscope.ui.components.InfoChip
import io.github.poodicraft.serverscope.ui.components.InfoNote
import io.github.poodicraft.serverscope.ui.components.InfoRow
import io.github.poodicraft.serverscope.ui.components.LoadingBlocks
import io.github.poodicraft.serverscope.ui.components.McCard
import io.github.poodicraft.serverscope.ui.components.PixelProgressBar
import io.github.poodicraft.serverscope.ui.components.PlayerHead
import io.github.poodicraft.serverscope.ui.components.ServerIcon
import io.github.poodicraft.serverscope.ui.components.StatusBadge
import io.github.poodicraft.serverscope.ui.components.rememberAppContainer
import io.github.poodicraft.serverscope.ui.theme.Mc
import io.github.poodicraft.serverscope.ui.theme.PixelLabel
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import kotlin.math.roundToInt

private const val AUTO_REFRESH_MILLIS = 30_000
private const val COLLAPSED_ADDONS = 8

private enum class Phase { Loading, Error, Content }

@Composable
fun ResultScreen(address: String, edition: Edition, onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: ResultViewModel = viewModel {
        ResultViewModel(address, edition, container.statusRepository, container.savedServers)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedPlayer by remember { mutableStateOf<Player?>(null) }
    val autoRefreshProgress = remember { Animatable(0f) }

    LaunchedEffect(viewModel) {
        viewModel.refreshErrors.collect { error ->
            snackbarHostState.showSnackbar("Couldn't refresh: ${error.title}. Showing the last result.")
        }
    }
    AutoRefreshEffect(
        enabled = state.autoRefresh,
        progress = autoRefreshProgress,
        onTick = { viewModel.refresh(userInitiated = false) },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.address,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${state.edition.label} Edition",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        AnimatedContent(
                            targetState = state.isFavorite,
                            transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                            label = "favorite",
                        ) { favorite ->
                            Icon(
                                imageVector = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
                                tint = if (favorite) Mc.Redstone else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.refresh() }, enabled = !state.isLoading && !state.isRefreshing) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
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
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val phase = when {
                state.status != null -> Phase.Content
                state.error != null && !state.isLoading -> Phase.Error
                else -> Phase.Loading
            }
            AnimatedContent(
                targetState = phase,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "phase",
            ) { current ->
                when (current) {
                    Phase.Loading -> LoadingState(state.address)
                    Phase.Error -> ErrorState(
                        error = state.error ?: StatusError.Unknown,
                        onRetry = { viewModel.refresh(userInitiated = false) },
                        onBack = onBack,
                    )
                    Phase.Content -> state.status?.let { status ->
                        ResultContent(
                            state = state,
                            status = status,
                            autoRefreshProgress = { autoRefreshProgress.value },
                            onAutoRefreshChange = viewModel::setAutoRefresh,
                            onPlayerClick = { selectedPlayer = it },
                        )
                    }
                }
            }
        }
    }

    selectedPlayer?.let { player ->
        PlayerDetailSheet(player = player, onDismiss = { selectedPlayer = null })
    }
}

/** Refreshes every 30 s while enabled and the screen is visible; [progress] drives the countdown bar. */
@Composable
private fun AutoRefreshEffect(
    enabled: Boolean,
    progress: Animatable<Float, AnimationVector1D>,
    onTick: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnTick by rememberUpdatedState(onTick)
    LaunchedEffect(enabled, lifecycleOwner) {
        progress.snapTo(0f)
        if (!enabled) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(AUTO_REFRESH_MILLIS, easing = LinearEasing))
                currentOnTick()
            }
        }
    }
}

@Composable
private fun ResultContent(
    state: ResultUiState,
    status: ServerStatus,
    autoRefreshProgress: () -> Float,
    onAutoRefreshChange: (Boolean) -> Unit,
    onPlayerClick: (Player) -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 340.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalItemSpacing = 14.dp,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header", span = StaggeredGridItemSpan.FullLine) { HeaderCard(state, status) }
        item(key = "auto-refresh", span = StaggeredGridItemSpan.FullLine) {
            AutoRefreshRow(state.autoRefresh, onAutoRefreshChange, autoRefreshProgress)
        }
        if (status.online) {
            item(key = "players") { PlayersCard(status, onPlayerClick) }
            item(key = "server") { ServerInfoCard(status) }
        } else {
            item(key = "offline", span = StaggeredGridItemSpan.FullLine) { OfflineCard(status) }
        }
        if (status.mods.isNotEmpty()) {
            item(key = "mods") { AddonsCard("Mods", status.mods, Mc.Amethyst) }
        }
        if (status.plugins.isNotEmpty()) {
            item(key = "plugins") { AddonsCard("Plugins", status.plugins, Mc.Lapis) }
        }
        item(key = "network") { NetworkCard(status) }
        item(key = "footer", span = StaggeredGridItemSpan.FullLine) { Footer(status) }
    }
}

@Composable
private fun HeaderCard(state: ResultUiState, status: ServerStatus) {
    McCard(accent = if (status.online) Mc.Grass else Mc.Redstone) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ServerIcon(status.iconDataUri, size = 72.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.address,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusBadge(status.online)
                    EditionTag(status.edition)
                    state.lookupMillis?.let { InfoChip("Lookup $it ms") }
                }
            }
        }
        status.motd?.let { motd ->
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = Mc.Obsidian,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = motd,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color(0xFFE6E6E6),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                )
            }
        }
        state.updatedAt?.let { updatedAt ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Updated ${formatTime(updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AutoRefreshRow(enabled: Boolean, onChange: (Boolean) -> Unit, progress: () -> Float) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Auto-refresh", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (enabled) "Checking again every 30 seconds" else "Off. Pull down to refresh manually",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = onChange)
            }
            if (enabled) {
                val color = MaterialTheme.colorScheme.primary
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                ) {
                    drawRect(color, size = Size(size.width * progress(), size.height))
                }
            }
        }
    }
}

@Composable
private fun PlayersCard(status: ServerStatus, onPlayerClick: (Player) -> Unit) {
    val info = status.players
    McCard(title = "Players", accent = Mc.Emerald) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = info?.online?.let(::formatCount) ?: "?",
                style = MaterialTheme.typography.headlineMedium,
                color = Mc.Emerald,
            )
            Text(
                text = " / ${info?.max?.let(::formatCount) ?: "?"}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            if (info?.max != null && info.max > 0) {
                Text(
                    text = "${(info.fillFraction * 100).roundToInt()}% full",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        PixelProgressBar(fraction = info?.fillFraction ?: 0f)
        Spacer(Modifier.height(16.dp))

        val players = info?.list.orEmpty()
        when (status.playerListVisibility) {
            PlayerListVisibility.FULL -> PlayerGrid(players, onPlayerClick)
            PlayerListVisibility.PARTIAL -> {
                PlayerGrid(players, onPlayerClick)
                Spacer(Modifier.height(12.dp))
                InfoNote(
                    "Showing ${players.size} of ${info?.online?.let(::formatCount)}. " +
                        "The server only shares a sample of who's online.",
                )
            }
            PlayerListVisibility.HIDDEN -> InfoNote("Player list hidden by server", icon = Icons.Outlined.Lock)
            PlayerListVisibility.NOBODY_ONLINE -> InfoNote("Nobody is online right now.")
            PlayerListVisibility.NOT_SUPPORTED -> InfoNote("Bedrock servers only share a player count, not names.")
            PlayerListVisibility.NOT_REPORTED -> InfoNote("This server didn't report any player information.")
        }

        val messages = info?.messages.orEmpty()
        if (messages.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("SERVER MESSAGE", style = PixelLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(
                text = messages.joinToString("\n"),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlayerGrid(players: List<Player>, onPlayerClick: (Player) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        players.forEach { player ->
            Surface(
                onClick = { onPlayerClick(player) },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerHead(player, size = 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(player.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ServerInfoCard(status: ServerStatus) {
    McCard(title = "Server", accent = Mc.Gold) {
        InfoRow("Version", status.version ?: "Not reported")
        status.protocol?.let { InfoRow("Protocol", it.toString(), monospace = true) }
        status.software?.let { InfoRow("Software", it) }
        status.gamemode?.let { InfoRow("Game mode", it) }
        status.bedrockEdition?.let {
            InfoRow(
                "Edition",
                when (it) {
                    "MCPE" -> "Bedrock (MCPE)"
                    "MCEE" -> "Education Edition (MCEE)"
                    else -> it
                },
            )
        }
    }
}

@Composable
private fun OfflineCard(status: ServerStatus) {
    McCard(title = "Server offline", accent = Mc.Redstone) {
        Text(
            "The server didn't answer the status request. It may be offline or restarting, it may block " +
                "status pings, or the address or port may be wrong.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        if (status.ipAddress == null) {
            InfoNote(
                "The address didn't resolve to an IP. Check the spelling.",
                icon = Icons.Outlined.Warning,
                tint = Mc.Gold,
            )
            Spacer(Modifier.height(8.dp))
        }
        InfoNote(
            "Java servers usually use port 25565 and Bedrock servers 19132. Make sure the right edition is selected.",
        )
    }
}

@Composable
private fun AddonsCard(title: String, addons: List<Addon>, accent: Color) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    val visible = if (expanded) addons else addons.take(COLLAPSED_ADDONS)
    McCard(
        title = "$title (${addons.size})",
        accent = accent,
        modifier = Modifier.animateContentSize(),
    ) {
        visible.forEachIndexed { index, addon ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .background(accent),
                )
                Spacer(Modifier.width(10.dp))
                Text(addon.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                addon.version?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (index < visible.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
        if (addons.size > COLLAPSED_ADDONS) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Show less" else "Show all ${addons.size}")
            }
        }
    }
}

@Composable
private fun NetworkCard(status: ServerStatus) {
    McCard(title = "Network", accent = Mc.Diamond) {
        SelectionContainer {
            Column {
                InfoRow("Hostname", status.host ?: "Not reported", monospace = true)
                InfoRow(
                    "Resolved IP",
                    status.ipAddress ?: "Didn't resolve",
                    monospace = status.ipAddress != null,
                    valueColor = if (status.ipAddress != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                InfoRow("Port", status.port?.toString() ?: "Default (${status.edition.defaultPort})", monospace = true)
                status.srvRecord?.let { srv ->
                    InfoRow("SRV record", srv.host + (srv.port?.let { ":$it" } ?: ""), monospace = true)
                }
                InfoRow(
                    "EULA blocked",
                    when (status.eulaBlocked) {
                        true -> "Yes"
                        false -> "No"
                        null -> "Not reported"
                    },
                    valueColor = if (status.eulaBlocked == true) Mc.Redstone else MaterialTheme.colorScheme.onSurface,
                )
                InfoRow("Ping", "Not reported by the status API", valueColor = MaterialTheme.colorScheme.onSurfaceVariant)
                status.serverId?.let { InfoRow("Server ID", it, monospace = true) }
            }
        }
        if (status.eulaBlocked == true) {
            Spacer(Modifier.height(8.dp))
            InfoNote(
                "Mojang has blocked this server for breaking the Minecraft EULA, so official clients may refuse to join.",
                icon = Icons.Outlined.Warning,
                tint = Mc.Redstone,
            )
        }
    }
}

@Composable
private fun Footer(status: ServerStatus) {
    val cachedUntil = status.expiresAt?.let { " (until ${formatTime(it)})" }.orEmpty()
    Text(
        text = "Status from api.mcstatus.io. Results are cached for up to a minute$cachedUntil.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    )
}

@Composable
private fun LoadingState(address: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(120.dp))
        LoadingBlocks()
        Spacer(Modifier.height(28.dp))
        Text("Pinging server...", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorState(error: StatusError, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(96.dp))
        Icon(Icons.Outlined.Warning, contentDescription = null, tint = Mc.Gold, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(20.dp))
        Text(error.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            error.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, shape = MaterialTheme.shapes.small) { Text("Try again") }
        if (error == StatusError.InvalidAddress) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onBack, shape = MaterialTheme.shapes.small) { Text("Edit address") }
        }
    }
}

private fun formatCount(value: Int): String = NumberFormat.getIntegerInstance().format(value)

private fun formatTime(epochMillis: Long): String =
    DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(epochMillis))
