package io.github.poodicraft.serverscope

import android.graphics.Bitmap
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tour against the real mcstatus.io API, run on an emulator in CI
 * (.github/workflows/ui-smoke.yml). It checks that nothing crashes along the main flows and
 * saves screenshots of each screen. Network-dependent steps accept an error screen as a valid
 * outcome, since the point is "never crash", not "the internet is up".
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val prefix = "api${Build.VERSION.SDK_INT}-"
    private val anyOutcome = arrayOf(
        "ONLINE", "OFFLINE", "No connection", "Timed out", "Too many lookups", "Service unavailable",
        "Unexpected response", "Something went wrong", "Invalid address",
    )

    @Test
    fun tourOfTheApp() {
        try {
            waitForText("Check any server")
            shot("01-home")

            check("demo.mcstatus.io", "Java Edition")
            waitForText(*anyOutcome)
            settle(2500)
            shot("02-java-result")

            val heads = compose.onAllNodesWithContentDescription("'s head", substring = true)
            if (heads.fetchSemanticsNodes().isNotEmpty()) {
                heads[0].performClick()
                waitForText("UUID")
                settle(3000)
                shot("03-player-sheet")
                shell("input keyevent 4")
                settle(800)
            }

            if (compose.onAllNodesWithText("NETWORK").fetchSemanticsNodes().isEmpty()) {
                compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("NETWORK"))
            }
            shot("04-java-result-details")

            compose.onNodeWithContentDescription("Add to favorites").performClick()
            settle(800)
            compose.onNodeWithContentDescription("Back").performClick()
            waitForText("FAVORITES")
            settle(1500)
            shot("05-home-with-favorite")

            check("demo.mcstatus.io", "Bedrock Edition")
            waitForText(*anyOutcome)
            settle(1000)
            shot("06-bedrock-result")
            compose.onNodeWithContentDescription("Back").performClick()

            check("mc.example.com", "Java Edition")
            waitForText(*anyOutcome)
            settle(1000)
            shot("07-offline-server")
            compose.onNodeWithContentDescription("Back").performClick()

            check("hypixel", "Java Edition")
            waitForText("Include the full domain")
            shot("08-invalid-address")

            shell("svc wifi disable")
            shell("svc data disable")
            settle(4000)
            check("play.example.org", "Java Edition")
            waitForText(*anyOutcome, timeoutMillis = 60_000)
            settle(500)
            shot("09-no-internet")
            shell("svc wifi enable")
            shell("svc data enable")
            compose.onNodeWithContentDescription("Back").performClick()
            settle(4000)

            // Resizing the display from the test process is unreliable on Android 7, so the tablet
            // layout is only checked on newer emulators.
            if (Build.VERSION.SDK_INT >= 26) {
                shell("wm size 2560x1600")
                shell("wm density 320")
                settle(4000)
                waitForText("Check any server")
                shot("10-tablet-home")
                check("demo.mcstatus.io", "Java Edition")
                waitForText(*anyOutcome)
                settle(2500)
                shot("11-tablet-result")
            }
        } finally {
            shell("svc wifi enable")
            shell("svc data enable")
            shell("wm size reset")
            shell("wm density reset")
        }
    }

    private fun check(address: String, edition: String) {
        compose.onNodeWithText(edition).performClick()
        val field = compose.onNode(hasSetTextAction())
        field.performTextClearance()
        field.performTextInput(address)
        compose.onNodeWithText("CHECK").performClick()
    }

    private fun waitForText(vararg texts: String, timeoutMillis: Long = 45_000) {
        compose.waitUntil(timeoutMillis) {
            texts.any { compose.onAllNodesWithText(it, substring = true).fetchSemanticsNodes().isNotEmpty() }
        }
    }

    /** Lets images load and animations finish before a screenshot. */
    private fun settle(millis: Long) {
        compose.waitForIdle()
        Thread.sleep(millis)
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        settle(300)
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val dir = File(instrumentation.targetContext.filesDir, "screenshots").apply { mkdirs() }
        FileOutputStream(File(dir, "$prefix$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Runs an adb-shell command as the shell user and waits for it to finish. */
    private fun shell(command: String) {
        val output = instrumentation.uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
    }
}
