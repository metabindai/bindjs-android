package ai.metabind.bindjs.model

class TextFieldComponent(
    props: TextFieldProps,
) : BaseComponent<TextFieldProps>(props)

/**
 * SwiftUI's `SecureField` — the same props as [TextFieldComponent], rendered with the
 * characters masked.
 *
 * Its own type rather than a flag on `TextFieldProps` because the JS side emits
 * `{ type: "SecureField" }`, and an unregistered type falls through to `EmptyComponent`:
 * before this existed, A2UI's `variant: "obscured"` — the password box on any login
 * form — rendered as nothing at all.
 */
class SecureFieldComponent(
    props: TextFieldProps,
) : BaseComponent<TextFieldProps>(props)

class TextFieldProps(
    val placeholder: String? = null,
    val setTextId: String? = null,
    val text: String? = null,
) : Props(children = emptyList())