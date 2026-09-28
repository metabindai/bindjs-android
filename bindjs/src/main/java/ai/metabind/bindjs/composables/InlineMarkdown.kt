package ai.metabind.bindjs.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.HtmlInline
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.parser.Parser

/**
 * `Text` markdown as SwiftUI draws it, as an [AnnotatedString] for a Compose `Text`.
 *
 * SwiftUI parses `Text` markdown as `inlineOnlyPreservingWhitespace`, so only inline
 * syntax is honoured: bold, italic, strikethrough, inline code and links. Headings, lists,
 * quotes, code blocks and rules stay literal text, which is why the parser runs with every
 * block type disabled. Rendering into the same `Text` as verbatim text, rather than a
 * platform TextView, is what lets every text modifier (gradients, tracking, line spacing,
 * selection, …) apply to markdown too.
 *
 * - Inline code is monospaced with no background, as on iOS.
 * - Links are drawn in [linkColor] (the accent colour, as SwiftUI tints them) without an
 *   underline, and open through `LocalUriHandler`, Compose's counterpart of the
 *   `openURL` environment action SwiftUI routes them through.
 * - [textCase] is applied to the displayed text only, never to a link's destination.
 * - [baseDecoration] is the text's own `.underline()` / `.strikethrough()`: a span's
 *   decoration replaces the base one in Compose, so `~~…~~` has to combine with it.
 */
internal fun inlineMarkdown(
    markdown: String,
    linkColor: Color,
    baseDecoration: TextDecoration = TextDecoration.None,
    textCase: (String) -> String = { it },
): AnnotatedString {
    val document = parser.parse(preservingWhitespace(markdown))
    return buildAnnotatedString {
        appendChildren(document, MarkdownStyle(linkColor, baseDecoration, textCase))
    }
}

private class MarkdownStyle(
    val linkColor: Color,
    val baseDecoration: TextDecoration,
    val textCase: (String) -> String,
)

private fun AnnotatedString.Builder.appendChildren(node: Node, style: MarkdownStyle) {
    var child = node.firstChild
    while (child != null) {
        appendNode(child, style)
        child = child.next
    }
}

private fun AnnotatedString.Builder.appendNode(node: Node, style: MarkdownStyle) {
    when (node) {
        is org.commonmark.node.Text -> append(style.textCase(node.literal))
        is Code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
            append(style.textCase(node.literal))
        }
        is Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendChildren(node, style) }
        is StrongEmphasis -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendChildren(node, style) }
        is Strikethrough -> withStyle(
            SpanStyle(textDecoration = TextDecoration.combine(listOf(style.baseDecoration, TextDecoration.LineThrough)))
        ) { appendChildren(node, style) }
        is Link -> withLink(
            LinkAnnotation.Url(node.destination, TextLinkStyles(style = SpanStyle(color = style.linkColor)))
        ) { appendChildren(node, style) }
        is SoftLineBreak, is HardLineBreak -> append('\n')
        // Not HTML to SwiftUI either; the markup is kept as written.
        is HtmlInline -> append(style.textCase(node.literal))
        is Paragraph -> {
            if (node.previous != null) append('\n')
            appendChildren(node, style)
        }
        // An image's children are its alt text, which is what is left to draw inline.
        else -> appendChildren(node, style)
    }
}

private val parser: Parser = Parser.builder()
    .enabledBlockTypes(emptySet())
    .extensions(listOf(StrikethroughExtension.create()))
    .build()

/**
 * Rewrite markdown so CommonMark keeps the whitespace SwiftUI keeps.
 *
 * SwiftUI keeps leading spaces and makes every newline a line break, however many there
 * are in a row. CommonMark strips a line's leading spaces, folds any run of blank lines
 * into one paragraph break and turns a single newline into a soft break. A non-breaking
 * space is none of those things to it: leading spaces and tabs become one each, and an
 * empty line becomes a line holding one, so the text stays a single paragraph whose soft
 * breaks [inlineMarkdown] draws as line breaks one for one.
 */
internal fun preservingWhitespace(markdown: String): String =
    markdown.lines().joinToString("\n") { line ->
        val indent = line.takeWhile { it == ' ' || it == '\t' }
        if (indent.length == line.length) {
            NBSP.repeat(maxOf(indent.length, 1))
        } else {
            NBSP.repeat(indent.length) + line.substring(indent.length)
        }
    }

private const val NBSP = "\u00A0"
