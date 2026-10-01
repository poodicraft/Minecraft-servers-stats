package io.github.poodicraft.serverscope.data

import io.github.poodicraft.serverscope.data.model.Addon
import io.github.poodicraft.serverscope.data.model.Edition
import io.github.poodicraft.serverscope.data.model.Player
import io.github.poodicraft.serverscope.data.model.PlayerInfo
import io.github.poodicraft.serverscope.data.model.ServerStatus
import io.github.poodicraft.serverscope.data.model.SrvRecord
import io.github.poodicraft.serverscope.data.remote.AddonDto
import io.github.poodicraft.serverscope.data.remote.MotdDto
import io.github.poodicraft.serverscope.data.remote.PlayersDto
import io.github.poodicraft.serverscope.data.remote.StatusDto
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class StatusParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Turns mcstatus.io v2 JSON into a [ServerStatus]. Missing fields are fine; malformed JSON is not. */
object StatusParser {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val FORMATTING_CODE = Regex("§.?")

    /** @throws StatusParseException when [body] isn't a status response. */
    fun parse(body: String, edition: Edition): ServerStatus {
        val dto = try {
            json.decodeFromString(StatusDto.serializer(), body)
        } catch (e: SerializationException) {
            throw StatusParseException("Malformed status response", e)
        } catch (e: IllegalArgumentException) {
            throw StatusParseException("Malformed status response", e)
        }
        return map(dto, edition)
    }

    fun map(dto: StatusDto, edition: Edition): ServerStatus {
        val hasStatusData = dto.version != null || dto.players != null || dto.motd != null
        val online = dto.online
            ?: if (hasStatusData) true else throw StatusParseException("Response has no status information")

        return ServerStatus(
            edition = edition,
            online = online,
            host = dto.host.clean(),
            port = dto.port?.takeIf { it in 1..65535 },
            ipAddress = dto.ipAddress.clean(),
            eulaBlocked = dto.eulaBlocked,
            srvRecord = dto.srvRecord?.let { srv -> srv.host.clean()?.let { SrvRecord(it, srv.port) } },
            retrievedAt = dto.retrievedAt?.takeIf { it > 0 },
            expiresAt = dto.expiresAt?.takeIf { it > 0 },
            version = dto.version?.let { v -> (v.nameClean ?: v.name ?: v.nameRaw?.let(::stripFormatting)).clean() },
            protocol = dto.version?.protocol?.takeIf { it in 0..Int.MAX_VALUE }?.toInt(),
            software = dto.software.clean(),
            motd = cleanMotd(dto.motd),
            iconDataUri = dto.icon.clean(),
            players = mapPlayers(dto.players),
            mods = mapAddons(dto.mods),
            plugins = mapAddons(dto.plugins),
            gamemode = dto.gamemode.clean(),
            bedrockEdition = dto.edition.clean(),
            serverId = dto.serverId.clean(),
        )
    }

    fun stripFormatting(text: String): String = text.replace(FORMATTING_CODE, "")

    /** Lower-case, dashed UUID, or null for anything that isn't a real (non-nil) UUID. */
    fun normalizeUuid(raw: String?): String? {
        val hex = raw?.trim()?.replace("-", "")?.lowercase() ?: return null
        if (hex.length != 32 || !hex.all { it in '0'..'9' || it in 'a'..'f' }) return null
        if (hex.all { it == '0' }) return null
        return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-" +
            "${hex.substring(16, 20)}-${hex.substring(20)}"
    }

    private fun isNilUuid(raw: String): Boolean {
        val hex = raw.trim().replace("-", "")
        return hex.isNotEmpty() && hex.all { it == '0' }
    }

    private fun cleanMotd(dto: MotdDto?): String? {
        val text = dto?.clean ?: dto?.raw?.let(::stripFormatting) ?: return null
        return text.lines().joinToString("\n") { it.trim() }.trim().takeIf { it.isNotEmpty() }
    }

    private fun mapPlayers(dto: PlayersDto?): PlayerInfo? {
        if (dto == null) return null
        val players = mutableListOf<Player>()
        val messages = mutableListOf<String>()
        for (entry in dto.list.orEmpty().filterNotNull()) {
            val name = (entry.nameClean ?: entry.nameRaw?.let(::stripFormatting))?.trim().orEmpty()
            // Player names never contain spaces; entries that do (or that use the all-zero UUID)
            // are text lines the server shows in the hover tooltip, not players.
            val isMessage = entry.uuid?.let(::isNilUuid) == true || name.any { it.isWhitespace() }
            when {
                name.isEmpty() -> Unit
                isMessage -> messages += name
                else -> players += Player(name = name, uuid = normalizeUuid(entry.uuid))
            }
        }
        return PlayerInfo(
            online = dto.online?.toCount(),
            max = dto.max?.toCount(),
            list = players.distinctBy { it.uuid ?: it.name.lowercase() },
            messages = messages,
        )
    }

    private fun mapAddons(list: List<AddonDto?>?): List<Addon> =
        list.orEmpty().filterNotNull().mapNotNull { addon ->
            addon.name.clean()?.let { Addon(name = it, version = addon.version.clean()) }
        }

    private fun Long.toCount(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
