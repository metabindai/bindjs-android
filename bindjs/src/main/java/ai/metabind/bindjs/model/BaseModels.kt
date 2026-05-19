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
        // A ComponentCall is a transparent slot — its single child is the
        // rendered body, so propagate that child's width (finite or infinite).
        // Without this, a Row of ComponentCalls with explicit per-card widths
        // is misclassified as "all flexible" and weight-distributed.
        if (this is Component) {
            return props.children?.firstOrNull()?.calculateMaxWidth()
        }
        // Layout containers (VStack, HStack, ZStack) only propagate infinity:
        // a finite width on one child doesn't describe the container's width.
        this.props.children?.forEach { child ->
            val childMax = child?.calculateMaxWidth()
            if (childMax == Float.POSITIVE_INFINITY) return Float.POSITIVE_INFINITY
        }
        return null
    }

    fun calculateMaxHeight(): Float? {
        (this as? ModifiedComponent)?.let { modifiedComponent ->
            (this.props.modifier as? FrameModifier)?.let { frameModifier ->
                // `minHeight` is a known floor — for weight-distribution
                // siblings, that's enough to treat this child as having a
                // fixed contribution rather than purely flexible. Without
                // this, a `frame(minHeight: 160)` text section next to a
                // greedy image leaves both classified as flexible, and the
                // image consumes the whole column.
                return frameModifier.props.height
                    ?: frameModifier.props.maxHeight
                    ?: frameModifier.props.minHeight
            }
            return modifiedComponent.props.content?.firstOrNull()?.calculateMaxHeight()
        }
        if (this is Component) {
            return props.children?.firstOrNull()?.calculateMaxHeight()
        }
        this.props.children?.forEach { child ->
            val childMax = child?.calculateMaxHeight()
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
