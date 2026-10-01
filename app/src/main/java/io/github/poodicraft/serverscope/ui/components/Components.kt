package io.github.poodicraft.serverscope.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.poodicraft.serverscope.AppContainer
import io.github.poodicraft.serverscope.R
import io.github.poodicraft.serverscope.ServerScopeApp
import io.github.poodicraft.serverscope.data.ServerIcons
import io.github.poodicraft.serverscope.data.model.Edition
import io.github.poodicraft.serverscope.data.model.Player
import io.github.poodicraft.serverscope.ui.theme.Mc
import io.github.poodicraft.serverscope.ui.theme.PixelLabel
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current
    return remember(context) { (context.applicationContext as ServerScopeApp).container }
}

fun shadesOf(color: Color): List<Color> =
    listOf(color, lerp(color, Color.Black, 0.2f), lerp(color, Color.White, 0.12f), color)

/** A row of randomly shaded square pixels, like the edge of a block texture. */
@Composable
fun PixelStrip(colors: List<Color>, modifier: Modifier = Modifier, pixelSize: Dp = 4.dp, seed: Int = 7) {
    Canvas(modifier.fillMaxWidth().height(pixelSize)) {
        val px = pixelSize.toPx()
        val random = Random(seed)
        repeat(ceil(size.width / px).toInt()) { column ->
            drawRect(colors[random.nextInt(colors.size)], topLeft = Offset(column * px, 0f), size = Size(px, px))
        }
    }
}

/** Decorative grass-on-dirt band drawn as pixel art. */
@Composable
fun GrassBanner(modifier: Modifier = Modifier, pixelSize: Dp = 6.dp, rows: Int = 5) {
    val grass = listOf(Mc.GrassLight, Mc.Grass, Color(0xFF5EA634), Mc.GrassLight)
    val dirt = listOf(Mc.Dirt, Mc.DirtLight, Mc.DirtDark, Mc.Dirt)
    Canvas(modifier.fillMaxWidth().height(pixelSize * rows)) {
        val px = pixelSize.toPx()
        val random = Random(42)
        repeat(ceil(size.width / px).toInt()) { column ->
            val grassDepth = 2 + if (random.nextInt(4) == 0) 1 else 0
            repeat(rows) { row ->
                val palette = if (row < grassDepth) grass else dirt
                val color = if (row >= 3 && random.nextInt(14) == 0) Mc.Stone else palette[random.nextInt(palette.size)]
                drawRect(color, topLeft = Offset(column * px, row * px), size = Size(px, px))
            }
        }
    }
}

/** The app's card: a stone-dark surface with a pixel accent strip on top. */
@Composable
fun McCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            PixelStrip(colors = shadesOf(accent), seed = title.hashCode())
            Column(Modifier.padding(16.dp)) {
                if (title != null || action != null) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title?.uppercase().orEmpty(),
                            style = PixelLabel,
                            color = accent,
                            modifier = Modifier.weight(1f),
                        )
                        action?.invoke()
                    }
                    Spacer(Modifier.height(12.dp))
                }
                content()
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(Mc.Grass))
        Spacer(Modifier.width(10.dp))
        Text(
            text = title.uppercase(),
            style = PixelLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

/** A square status light; pulses gently when [pulsing]. */
@Composable
fun PixelDot(color: Color, modifier: Modifier = Modifier, pulsing: Boolean = false, size: Dp = 10.dp) {
    val alpha = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "pulse")
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "pulseAlpha",
        ).value
    } else {
        1f
    }
    Box(
        modifier
            .size(size)
            .graphicsLayer { this.alpha = alpha }
            .background(color, RectangleShape),
    )
}

@Composable
fun StatusBadge(online: Boolean, modifier: Modifier = Modifier) {
    val color = if (online) Mc.Emerald else Mc.Redstone
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            PixelDot(color, pulsing = online, size = 8.dp)
            Spacer(Modifier.width(8.dp))
            Text(if (online) "ONLINE" else "OFFLINE", style = PixelLabel, color = color)
        }
    }
}

@Composable
fun EditionTag(edition: Edition, modifier: Modifier = Modifier) {
    val color = if (edition == Edition.JAVA) Mc.Gold else Mc.Diamond
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
    ) {
        Text(
            text = edition.label.uppercase(),
            style = PixelLabel.copy(fontSize = 8.sp, lineHeight = 12.sp),
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

@Composable
fun InfoNote(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info,
    tint: Color = MaterialTheme.colorScheme.tertiary,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(tint.copy(alpha = 0.10f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    monospace: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(118.dp),
        )
        Text(
            text = value,
            style = if (monospace) {
                MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = valueColor,
            modifier = Modifier.weight(1f),
        )
    }
}

/** An XP-bar style meter with block segments. Animates whenever [fraction] changes. */
@Composable
fun PixelProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = Mc.Emerald,
    segments: Int = 20,
    height: Dp = 14.dp,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "fill",
    )
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier.fillMaxWidth().height(height)) {
        val border = 2.dp.toPx()
        drawRect(Mc.Obsidian)
        val inner = Size(size.width - 2 * border, size.height - 2 * border)
        drawRect(track, topLeft = Offset(border, border), size = inner)
        val filled = inner.width * animated
        if (filled > 0f) {
            drawRect(color, topLeft = Offset(border, border), size = Size(filled, inner.height))
            drawRect(Color.White.copy(alpha = 0.28f), topLeft = Offset(border, border), size = Size(filled, inner.height * 0.3f))
            drawRect(
                Color.Black.copy(alpha = 0.2f),
                topLeft = Offset(border, border + inner.height * 0.72f),
                size = Size(filled, inner.height * 0.28f),
            )
        }
        val segment = inner.width / segments
        for (i in 1 until segments) {
            drawRect(
                Mc.Obsidian.copy(alpha = 0.6f),
                topLeft = Offset(border + segment * i - border / 4, border),
                size = Size(border / 2, inner.height),
            )
        }
    }
}

/** Three bouncing blocks. */
@Composable
fun LoadingBlocks(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loading")
    Row(
        modifier = modifier.height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(Mc.Grass, Mc.DirtLight, Mc.Stone).forEachIndexed { index, color ->
            val lift by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(420, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 140),
                ),
                label = "block$index",
            )
            Box(
                Modifier
                    .offset { IntOffset(0, -(lift * 18.dp.toPx()).roundToInt()) }
                    .size(18.dp)
                    .background(color)
                    .border(2.dp, Color.Black.copy(alpha = 0.25f)),
            )
        }
    }
}

/** The server's favicon, or a grass block when it has none. */
@Composable
fun ServerIcon(dataUri: String?, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val bitmap = remember(dataUri) {
        ServerIcons.decodePng(dataUri)?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }
    val shape = MaterialTheme.shapes.small
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Mc.Obsidian)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Server icon",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_server_default),
                contentDescription = "Default server icon",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(size / 8),
            )
        }
    }
}

/** Player head from mc-heads.net by UUID, falling back to minotar.net by name. */
@Composable
fun PlayerHead(player: Player, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    var useFallback by remember(player) { mutableStateOf(false) }
    val placeholder = painterResource(R.drawable.ic_head_placeholder)
    AsyncImage(
        model = if (useFallback) player.headFallbackUrl ?: player.headUrl else player.headUrl,
        contentDescription = "${player.name}'s head",
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.extraSmall),
        placeholder = placeholder,
        error = placeholder,
        filterQuality = FilterQuality.None,
        onError = { if (!useFallback && player.headFallbackUrl != null) useFallback = true },
    )
}
