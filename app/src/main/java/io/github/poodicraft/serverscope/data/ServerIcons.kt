package io.github.poodicraft.serverscope.data

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object ServerIcons {
    private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    /**
     * Decodes a server favicon (`data:image/png;base64,...`, or bare base64) into PNG bytes.
     * Returns null for anything that isn't a decodable PNG.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun decodePng(dataUri: String?): ByteArray? {
        if (dataUri.isNullOrBlank()) return null
        val payload = dataUri.substringAfter("base64,", missingDelimiterValue = dataUri)
            .filterNot { it.isWhitespace() }
        if (payload.isEmpty()) return null
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        val bytes = try {
            Base64.Default.decode(padded)
        } catch (e: IllegalArgumentException) {
            return null
        }
        return bytes.takeIf { it.size > PNG_SIGNATURE.size && it.copyOf(PNG_SIGNATURE.size).contentEquals(PNG_SIGNATURE) }
    }
}
