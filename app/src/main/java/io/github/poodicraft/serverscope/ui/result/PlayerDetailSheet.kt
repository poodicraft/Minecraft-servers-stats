@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.poodicraft.serverscope.ui.result

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.poodicraft.serverscope.R
import io.github.poodicraft.serverscope.data.model.Player
import io.github.poodicraft.serverscope.ui.components.InfoNote
import io.github.poodicraft.serverscope.ui.components.LoadingBlocks
import io.github.poodicraft.serverscope.ui.components.PlayerHead
import io.github.poodicraft.serverscope.ui.theme.Mc
import io.github.poodicraft.serverscope.ui.theme.PixelLabel

/**
 * What we really know about a player from a status ping: name, UUID and their skin.
 * Stats like playtime or kills are not part of the protocol, so none are shown.
 */
@Composable
fun PlayerDetailSheet(player: Player, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SkinRender(player)
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerHead(player)
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            UuidRow(player.uuid)
            InfoNote("Detailed stats require the server to run a stats plugin.")
        }
    }
}

@Composable
private fun SkinRender(player: Player) {
    var useFallback by remember(player) { mutableStateOf(false) }
    var loading by remember(player) { mutableStateOf(true) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(Brush.verticalGradient(listOf(Mc.Lapis.copy(alpha = 0.28f), Color.Transparent))),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = if (useFallback) player.bodyFallbackUrl ?: player.bodyUrl else player.bodyUrl,
            contentDescription = "${player.name}'s skin",
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
            error = painterResource(R.drawable.ic_head_placeholder),
            onSuccess = { loading = false },
            onError = {
                if (!useFallback && player.bodyFallbackUrl != null) useFallback = true else loading = false
            },
        )
        if (loading) LoadingBlocks()
    }
}

@Composable
private fun UuidRow(uuid: String?) {
    val context = LocalContext.current
    var copied by remember(uuid) { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("UUID", style = PixelLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = uuid ?: "Not shared by the server",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    textAlign = TextAlign.Start,
                )
            }
            if (uuid != null) {
                TextButton(onClick = { copied = copyToClipboard(context, uuid) }) {
                    Text(if (copied) "Copied" else "Copy")
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String): Boolean {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
    clipboard.setPrimaryClip(ClipData.newPlainText("Player UUID", text))
    return true
}
