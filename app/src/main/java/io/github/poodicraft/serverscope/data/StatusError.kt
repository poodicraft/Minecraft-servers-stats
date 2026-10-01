package io.github.poodicraft.serverscope.data

import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.serialization.SerializationException

/** Why a status lookup failed, with a friendly title and explanation for the UI. */
sealed class StatusError(val title: String, val message: String) {

    data object NoInternet : StatusError(
        "No connection",
        "Couldn't reach the status service. Check your internet connection and try again.",
    )

    data object Timeout : StatusError(
        "Timed out",
        "The lookup took too long. The server may be slow or unreachable, so try again in a moment.",
    )

    data object SecureConnection : StatusError(
        "Connection not secure",
        "Couldn't open a secure connection to the status service. On public Wi-Fi you may need to sign in first.",
    )

    data object InvalidAddress : StatusError(
        "Invalid address",
        "The status service couldn't use that address. Double-check the spelling and the port.",
    )

    data object RateLimited : StatusError(
        "Too many lookups",
        "The status service is rate-limiting requests. Wait a minute and try again.",
    )

    data class ServiceUnavailable(val code: Int) : StatusError(
        "Service unavailable",
        "The status service is having trouble right now (HTTP $code). Try again later.",
    )

    data object BadResponse : StatusError(
        "Unexpected response",
        "The status service sent data ServerScope couldn't read. Try again later.",
    )

    data object Unknown : StatusError(
        "Something went wrong",
        "The lookup failed for an unexpected reason. Try again.",
    )

    companion object {
        fun fromHttpCode(code: Int): StatusError = when (code) {
            400, 404, 422 -> InvalidAddress
            429 -> RateLimited
            else -> ServiceUnavailable(code)
        }

        fun fromThrowable(t: Throwable): StatusError = when (t) {
            is SocketTimeoutException -> Timeout
            is UnknownHostException, is ConnectException, is NoRouteToHostException -> NoInternet
            // OkHttp reports its call timeout as a bare InterruptedIOException("timeout").
            is InterruptedIOException -> Timeout
            is SSLException -> SecureConnection
            is StatusParseException, is SerializationException -> BadResponse
            is IOException -> NoInternet
            else -> Unknown
        }
    }
}
