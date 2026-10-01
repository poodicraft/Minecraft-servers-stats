package io.github.poodicraft.serverscope.data

import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusErrorTest {

    @Test
    fun `network exceptions map to friendly errors`() {
        assertEquals(StatusError.NoInternet, StatusError.fromThrowable(UnknownHostException("api.mcstatus.io")))
        assertEquals(StatusError.NoInternet, StatusError.fromThrowable(ConnectException("refused")))
        assertEquals(StatusError.NoInternet, StatusError.fromThrowable(IOException("reset")))
        assertEquals(StatusError.Timeout, StatusError.fromThrowable(SocketTimeoutException("read timed out")))
        assertEquals(StatusError.Timeout, StatusError.fromThrowable(InterruptedIOException("timeout")))
        assertEquals(StatusError.SecureConnection, StatusError.fromThrowable(SSLHandshakeException("bad cert")))
        assertEquals(StatusError.BadResponse, StatusError.fromThrowable(StatusParseException("bad")))
        assertEquals(StatusError.Unknown, StatusError.fromThrowable(IllegalStateException()))
    }

    @Test
    fun `HTTP status codes map to friendly errors`() {
        assertEquals(StatusError.InvalidAddress, StatusError.fromHttpCode(400))
        assertEquals(StatusError.RateLimited, StatusError.fromHttpCode(429))
        assertEquals(StatusError.ServiceUnavailable(503), StatusError.fromHttpCode(503))
    }
}
