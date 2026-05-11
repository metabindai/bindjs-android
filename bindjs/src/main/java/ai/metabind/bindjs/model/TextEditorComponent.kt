package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.modifier.ComponentModifierProps

class TextEditorComponentProps(
    val rawValue: String?,
) : ComponentModifierProps(emptyList())

class TextEditorComponent(
    props: TextEditorComponentProps,
) : BaseComponent<TextEditorComponentProps>(props)
