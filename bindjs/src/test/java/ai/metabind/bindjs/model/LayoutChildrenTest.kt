package ai.metabind.bindjs.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * `Empty()` is how a BindJS component writes an absent optional element — A2UI's
 * CheckBox emits one in place of its validation message, DateTimeInput in place of its
 * format hint. It must stay invisible to the enclosing stack's sizing: when it did not,
 * `NonModifiedComponent` wrapped it in a `fillMaxWidth` Box that stretched the CheckBox's
 * VStack across the whole Row and left its siblings zero width — the A2UI task card
 * rendered as a toggle on an otherwise blank card.
 */
class LayoutChildrenTest {

    private fun text(t: String) =
        TextComponent(TextComponentProps(markdown = null, rawValue = t, children = null))

    @Test
    fun `drops empty children`() {
        val kept = text("hello")
        val children = listOf(kept, EmptyComponent(), text("world"))
        assertEquals(2, children.layoutChildren()?.size)
        assertSame(kept, children.layoutChildren()?.first())
    }

    @Test
    fun `returns the same list when there is nothing to drop`() {
        val children = listOf<BaseComponent<*>?>(text("a"), text("b"))
        // Identity, not just equality: the common case must not allocate.
        assertSame(children, children.layoutChildren())
    }

    @Test
    fun `handles all-empty and null`() {
        assertEquals(emptyList<BaseComponent<*>?>(), listOf(EmptyComponent()).layoutChildren())
        assertNull(null.layoutChildren())
    }
}
