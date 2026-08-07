package ai.metabind.bindjs.composables.ext

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * SF Symbol names arrive from the JS bridge with arbitrary decorations appended
 * (`.fill`, `.circle`, `.circle.fill`), so [matchSystemIcon] walks shortening prefixes
 * rather than requiring an entry per variant. These pin that walk down.
 */
class SystemIconMappingTest {

    @Test
    fun `maps an exact name`() {
        assertEquals(Icons.AutoMirrored.Filled.Send, "paperplane".matchSystemIcon())
    }

    @Test
    fun `strips a fill decoration`() {
        // `paperplane.fill` is the variant A2UI content actually emits for a send button.
        assertEquals(Icons.AutoMirrored.Filled.Send, "paperplane.fill".matchSystemIcon())
        assertEquals(Icons.Filled.Favorite, "heart.fill".matchSystemIcon())
    }

    @Test
    fun `strips multiple decorations`() {
        assertEquals(Icons.Filled.Close, "xmark.circle.fill".matchSystemIcon())
    }

    @Test
    fun `prefers the longest matching prefix`() {
        // Both `checkmark.circle` and `checkmark` are mapped; the more specific entry wins
        // rather than being shortened past.
        assertEquals(Icons.Filled.CheckCircle, "checkmark.circle.fill".matchSystemIcon())
    }

    @Test
    fun `mirrors chevrons for RTL`() {
        assertEquals(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            "chevron.right".matchSystemIcon(),
        )
    }

    @Test
    fun `returns null for an unmapped name`() {
        assertNull("waveform.path.ecg".matchSystemIcon())
        assertNull("".matchSystemIcon())
    }

    @Test
    fun `keeps skip distinct from seek`() {
        // `forward.end` must not shorten to `forward`: A2UI's skipNext/skipPrevious and
        // fastForward/rewind are four separate icons that would otherwise collapse to two.
        assertEquals(VendoredIcons.SkipNext, "forward.end".matchSystemIcon())
        assertEquals(VendoredIcons.SkipPrevious, "backward.end".matchSystemIcon())
        assertEquals(VendoredIcons.FastForward, "forward".matchSystemIcon())
        assertEquals(VendoredIcons.FastRewind, "backward".matchSystemIcon())
    }

    @Test
    fun `maps every symbol the A2UI icon catalog emits`() {
        // The A2UI `Icon` primitive names 59 icons and resolves each to one of these SF
        // Symbols before the renderer ever sees it, so a miss here is a blank space on
        // screen — which is exactly how the music-player controls came out empty.
        val a2uiSymbols = listOf(
            "person.crop.circle", "plus", "arrow.left", "arrow.right", "paperclip",
            "calendar", "phone", "camera", "checkmark", "xmark", "trash",
            "arrow.down.circle", "pencil", "calendar.badge.clock", "exclamationmark.circle",
            "forward", "heart.fill", "heart", "folder", "questionmark.circle", "house",
            "info.circle", "location", "lock", "lock.open", "envelope", "line.3.horizontal",
            "ellipsis", "bell.slash", "bell", "pause", "creditcard", "person", "photo",
            "play", "printer", "arrow.clockwise", "backward", "magnifyingglass",
            "paperplane", "gearshape", "square.and.arrow.up", "cart", "forward.end",
            "backward.end", "star.fill", "star.leadinghalf.filled", "star", "stop",
            "arrow.up.circle", "eye", "eye.slash", "speaker.wave.1", "speaker",
            "speaker.slash", "speaker.wave.3", "exclamationmark.triangle",
        )
        // `photo` is the one name served by a drawable rather than an ImageVector.
        val unmapped = a2uiSymbols.filter { it != "photo" && it.matchSystemIcon() == null }
        assertEquals(emptyList<String>(), unmapped)
    }
}
