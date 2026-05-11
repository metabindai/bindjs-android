package ai.metabind.bindjs

import java.io.Serializable

data class DesignerComponent(
    val name: String,
    val content: String? = null,
    val dependencies: List<DesignerComponent> = emptyList()
) : Serializable