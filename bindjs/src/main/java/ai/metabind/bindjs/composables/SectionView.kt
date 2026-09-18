package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.model.SectionComponent
import ai.metabind.bindjs.model.SpacerComponent
import ai.metabind.bindjs.model.expandingForEach
import ai.metabind.bindjs.model.modifier.ComponentModifier

/**
 * The gap a section leaves between its header, its rows and its footer. Also used by the
 * pinned-header path in [ScrollView], which lifts the header out into its own lazy item and
 * has to re-create the gap the arrangement here would have left.
 */
internal val SectionContentSpacing = 10.dp

@Composable
fun SectionView(
    jsRuntime: JsRuntime,
    component: SectionComponent,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    Column(
        modifier = modifiers.buildModifier(onUiEvent),
        verticalArrangement = Arrangement.spacedBy(space = SectionContentSpacing)
    ) {
        component.props.header?.let { child ->
            BindJSView(
                jsRuntime = jsRuntime,
                component = child,
                version = version,
                onUiEvent = onUiEvent,
                modifiers = modifiers
            )
        }
        component.props.children.expandingForEach()?.forEach { child ->
            if (child is SpacerComponent) {
                Spacer(
                    modifier = Modifier.then(
                        if (child.props.minLength != null) Modifier.height(
                            child.props.minLength.dp
                        ) else Modifier.weight(1.0f)
                    )
                )
            } else {
                child?.let {
                    BindJSView(
                        jsRuntime = jsRuntime,
                        component = child,
                        version = version,
                        onUiEvent = onUiEvent
                    )
                }
            }
        }
        component.props.footer?.let { child ->
            BindJSView(
                jsRuntime = jsRuntime,
                component = child,
                version = version,
                onUiEvent = onUiEvent,
                modifiers = modifiers
            )
        }
    }
}
