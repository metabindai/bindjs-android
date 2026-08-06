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
}
