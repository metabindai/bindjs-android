package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.modifier.ComponentModifierProps

/**
 * `rawValue` is what the JS bridge produces for the positional form, `TextEditor("text")`;
 * the binding form used by real content — `TextEditor({ text, setText })` — arrives as
 * `text` plus the stored-closure id `setTextId` (every `set*` prop is serialised that way,
 * as on [TextFieldProps]). Only `rawValue` was declared, so the binding form deserialised
 * to nulls: the editor rendered empty and had nothing to write edits back to.
 */
class TextEditorComponentProps(
    val rawValue: String?,
    val text: String? = null,
    val setTextId: String? = null,
) : ComponentModifierProps(emptyList()) {
    /** The positional and binding forms are mutually exclusive; either may carry the value. */
    val value: String get() = text ?: rawValue ?: ""
}

class TextEditorComponent(
    props: TextEditorComponentProps,
) : BaseComponent<TextEditorComponentProps>(props)
