package io.github.poodicraft.serverscope.data.model

import java.net.URLEncoder

/** Everything ServerScope shows about a server, cleaned up from the API response. */
data class ServerStatus(
    val edition: Edition,
    val online: Boolean,
    val host: String?,
    val port: Int?,
    val ipAddress: String?,
    val eulaBlocked: Boolean?,
    val srvRecord: SrvRecord?,
    /** Epoch millis when mcstatus.io pinged the server. */
    val retrievedAt: Long?,
    /** Epoch millis when mcstatus.io's cached result expires. */
    val expiresAt: Long?,
    val version: String?,
    val protocol: Int?,
    val software: String?,
    val motd: String?,
    /** `data:image/png;base64,...` as sent by Java servers. */
    val iconDataUri: String?,
    val players: PlayerInfo?,
    val mods: List<Addon>,
    val plugins: List<Addon>,
    /** Bedrock only. */
    val gamemode: String?,
    /** Bedrock only: `MCPE` or `MCEE`. */
    val bedrockEdition: String?,
    /** Bedrock only. */
    val serverId: String?,
) {
    val playerListVisibility: PlayerListVisibility
        get() {
            val info = players
            return when {
                !online || info == null -> PlayerListVisibility.NOT_REPORTED
                (info.online ?: 0) <= 0 && info.list.isEmpty() -> PlayerListVisibility.NOBODY_ONLINE
                edition == Edition.BEDROCK -> PlayerListVisibility.NOT_SUPPORTED
                info.list.isEmpty() -> PlayerListVisibility.HIDDEN
                info.online != null && info.list.size < info.online -> PlayerListVisibility.PARTIAL
                else -> PlayerListVisibility.FULL
            }
        }
}

enum class PlayerListVisibility {
    /** Every online player is listed. */
    FULL,

    /** The server only shares a sample of who is online (vanilla caps it at 12). */
    PARTIAL,

    /** Players are online but the server shares no names. */
    HIDDEN,

    NOBODY_ONLINE,

    /** The Bedrock status protocol has no player list at all. */
    NOT_SUPPORTED,

    /** Offline, or no player section in the response. */
    NOT_REPORTED,
}

data class SrvRecord(val host: String, val port: Int?)

data class Addon(val name: String, val version: String?)

data class PlayerInfo(
    val online: Int?,
    val max: Int?,
    /** Real players from the server's player sample. */
    val list: List<Player>,
    /**
     * Lines some servers put in the player sample instead of players (all-zero UUID),
     * e.g. "Welcome to the network!". Shown as text, never as players.
     */
    val messages: List<String>,
) {
    /** Fraction of slots in use, clamped to 0..1. */
    val fillFraction: Float
        get() {
            val count = online ?: return 0f
            val capacity = max ?: return 0f
            if (capacity <= 0) return 0f
            return (count.toFloat() / capacity).coerceIn(0f, 1f)
        }
}

data class Player(val name: String, val uuid: String?) {
    /** Primary head render: by UUID when we have one, otherwise by name. */
    val headUrl: String get() = uuid?.let(PlayerImages::headByUuid) ?: PlayerImages.headByName(name)

    /** Tried when [headUrl] fails; null when the primary is already the name-based fallback. */
    val headFallbackUrl: String? get() = if (uuid != null) PlayerImages.headByName(name) else null

    val bodyUrl: String get() = uuid?.let(PlayerImages::bodyByUuid) ?: PlayerImages.bodyByName(name)

    val bodyFallbackUrl: String? get() = if (uuid != null) PlayerImages.bodyByName(name) else null
}

object PlayerImages {
    fun headByUuid(uuid: String) = "https://mc-heads.net/avatar/$uuid/128"
    fun headByName(name: String) = "https://minotar.net/avatar/${encode(name)}/128"
    fun bodyByUuid(uuid: String) = "https://mc-heads.net/body/$uuid/256"
    fun bodyByName(name: String) = "https://minotar.net/body/${encode(name)}/256"

    private fun encode(name: String) = URLEncoder.encode(name, "UTF-8").replace("+", "%20")
}
