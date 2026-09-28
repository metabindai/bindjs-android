package ai.metabind.bindjs.model

import ai.metabind.bindjs.GsonProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `Text` carries its string under one of four props; which ones are markdown follows
 * bindjs-apple's `TextComponent`: `rawValue`, `markdown` and `text` are, `verbatim` is not.
 */
class TextPropsTest {

    private fun parse(props: String): TextComponentProps =
        (GsonProvider.get().fromJson("""{"type":"Text","props":$props}""", BaseComponent::class.java) as TextComponent).props

    @Test
    fun `rawValue markdown and text are all markdown`() {
        assertEquals("**a**", parse("""{"rawValue":"**a**"}""").markdownSource)
        assertEquals("**b**", parse("""{"markdown":"**b**"}""").markdownSource)
        assertEquals("**c**", parse("""{"text":"**c**"}""").markdownSource)
    }

    @Test
    fun `verbatim is not markdown but still has its text`() {
        val props = parse("""{"verbatim":"**d**"}""")
        assertNull(props.markdownSource)
        assertEquals("**d**", props.plainText)
    }

    @Test
    fun `the first prop present wins, in the order iOS reads them`() {
        assertEquals("raw", parse("""{"rawValue":"raw","markdown":"md","text":"t"}""").markdownSource)
        assertEquals("md", parse("""{"markdown":"md","text":"t","verbatim":"v"}""").markdownSource)
        assertEquals("t", parse("""{"text":"t","verbatim":"v"}""").markdownSource)
    }

    @Test
    fun `a Text with no string is empty`() {
        assertEquals("", parse("{}").plainText)
    }
}
