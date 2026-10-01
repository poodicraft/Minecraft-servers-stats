package io.github.poodicraft.serverscope.data

import java.net.IDN

/** A validated server address: a host name or IP literal, plus an optional port. */
data class ServerAddress(val host: String, val port: Int? = null) {

    /** The form sent to the status API and shown to the user, e.g. `play.example.net:25566`. */
    val query: String
        get() {
            val hostPart = if (host.contains(':')) "[$host]" else host
            return if (port == null) hostPart else "$hostPart:$port"
        }

    override fun toString(): String = query

    companion object {
        private const val GENERIC_ERROR =
            "That doesn't look like a server address. Use a domain or IP, e.g. play.example.net:25565"

        private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
        private val IPV4 = Regex("""\d{1,3}(\.\d{1,3}){3}""")
        private val LABEL = Regex("[a-z0-9_]([a-z0-9_-]{0,61}[a-z0-9_])?")

        /**
         * Parses what the user typed. Accepts `host`, `host:port`, IPv4, bracketed IPv6
         * (`[2001:db8::1]:25565`) and tolerates pasted URLs like `minecraft://host/`.
         */
        fun parse(input: String): AddressResult {
            var text = input.trim()
            if (text.isEmpty()) return AddressResult.Invalid("Enter a server address, like play.example.net")

            SCHEME.find(text)?.let { text = text.substring(it.range.last + 1) }
            text = text.trimEnd('/')
            if (text.isEmpty() || text.any { it.isWhitespace() || it in "/?#@" }) {
                return AddressResult.Invalid(GENERIC_ERROR)
            }

            val host: String
            val portText: String?
            if (text.startsWith("[")) {
                val close = text.indexOf(']')
                if (close < 0) return AddressResult.Invalid(GENERIC_ERROR)
                host = text.substring(1, close)
                val rest = text.substring(close + 1)
                portText = when {
                    rest.isEmpty() -> null
                    rest.startsWith(":") -> rest.substring(1)
                    else -> return AddressResult.Invalid(GENERIC_ERROR)
                }
                if (!isIpv6(host)) return AddressResult.Invalid(GENERIC_ERROR)
            } else {
                val colons = text.count { it == ':' }
                if (colons > 1) {
                    return AddressResult.Invalid("Put IPv6 addresses in brackets, e.g. [2001:db8::1]:25565")
                }
                host = text.substringBefore(':')
                portText = if (colons == 1) text.substringAfter(':') else null
            }

            val port = portText?.let {
                val value = it.toIntOrNull()
                if (value == null || value !in 1..65535) {
                    return AddressResult.Invalid("The port must be a number from 1 to 65535")
                }
                value
            }

            if (text.startsWith("[")) return AddressResult.Valid(ServerAddress(host.lowercase(), port))

            val normalized = host.trimEnd('.').lowercase()
            if (normalized.isEmpty()) return AddressResult.Invalid(GENERIC_ERROR)
            if (normalized.all { it.isDigit() || it == '.' }) {
                val valid = IPV4.matches(normalized) && normalized.split('.').all { it.toInt() in 0..255 }
                return if (valid) {
                    AddressResult.Valid(ServerAddress(normalized, port))
                } else {
                    AddressResult.Invalid("That IP address isn't valid. It should look like 203.0.113.7")
                }
            }
            if (!normalized.contains('.')) {
                return AddressResult.Invalid("Include the full domain, e.g. mc.hypixel.net")
            }
            val ascii = try {
                IDN.toASCII(normalized, IDN.ALLOW_UNASSIGNED)
            } catch (e: IllegalArgumentException) {
                return AddressResult.Invalid(GENERIC_ERROR)
            }
            val labels = ascii.split('.')
            val validHost = ascii.length <= 253 &&
                labels.all { LABEL.matches(it) } &&
                !labels.last().all { it.isDigit() }
            return if (validHost) {
                AddressResult.Valid(ServerAddress(ascii, port))
            } else {
                AddressResult.Invalid(GENERIC_ERROR)
            }
        }

        private fun isIpv6(host: String): Boolean =
            host.contains(':') && host.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it == ':' || it == '.' }
    }
}

sealed interface AddressResult {
    data class Valid(val address: ServerAddress) : AddressResult
    data class Invalid(val message: String) : AddressResult
}
