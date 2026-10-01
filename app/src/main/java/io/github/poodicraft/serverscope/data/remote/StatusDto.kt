package io.github.poodicraft.serverscope.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Raw response of `GET https://api.mcstatus.io/v2/status/{java|bedrock}/{address}`.
 *
 * One class covers both editions (the Bedrock response simply lacks some Java fields and adds
 * `gamemode`, `server_id` and `edition`). Every field is nullable with a default because servers
 * and the API omit fields freely; offline responses only carry the connection fields.
 */
@Serializable
data class StatusDto(
    val online: Boolean? = null,
    val host: String? = null,
    val port: Int? = null,
    @SerialName("ip_address") val ipAddress: String? = null,
    @SerialName("eula_blocked") val eulaBlocked: Boolean? = null,
    @SerialName("retrieved_at") val retrievedAt: Long? = null,
    @SerialName("expires_at") val expiresAt: Long? = null,
    @SerialName("srv_record") val srvRecord: SrvRecordDto? = null,
    val version: VersionDto? = null,
    val players: PlayersDto? = null,
    val motd: MotdDto? = null,
    val icon: String? = null,
    val mods: List<AddonDto?>? = null,
    val plugins: List<AddonDto?>? = null,
    val software: String? = null,
    val gamemode: String? = null,
    @SerialName("server_id") val serverId: String? = null,
    val edition: String? = null,
)

@Serializable
data class SrvRecordDto(
    val host: String? = null,
    val port: Int? = null,
)

@Serializable
data class VersionDto(
    /** Java only: version text with § formatting codes. */
    @SerialName("name_raw") val nameRaw: String? = null,
    /** Java only: version text without formatting. */
    @SerialName("name_clean") val nameClean: String? = null,
    /** Bedrock only. */
    val name: String? = null,
    val protocol: Long? = null,
)

@Serializable
data class PlayersDto(
    val online: Long? = null,
    val max: Long? = null,
    val list: List<PlayerDto?>? = null,
)

@Serializable
data class PlayerDto(
    val uuid: String? = null,
    @SerialName("name_raw") val nameRaw: String? = null,
    @SerialName("name_clean") val nameClean: String? = null,
)

@Serializable
data class MotdDto(
    val raw: String? = null,
    val clean: String? = null,
)

@Serializable
data class AddonDto(
    val name: String? = null,
    val version: String? = null,
)
