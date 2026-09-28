package ai.metabind.bindjs.model

class TextComponent(
    props: TextComponentProps,
) : BaseComponent<TextComponentProps>(props)

/**
 * `Text`'s string, under whichever prop JS wrote it in. Resolved as bindjs-apple's
 * `TextComponent` does: `Text("…")` (`rawValue`), `{ markdown }` and `{ text }` are
 * markdown, `{ verbatim }` is drawn literally, and the first one present wins.
 */
class TextComponentProps(
    val markdown: String?,
    val rawValue: String?,
    children: List<BaseComponent<*>>?,
    val text: String? = null,
    val verbatim: String? = null,
) : Props(children = children) {

    /** The markdown to parse, or null when the text is [verbatim] (or absent). */
    val markdownSource: String?
        get() = rawValue ?: markdown ?: text

    /** The string as written, markup and all, for places that only take plain text. */
    val plainText: String
        get() = markdownSource ?: verbatim ?: ""

    override fun toString(): String {
        return "TextComponentProps(rawValue=$rawValue, markdown=$markdown, text=$text, verbatim=$verbatim)"
    }
}
