package ai.metabind.bindjs.model.modifier

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.composables.ext.materialBlur
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.BrushComponent
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.Component
import ai.metabind.bindjs.model.GeometryReaderComponent
import ai.metabind.bindjs.model.ImageComponent
import ai.metabind.bindjs.model.ModifiedComponent

class BackgroundModifier(
    props: BackgroundProps,
) : ComponentModifier<BackgroundProps>(props) {
    companion object {
        private const val TAG = "BackgroundModifier"
    }

    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit,
    ): Modifier {
        return when (val component = props.content) {
            is ColorComponent -> {
                if (component.isMaterial()) {
                    Modifier.materialBlur()
                } else {
                    Modifier.background(Color(component.color))
                }
            }

            is BrushComponent -> {
                Modifier.background(brush = component.createBrush())
            }

            is ImageComponent -> {
                // Rendered as a composable in BackgroundViews
                Modifier
            }

            is ModifiedComponent -> {
                // Rendered as a composable in BackgroundViews
                Modifier
            }

            is GeometryReaderComponent -> {
                // Rendered as a composable in BackgroundViews
                Modifier
            }

            is Component -> {
                val colorComponent = component.props.children?.firstOrNull {
                    it is ColorComponent
                } as? ColorComponent
                colorComponent?.let {
                    Modifier.background(Color(it.color))
                } ?: Modifier
            }

            else -> {
                if (component != null) {
                    Log.d(
                        TAG,
                        "ComponentView: Unknown background modifier: ${component::class.java}"
                    )
                }
                Modifier
            }
        }
    }
}

class BackgroundProps(
    @SerializedName(value = "content", alternate = ["value"])
    val content: BaseComponent<*>?,
    children: List<BaseComponent<*>>? = null,
) : ComponentModifierProps(children)
