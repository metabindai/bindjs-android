package com.yapstudios.bindjs.model.modifier.chart

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.gson.annotations.SerializedName
import com.yapstudios.bindjs.composables.UiEvent
import com.yapstudios.bindjs.model.BaseComponent
import com.yapstudios.bindjs.model.modifier.ComponentModifier
import com.yapstudios.bindjs.model.modifier.ComponentModifierProps

abstract class ChartSemanticModifier<T : ComponentModifierProps>(
    props: T,
) : ComponentModifier<T>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit,
    ): Modifier = Modifier
}

class ChartXAxisModifier(props: ChartAxisModifierProps) :
    ChartSemanticModifier<ChartAxisModifierProps>(props)

class ChartYAxisModifier(props: ChartAxisModifierProps) :
    ChartSemanticModifier<ChartAxisModifierProps>(props)

class ChartAxisModifierProps(
    children: List<BaseComponent<*>?>?,
    @SerializedName(value = "rawValue", alternate = ["value"])
    val rawValue: Any? = null,
    val hidden: Boolean? = null,
    val visibility: String? = null,
    val values: Any? = null,
    val position: String? = null,
    val label: String? = null,
    val labelsHidden: Boolean? = null,
    val ticksHidden: Boolean? = null,
    val gridHidden: Boolean? = null,
    val formatter: Any? = null,
) : ComponentModifierProps(children)

class ChartXScaleModifier(props: ChartScaleModifierProps) :
    ChartSemanticModifier<ChartScaleModifierProps>(props)

class ChartYScaleModifier(props: ChartScaleModifierProps) :
    ChartSemanticModifier<ChartScaleModifierProps>(props)

class ChartScaleModifierProps(
    children: List<BaseComponent<*>?>?,
    val type: String? = null,
    val domain: List<Any?>? = null,
) : ComponentModifierProps(children)

class ChartForegroundStyleScaleModifier(props: ChartForegroundStyleScaleProps) :
    ChartSemanticModifier<ChartForegroundStyleScaleProps>(props)

class ChartForegroundStyleScaleProps(
    children: List<BaseComponent<*>?>?,
    val scale: Map<String, Any?>? = null,
    @SerializedName(value = "rawValue", alternate = ["value"])
    val rawValue: Any? = null,
) : ComponentModifierProps(children)

class ChartSymbolScaleModifier(props: ChartSymbolScaleProps) :
    ChartSemanticModifier<ChartSymbolScaleProps>(props)

class ChartSymbolScaleProps(
    children: List<BaseComponent<*>?>?,
    val scale: Map<String, String>? = null,
    @SerializedName(value = "rawValue", alternate = ["value"])
    val rawValue: Any? = null,
) : ComponentModifierProps(children)

class ChartLegendModifier(props: ChartLegendModifierProps) :
    ChartSemanticModifier<ChartLegendModifierProps>(props)

class ChartLegendModifierProps(
    children: List<BaseComponent<*>?>?,
    @SerializedName(value = "rawValue", alternate = ["value"])
    val rawValue: Any? = null,
    val hidden: Boolean? = null,
    val visibility: String? = null,
    val position: String? = null,
) : ComponentModifierProps(children)

class ChartXAxisLabelModifier(props: ChartAxisLabelModifierProps) :
    ChartSemanticModifier<ChartAxisLabelModifierProps>(props)

class ChartYAxisLabelModifier(props: ChartAxisLabelModifierProps) :
    ChartSemanticModifier<ChartAxisLabelModifierProps>(props)

class ChartAxisLabelModifierProps(
    children: List<BaseComponent<*>?>?,
    @SerializedName(value = "rawValue", alternate = ["value"])
    val rawValue: Any? = null,
    val label: String? = null,
) : ComponentModifierProps(children)

class ChartXSelectionModifier(props: ChartSelectionModifierProps) :
    ChartSemanticModifier<ChartSelectionModifierProps>(props)

class ChartYSelectionModifier(props: ChartSelectionModifierProps) :
    ChartSemanticModifier<ChartSelectionModifierProps>(props)

class ChartSelectionModifier(props: ChartSelectionModifierProps) :
    ChartSemanticModifier<ChartSelectionModifierProps>(props)

class ChartSelectionModifierProps(
    children: List<BaseComponent<*>?>?,
    @SerializedName(value = "rawValue")
    val rawValue: Any? = null,
    val value: Any? = null,
    val onChangeId: String? = null,
) : ComponentModifierProps(children)

class LineStyleModifier(props: LineStyleModifierProps) :
    ChartSemanticModifier<LineStyleModifierProps>(props)

class LineStyleModifierProps(
    children: List<BaseComponent<*>?>?,
    val width: Double? = null,
    val dash: List<Double>? = null,
) : ComponentModifierProps(children)

class InterpolationMethodModifier(props: InterpolationMethodModifierProps) :
    ChartSemanticModifier<InterpolationMethodModifierProps>(props)

class InterpolationMethodModifierProps(
    children: List<BaseComponent<*>?>?,
    @SerializedName(value = "rawValue", alternate = ["value"])
    val rawValue: String? = null,
    val method: String? = null,
) : ComponentModifierProps(children)

class SymbolModifier(props: SymbolModifierProps) :
    ChartSemanticModifier<SymbolModifierProps>(props)

class SymbolModifierProps(
    children: List<BaseComponent<*>?>?,
    @SerializedName(value = "rawValue", alternate = ["value"])
    val rawValue: String? = null,
    val symbol: String? = null,
) : ComponentModifierProps(children)

class SymbolSizeModifier(props: SymbolSizeModifierProps) :
    ChartSemanticModifier<SymbolSizeModifierProps>(props)

class SymbolSizeModifierProps(
    children: List<BaseComponent<*>?>?,
    @SerializedName(value = "rawValue", alternate = ["value"])
    val rawValue: Double? = null,
    val size: Double? = null,
) : ComponentModifierProps(children)

class AnnotationModifier(props: AnnotationModifierProps) :
    ChartSemanticModifier<AnnotationModifierProps>(props)

class AnnotationModifierProps(
    children: List<BaseComponent<*>?>?,
    @SerializedName(value = "rawValue", alternate = ["value"])
    val rawValue: Any? = null,
    val text: String? = null,
    val position: String? = null,
) : ComponentModifierProps(children)
