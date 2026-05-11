package ai.metabind.bindjs.model

class TextFieldComponent(
    props: TextFieldProps,
) : BaseComponent<TextFieldProps>(props)

class TextFieldProps(
    val placeholder: String? = null,
    val setTextId: String? = null,
    val text: String? = null,
) : Props(children = emptyList())