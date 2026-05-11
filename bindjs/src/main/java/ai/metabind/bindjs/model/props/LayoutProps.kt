package ai.metabind.bindjs.model.props

import androidx.compose.ui.Alignment
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.Props
import ai.metabind.bindjs.model.ext.toAlignment

class LayoutProps(
    val spacing: Float?,
    val alignment: String?,
    children: List<BaseComponent<*>>?,
) : Props(children = children) {
    override fun toString(): String {
        return "LayoutProps(spacing=$spacing, alignment=$alignment)"
    }
}

fun LayoutProps.horizontalAlignment(): Alignment.Horizontal {
    return when (alignment) {
        "leading" -> Alignment.Start
        "trailing" -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
}

fun LayoutProps.verticalAlignment(): Alignment.Vertical {
    return when (alignment) {
        "leading" -> Alignment.Top
        "top" -> Alignment.Top
        "trailing" -> Alignment.Bottom
        "bottom" -> Alignment.Bottom
        "center" -> Alignment.CenterVertically
        else -> Alignment.CenterVertically
    }
}

fun LayoutProps.uiAlignment(): Alignment {
    return alignment.toAlignment()
}
