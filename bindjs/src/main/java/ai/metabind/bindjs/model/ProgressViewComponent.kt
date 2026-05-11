package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.modifier.ComponentModifierProps

class ProgressViewComponent(
    props: ProgressViewComponentProps,
) : BaseComponent<ProgressViewComponentProps>(props)

class ProgressViewComponentProps(
    val value: Float?,
    val total: Float?,
) : ComponentModifierProps(emptyList())
