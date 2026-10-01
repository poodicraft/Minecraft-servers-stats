package io.github.poodicraft.serverscope.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerAddressTest {

    private fun valid(input: String): ServerAddress {
        val result = ServerAddress.parse(input)
        assertTrue("expected '$input' to be valid but got $result", result is AddressResult.Valid)
        return (result as AddressResult.Valid).address
    }

    private fun invalid(input: String): String {
        val result = ServerAddress.parse(input)
        assertTrue("expected '$input' to be invalid but got $result", result is AddressResult.Invalid)
        return (result as AddressResult.Invalid).message
    }

    @Test
    fun `domains with and without a port`() {
        assertEquals(ServerAddress("mc.hypixel.net", null), valid("mc.hypixel.net"))
        assertEquals(ServerAddress("play.example.net", 25566), valid("  Play.Example.NET:25566 "))
        assertEquals("play.example.net:25566", valid("play.example.net:25566").query)
        assertEquals(ServerAddress("mc.example.com", null), valid("mc.example.com."))
        assertEquals(ServerAddress("my-server_1.example.org", null), valid("my-server_1.example.org"))
    }

    @Test
    fun `IP addresses`() {
        assertEquals(ServerAddress("203.0.113.7", 19132), valid("203.0.113.7:19132"))
        assertEquals(ServerAddress("2001:db8::1", 25565), valid("[2001:db8::1]:25565"))
        assertEquals("[2001:db8::1]:25565", valid("[2001:DB8::1]:25565").query)
        assertTrue(invalid("999.1.1.1").contains("IP"))
        assertTrue(invalid("1.2.3").contains("IP"))
        assertTrue(invalid("2001:db8::1").contains("brackets"))
    }

    @Test
    fun `pasted URLs are tolerated`() {
        assertEquals(ServerAddress("play.example.net", null), valid("https://play.example.net/"))
        assertEquals(ServerAddress("play.example.net", 25565), valid("minecraft://play.example.net:25565"))
    }

    @Test
    fun `internationalized domains are converted to ASCII`() {
        assertEquals("xn--mnchen-3ya.example", valid("münchen.example").host)
    }

    @Test
    fun `bad input gets a helpful message`() {
        assertTrue(invalid("").contains("Enter"))
        assertTrue(invalid("   ").contains("Enter"))
        assertTrue(invalid("hypixel").contains("full domain"))
        assertTrue(invalid("play.example.net:0").contains("port"))
        assertTrue(invalid("play.example.net:65536").contains("port"))
        assertTrue(invalid("play.example.net:abc").contains("port"))
        assertTrue(invalid("play.example.net:").contains("port"))
        invalid("play example.net")
        invalid("play.example.net/path")
        invalid("-bad-.example.net")
        invalid("user@play.example.net")
        invalid("example.123")
    }
}
