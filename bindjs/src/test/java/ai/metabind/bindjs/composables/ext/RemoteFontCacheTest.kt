package ai.metabind.bindjs.composables.ext

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cache naming for remote fonts. The bytes are keyed by URL, so two hosts serving
 * different faces under the same filename must not collide, and one face fetched
 * through two spellings of the same URL is allowed to be fetched twice.
 */
class RemoteFontCacheTest {

    @Test
    fun `the name is the URL's hash plus its extension`() {
        val name = cacheFileName("https://example.com/fonts/Inter.ttf")
        assertTrue("expected a sha256 hex name, got $name", name.matches(Regex("[0-9a-f]{64}\\.ttf")))
    }

    @Test
    fun `the same URL always names the same file`() {
        assertEquals(
            cacheFileName("https://example.com/Inter.ttf"),
            cacheFileName("https://example.com/Inter.ttf"),
        )
    }

    @Test
    fun `hosts serving the same filename do not collide`() {
        assertNotEquals(
            cacheFileName("https://a.example.com/font.ttf"),
            cacheFileName("https://b.example.com/font.ttf"),
        )
    }

    @Test
    fun `a query string does not leak into the extension`() {
        // Taking the extension off the whole URL produced `…ttf?v=2` as a filename.
        assertTrue(cacheFileName("https://example.com/Inter.ttf?v=2").endsWith(".ttf"))
        assertTrue(cacheFileName("https://example.com/Inter.otf#bold").endsWith(".otf"))
    }

    @Test
    fun `a cache-busting query still keys its own entry`() {
        assertNotEquals(
            cacheFileName("https://example.com/Inter.ttf"),
            cacheFileName("https://example.com/Inter.ttf?v=2"),
        )
    }

    @Test
    fun `an extensionless or implausible extension URL is just the hash`() {
        assertTrue(cacheFileName("https://example.com/fonts/inter").matches(Regex("[0-9a-f]{64}")))
        // Not an extension — a version segment in the path.
        assertTrue(cacheFileName("https://example.com/fonts/inter/v1.20250101").matches(Regex("[0-9a-f]{64}")))
    }
}
