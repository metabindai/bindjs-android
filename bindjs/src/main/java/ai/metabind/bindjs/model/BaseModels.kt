package ai.metabind.bindjs.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import ai.metabind.bindjs.model.modifier.FrameModifier
import java.io.Serializable

abstract class BaseComponent<T : Props>(open val props: T) : Serializable {
    override fun toString(): String {
        return "${this::class.simpleName}(props=$props)"
    }

    fun calculateMaxWidth(): Float? {
        (this as? ModifiedComponent)?.let { modifiedComponent ->
            (this.props.modifier as? FrameModifier)?.let { frameModifier ->
                return frameModifier.props.width ?: frameModifier.props.maxWidth
            }
            return modifiedComponent.props.content?.firstOrNull()?.calculateMaxWidth()
        }
        // Recurse into layout containers (VStack, HStack, ZStack) to find
        // descendants that request infinite width, so the parent Row/Column
        // can give this child flexible weight instead of equal distribution.
        this.props.children?.forEach { child ->
            val childMax = child?.calculateMaxWidth()
            if (childMax == Float.POSITIVE_INFINITY) return Float.POSITIVE_INFINITY
        }
        return null
    }
}

interface BrushComponent {
    @Composable
    fun createBrush(): Brush
}

class Component(
    val type: String,
    props: Props,
) : BaseComponent<Props>(props) {
    override fun toString(): String {
        return "Component(type=$type)"
    }
}

open class Props(
    val name: String? = null,
    val children: List<BaseComponent<*>?>?,
) : Serializable {
    override fun toString(): String {
        return "Props(children=$children)"
    }
}
