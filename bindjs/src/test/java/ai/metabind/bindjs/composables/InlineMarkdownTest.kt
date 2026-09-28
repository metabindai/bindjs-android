package ai.metabind.bindjs.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [inlineMarkdown] draws `Text` markdown the way SwiftUI's `inlineOnlyPreservingWhitespace`
 * parse does: inline styles only, block syntax literal, whitespace as written.
 */
class InlineMarkdownTest {

    private val accent = Color(0xFF007AFF)

    private fun render(markdown: String, decoration: TextDecoration = TextDecoration.None) =
        inlineMarkdown(markdown, accent, decoration)

    private fun AnnotatedString.styleOver(text: String): List<SpanStyle> {
        val start = this.text.indexOf(text)
        return spanStyles.filter { it.start <= start && it.end >= start + text.length }.map { it.item }
    }

    @Test
    fun `inline styles become spans and lose their markers`() {
        val result = render("**bold** _italic_ ~~gone~~ `code`")
        assertEquals("bold italic gone code", result.text)
        assertTrue(result.styleOver("bold").any { it.fontWeight == FontWeight.Bold })
        assertTrue(result.styleOver("italic").any { it.fontStyle == FontStyle.Italic })
        assertTrue(result.styleOver("gone").any { it.textDecoration == TextDecoration.LineThrough })
        assertTrue(result.styleOver("code").any { it.fontFamily == FontFamily.Monospace })
    }

    @Test
    fun `strikethrough keeps the text's own underline`() {
        val result = render("~~gone~~", TextDecoration.Underline)
        val decoration = result.styleOver("gone").mapNotNull { it.textDecoration }.single()
        assertTrue(decoration.contains(TextDecoration.Underline))
        assertTrue(decoration.contains(TextDecoration.LineThrough))
    }

    @Test
    fun `a link is an accent-coloured url annotation`() {
        val result = render("see [docs](https://metabind.ai/Docs)")
        assertEquals("see docs", result.text)
        val link = result.getLinkAnnotations(0, result.length).single()
        assertEquals("https://metabind.ai/Docs", (link.item as LinkAnnotation.Url).url)
        assertEquals(accent, link.item.styles?.style?.color)
        assertEquals(result.text.indexOf("docs"), link.start)
    }

    @Test
    fun `text case applies to what is shown, not to a link's url`() {
        val result = inlineMarkdown("[docs](https://metabind.ai/Docs)", accent) { it.uppercase() }
        assertEquals("DOCS", result.text)
        val link = result.getLinkAnnotations(0, result.length).single().item as LinkAnnotation.Url
        assertEquals("https://metabind.ai/Docs", link.url)
    }

    @Test
    fun `block syntax stays literal`() {
        val source = "# Title\n- item\n1. step\n> quote\n---"
        assertEquals(source, render(source).text)
    }

    @Test
    fun `newlines, blank lines and leading spaces are kept`() {
        assertEquals("a\nb", render("a\nb").text)
        assertEquals("a\n${NBSP}\n${NBSP}\nb", render("a\n\n\nb").text)
        assertEquals("a\n${NBSP}${NBSP}b", render("a\n  b").text)
    }

    @Test
    fun `whitespace pre-pass swaps only leading and blank-line whitespace`() {
        assertEquals("**bold** and `code`\nnext", preservingWhitespace("**bold** and `code`\nnext"))
        assertEquals("${NBSP}${NBSP}x y", preservingWhitespace(" \tx y"))
        assertEquals("a\n${NBSP}${NBSP}${NBSP}\nb", preservingWhitespace("a\n   \nb"))
    }

    private companion object {
        const val NBSP = '\u00A0'
    }
}
