package ai.metabind.bindjs

import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The environment BindJS supplies itself, in bindjs-apple's shape, so a component reading
 * `env.screen.width` or `env.colorScheme` gets a value whether or not the host passes one.
 */
class SystemEnvironmentTest {

    private fun env(dark: Boolean = false, rightToLeft: Boolean = false) = systemEnvironment(
        screenWidthDp = 411,
        screenHeightDp = 914,
        density = 2.625f,
        dark = dark,
        rightToLeft = rightToLeft,
        locale = Locale.US,
        timeZone = TimeZone.getTimeZone("America/Chicago"),
    )

    @Test
    fun `screen is in dp, like SwiftUI points`() =
        assertEquals(mapOf("width" to 411, "height" to 914), env()["screen"])

    @Test
    fun `display scale is the density`() = assertEquals(2.625f, env()["displayScale"])

    @Test
    fun `color scheme follows night mode`() {
        assertEquals("light", env()["colorScheme"])
        assertEquals("dark", env(dark = true)["colorScheme"])
    }

    @Test
    fun `layout direction uses bindjs-apple's names`() {
        assertEquals("leftToRight", env()["layoutDirection"])
        assertEquals("rightToLeft", env(rightToLeft = true)["layoutDirection"])
    }

    @Test
    fun `locale and time zone are identifiers`() {
        assertEquals("en_US", env()["locale"])
        assertEquals("America/Chicago", env()["timeZone"])
    }

    @Test
    fun `the host's values win over the defaults`() {
        // How JsRuntimeImpl merges them before sending.
        val merged = env() + mapOf("screen" to mapOf("width" to 300, "height" to 600), "brand" to "x")
        assertEquals(mapOf("width" to 300, "height" to 600), merged["screen"])
        assertEquals("x", merged["brand"])
        assertEquals("light", merged["colorScheme"])
    }
}
