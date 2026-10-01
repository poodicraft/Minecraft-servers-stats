package io.github.poodicraft.serverscope.data

import io.github.poodicraft.serverscope.data.model.Edition
import io.github.poodicraft.serverscope.data.model.PlayerListVisibility
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The fixtures in src/test/resources/fixtures are unmodified responses captured from
 * https://api.mcstatus.io/v2/status/... on 2026-10-01.
 */
class StatusParserTest {

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("fixtures/$name")) { "missing fixture $name" }.readText()

    @Test
    fun `online Java server parses every section`() {
        val status = StatusParser.parse(fixture("java_online_demo.json"), Edition.JAVA)

        assertTrue(status.online)
        assertEquals("demo.mcstatus.io", status.host)
        assertEquals(25565, status.port)
        assertEquals("144.172.67.4", status.ipAddress)
        assertEquals(false, status.eulaBlocked)
        assertNull(status.srvRecord)
        assertEquals(1790844655305L, status.retrievedAt)
        assertEquals(1790844715305L, status.expiresAt)
        assertEquals("1.20.1", status.version)
        assertEquals(47, status.protocol)
        assertEquals("github.com/mcstatus-io/demo-server", status.software)
        assertNull(status.iconDataUri)

        // MOTD: the clean text, with the centering spaces trimmed from each line.
        assertEquals(";;; >>> Minecraft Server Status <<< ;;;\nhttps://mcstatus.io/", status.motd)

        val players = requireNotNull(status.players)
        assertEquals(71, players.online)
        assertEquals(100, players.max)
        assertEquals(0.71f, players.fillFraction, 0.0001f)
        assertEquals(listOf("Leaks22", "PassTheMayo"), players.list.map { it.name })
        assertEquals("86c95eb8-302c-4b82-ad57-fc9f148efa2d", players.list[0].uuid)
        assertTrue(players.messages.isEmpty())
        // 2 names for 71 players online: the server only shares a sample.
        assertEquals(PlayerListVisibility.PARTIAL, status.playerListVisibility)

        assertEquals(3, status.mods.size)
        assertEquals("applied-energistics-2", status.mods[0].name)
        assertEquals("11.7.2", status.mods[0].version)
        assertEquals(
            listOf("WorldEdit", "Dynmap", "Holographic-Displays", "Multiverse-Core", "EssentialsX"),
            status.plugins.map { it.name },
        )
    }

    @Test
    fun `offline Java server that does not resolve`() {
        val status = StatusParser.parse(fixture("java_offline_unresolved.json"), Edition.JAVA)

        assertFalse(status.online)
        assertEquals("mc.example.com", status.host)
        assertEquals(25565, status.port)
        assertNull(status.ipAddress)
        assertNull(status.players)
        assertNull(status.version)
        assertNull(status.motd)
        assertTrue(status.mods.isEmpty())
        assertTrue(status.plugins.isEmpty())
        assertEquals(PlayerListVisibility.NOT_REPORTED, status.playerListVisibility)
    }

    @Test
    fun `offline Java server keeps its resolved IP`() {
        val status = StatusParser.parse(fixture("java_offline_resolved.json"), Edition.JAVA)

        assertFalse(status.online)
        assertEquals("mc.hypixel.net", status.host)
        assertEquals("172.65.197.160", status.ipAddress)
        assertEquals(false, status.eulaBlocked)
    }

    @Test
    fun `online Bedrock server`() {
        val status = StatusParser.parse(fixture("bedrock_online_demo.json"), Edition.BEDROCK)

        assertTrue(status.online)
        assertEquals(Edition.BEDROCK, status.edition)
        assertEquals(19132, status.port)
        assertEquals("1.19.70", status.version)
        assertEquals(575, status.protocol)
        assertEquals("Survival", status.gamemode)
        assertEquals("MCPE", status.bedrockEdition)
        assertEquals("12607766728786842809", status.serverId)
        assertEquals("A Bedrock server\nYou cannot connect!", status.motd)
        assertNull(status.software)
        val players = requireNotNull(status.players)
        assertEquals(85, players.online)
        assertEquals(100, players.max)
        assertTrue(players.list.isEmpty())
        // Bedrock's status protocol never includes names; that's not the server hiding them.
        assertEquals(PlayerListVisibility.NOT_SUPPORTED, status.playerListVisibility)
    }

    @Test
    fun `Bedrock MOTD with formatting codes and a big player count`() {
        val status = StatusParser.parse(fixture("bedrock_online_cubecraft.json"), Edition.BEDROCK)

        assertEquals("BEDWARS UPDATE: NEW ITEMS & MAPS\nC", status.motd)
        assertEquals(4291, status.players?.online)
        assertEquals(55000, status.players?.max)
        assertEquals("1.26.50", status.version)

        val hive = StatusParser.parse(fixture("bedrock_online_hive.json"), Edition.BEDROCK)
        assertEquals(100001, hive.players?.max)
        assertEquals("-7289536331672023015", hive.serverId)
    }

    @Test
    fun `offline Bedrock server`() {
        val status = StatusParser.parse(fixture("bedrock_offline.json"), Edition.BEDROCK)

        assertFalse(status.online)
        assertEquals(19132, status.port)
        assertNull(status.players)
        assertNull(status.gamemode)
    }

    @Test
    fun `missing fields never crash`() {
        val status = StatusParser.parse("""{"online":true}""", Edition.JAVA)

        assertTrue(status.online)
        assertNull(status.host)
        assertNull(status.players)
        assertEquals(PlayerListVisibility.NOT_REPORTED, status.playerListVisibility)

        val noListField = StatusParser.parse("""{"online":true,"players":{"online":5,"max":20}}""", Edition.JAVA)
        assertEquals(PlayerListVisibility.HIDDEN, noListField.playerListVisibility)
    }

    @Test
    fun `nulls, unknown keys and odd values are tolerated`() {
        val body = """
            {"online":true,"host":null,"port":0,"brand_new_field":{"x":1},
             "version":{"name_raw":"§aPaper §l1.21","protocol":-1},
             "players":{"online":-3,"max":null,"list":[null,{"uuid":null,"name_clean":"Steve"}]},
             "motd":{"raw":"§6Hello\n§cWorld"},
             "mods":[null,{"name":"  ","version":"1"},{"name":"jei","version":""}]}
        """.trimIndent()

        val status = StatusParser.parse(body, Edition.JAVA)

        assertNull(status.port)
        assertEquals("Paper 1.21", status.version)
        assertNull(status.protocol)
        assertEquals("Hello\nWorld", status.motd)
        val players = requireNotNull(status.players)
        assertEquals(0, players.online)
        assertNull(players.max)
        assertEquals(0f, players.fillFraction)
        assertEquals("Steve", players.list.single().name)
        assertNull(players.list.single().uuid)
        assertEquals(1, status.mods.size)
        assertEquals("jei", status.mods.single().name)
        assertNull(status.mods.single().version)
    }

    @Test
    fun `text lines in the player sample are not treated as players`() {
        // Many networks put hover text in the sample using the all-zero UUID.
        val body = """
            {"online":true,"players":{"online":1500,"max":2000,"list":[
              {"uuid":"00000000-0000-0000-0000-000000000000","name_clean":"Welcome to the network!"},
              {"uuid":"00000000-0000-0000-0000-000000000000","name_clean":"Play now"},
              {"uuid":"85e5f06eff894c118050329e8fdc29de","name_clean":"PassTheMayo"}]}}
        """.trimIndent()

        val players = requireNotNull(StatusParser.parse(body, Edition.JAVA).players)

        assertEquals(listOf("PassTheMayo"), players.list.map { it.name })
        assertEquals("85e5f06e-ff89-4c11-8050-329e8fdc29de", players.list.single().uuid)
        assertEquals(listOf("Welcome to the network!", "Play now"), players.messages)
    }

    @Test
    fun `only text lines means the list is hidden`() {
        val body = """
            {"online":true,"players":{"online":42,"max":100,"list":[
              {"uuid":"00000000-0000-0000-0000-000000000000","name_clean":"Join our Discord!"}]}}
        """.trimIndent()

        val status = StatusParser.parse(body, Edition.JAVA)

        assertEquals(PlayerListVisibility.HIDDEN, status.playerListVisibility)
        assertEquals(listOf("Join our Discord!"), status.players?.messages)
    }

    @Test
    fun `full and empty player lists`() {
        val full = StatusParser.parse(
            """{"online":true,"players":{"online":1,"max":10,"list":[{"uuid":"86c95eb8-302c-4b82-ad57-fc9f148efa2d","name_clean":"Leaks22"}]}}""",
            Edition.JAVA,
        )
        assertEquals(PlayerListVisibility.FULL, full.playerListVisibility)

        val empty = StatusParser.parse("""{"online":true,"players":{"online":0,"max":10,"list":[]}}""", Edition.JAVA)
        assertEquals(PlayerListVisibility.NOBODY_ONLINE, empty.playerListVisibility)
    }

    @Test(expected = StatusParseException::class)
    fun `non-JSON body is rejected`() {
        StatusParser.parse("Invalid address value", Edition.JAVA)
    }

    @Test(expected = StatusParseException::class)
    fun `wrong types are rejected rather than half-parsed`() {
        StatusParser.parse("""{"online":true,"players":"lots"}""", Edition.JAVA)
    }

    @Test(expected = StatusParseException::class)
    fun `object without any status data is rejected`() {
        StatusParser.parse("""{"error":"something"}""", Edition.JAVA)
    }

    @Test
    fun `player image URLs use UUID first and fall back to the name`() {
        val players = requireNotNull(StatusParser.parse(fixture("java_online_demo.json"), Edition.JAVA).players)
        val player = players.list.first()

        assertEquals("https://mc-heads.net/avatar/86c95eb8-302c-4b82-ad57-fc9f148efa2d/128", player.headUrl)
        assertEquals("https://minotar.net/avatar/Leaks22/128", player.headFallbackUrl)
        assertEquals("https://mc-heads.net/body/86c95eb8-302c-4b82-ad57-fc9f148efa2d/256", player.bodyUrl)

        val noUuid = io.github.poodicraft.serverscope.data.model.Player(name = "Steve", uuid = null)
        assertEquals("https://minotar.net/avatar/Steve/128", noUuid.headUrl)
        assertNull(noUuid.headFallbackUrl)
    }

    @Test
    fun `server icon data URI decodes to PNG bytes`() {
        val dataUri = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0kAAAAFUlEQVR4nGOIW2byH4QZqgJM/oMwAErgCGl8i4VbAAAAAElFTkSuQmCC"

        val bytes = ServerIcons.decodePng(dataUri)

        assertNotNull(bytes)
        assertEquals(78, bytes!!.size)
        assertArrayEquals(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()), bytes.copyOf(4))
        assertNull(ServerIcons.decodePng(null))
        assertNull(ServerIcons.decodePng("data:image/png;base64,not-base64!!"))
        assertNull(ServerIcons.decodePng("data:image/png;base64,aGVsbG8="))
    }
}
