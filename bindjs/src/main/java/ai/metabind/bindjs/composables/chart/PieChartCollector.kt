package ai.metabind.bindjs.composables.chart

import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.Component
import ai.metabind.bindjs.model.ForEachComponent
import ai.metabind.bindjs.model.GroupComponent
import ai.metabind.bindjs.model.ModifiedComponent
import ai.metabind.bindjs.model.chart.ChartAccessibilityOptions
import ai.metabind.bindjs.model.chart.ChartChannel
import ai.metabind.bindjs.model.chart.ChartDiagnostic
import ai.metabind.bindjs.model.chart.ChartForegroundStyle
import ai.metabind.bindjs.model.chart.ChartLegendOptions
import ai.metabind.bindjs.model.chart.ChartMarkAccessibility
import ai.metabind.bindjs.model.chart.ChartMarkComponent
import ai.metabind.bindjs.model.chart.ChartStyleOptions
import ai.metabind.bindjs.model.chart.PieChartComponent
import ai.metabind.bindjs.model.chart.PieChartModel
import ai.metabind.bindjs.model.chart.PieSelectionBinding
import ai.metabind.bindjs.model.chart.PieSliceMark
import ai.metabind.bindjs.model.chart.PieSliceMarkComponentProtocol
import ai.metabind.bindjs.model.chart.PieSliceStyle
import ai.metabind.bindjs.model.modifier.AccessibilityHintModifier
import ai.metabind.bindjs.model.modifier.AccessibilityLabelModifier
import ai.metabind.bindjs.model.modifier.AccessibilityValueModifier
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.CornerRadiusModifier
import ai.metabind.bindjs.model.modifier.ForegroundStyleModifier
import ai.metabind.bindjs.model.modifier.chart.AnnotationModifier
import ai.metabind.bindjs.model.modifier.chart.ChartForegroundStyleScaleModifier
import ai.metabind.bindjs.model.modifier.chart.ChartLegendModifier
import ai.metabind.bindjs.model.modifier.chart.ChartSelectionModifier
import ai.metabind.bindjs.model.modifier.chart.ChartXSelectionModifier
import ai.metabind.bindjs.model.modifier.chart.ChartYSelectionModifier
import ai.metabind.bindjs.model.modifier.chart.InterpolationMethodModifier
import ai.metabind.bindjs.model.modifier.chart.LineStyleModifier
import ai.metabind.bindjs.model.modifier.chart.SymbolModifier
import ai.metabind.bindjs.model.modifier.chart.SymbolSizeModifier

object PieChartCollector {
    fun collect(
        chart: PieChartComponent,
        modifiers: List<ComponentModifier<*>> = emptyList(),
    ): PieChartModel {
        val collector = Collector(chart.props.innerRadius?.coerceIn(0.0, 1.0))
        collector.collectChildren(chart.props.children, path = "PieChart.children")
        modifiers.forEach { collector.applyPieChartModifier(it, path = "PieChart") }
        return collector.build()
    }

    fun collectRoot(
        component: BaseComponent<*>,
        modifiers: List<ComponentModifier<*>> = emptyList(),
    ): PieChartModel? {
        val collectedModifiers = mutableListOf<ComponentModifier<*>>()
        val base = unwrapModified(component, collectedModifiers)
        val chart = base as? PieChartComponent ?: return null
        return collect(chart, collectedModifiers.asReversed() + modifiers)
    }

    fun isPieChartLevelModifier(modifier: ComponentModifier<*>): Boolean =
        modifier is ChartForegroundStyleScaleModifier ||
            modifier is ChartLegendModifier ||
            modifier is ChartSelectionModifier ||
            modifier is AccessibilityLabelModifier ||
            modifier is AccessibilityHintModifier

    private class Collector(
        private var innerRadius: Double?,
    ) {
        private val slices = mutableListOf<PieSliceMark>()
        private val diagnostics = mutableListOf<ChartDiagnostic>()
        private var legend = ChartLegendOptions()
        private var style = ChartStyleOptions()
        private var selection: PieSelectionBinding? = null
        private var accessibility = ChartAccessibilityOptions()

        fun collectChildren(children: List<BaseComponent<*>?>?, path: String) {
            children?.forEachIndexed { index, child ->
                if (child != null) collectChild(child, "$path[$index]")
            }
        }

        fun build(): PieChartModel =
            PieChartModel(
                slices = slices,
                innerRadius = innerRadius,
                legend = legend,
                style = style,
                selection = selection,
                accessibility = accessibility,
                diagnostics = diagnostics,
            )

        private fun collectChild(component: BaseComponent<*>, path: String) {
            when (component) {
                is GroupComponent -> collectChildren(component.props.children, "$path.Group")
                is ForEachComponent -> collectChildren(component.props.children, "$path.ForEach")
                is ModifiedComponent -> collectModifiedChild(component, path)
                is Component -> collectChildren(component.props.children, "$path.${component.type}")
                is PieSliceMarkComponentProtocol -> appendSlice(component.pieSlice(path), componentName(component), path)
                is ChartMarkComponent -> diagnostics.add(
                    ChartDiagnostic(
                        ChartDiagnostic.Severity.Error,
                        "PieChart children must be PieSliceMark, Group, or materialized ForEach; found Cartesian mark ${componentName(component)}",
                        path,
                    )
                )
                else -> diagnostics.add(
                    ChartDiagnostic(
                        ChartDiagnostic.Severity.Error,
                        "PieChart children must be PieSliceMark, Group, or materialized ForEach; found ${componentName(component)}",
                        path,
                    )
                )
            }
        }

        private fun collectModifiedChild(component: ModifiedComponent, path: String) {
            val modifiers = mutableListOf<ComponentModifier<*>>()
            val base = unwrapModified(component, modifiers)
            val slice = base as? PieSliceMarkComponentProtocol
            if (slice == null) {
                collectChild(base, path)
                return
            }

            var style = PieSliceStyle()
            var accessibility = ChartMarkAccessibility()
            modifiers.asReversed().forEach { modifier ->
                style = foldSliceStyle(modifier, style, sliceName = componentName(base), path = path)
                accessibility = foldSliceAccessibility(modifier, accessibility)
            }
            appendSlice(slice.pieSlice(path, style, accessibility), componentName(base), path)
        }

        private fun PieSliceMarkComponentProtocol.pieSlice(
            path: String,
            style: PieSliceStyle = PieSliceStyle(),
            accessibility: ChartMarkAccessibility = ChartMarkAccessibility(),
        ): PieSliceMark? {
            val value = sliceProps.value ?: return null
            return PieSliceMark(
                id = sliceProps.explicitId ?: path,
                value = value,
                label = sliceProps.label,
                style = style,
                accessibility = accessibility,
            )
        }

        private fun appendSlice(slice: PieSliceMark?, componentName: String, path: String) {
            if (slice == null) {
                diagnostics.add(
                    ChartDiagnostic(
                        ChartDiagnostic.Severity.Error,
                        "$componentName requires a literal numeric value.",
                        path,
                    )
                )
            } else {
                slices.add(slice)
            }
        }

        private fun foldSliceStyle(
            modifier: ComponentModifier<*>,
            current: PieSliceStyle,
            sliceName: String,
            path: String,
        ): PieSliceStyle =
            when (modifier) {
                is ForegroundStyleModifier -> current.copy(
                    foregroundStyle = parseForegroundStyle(modifier) ?: current.foregroundStyle
                )
                is CornerRadiusModifier -> current.copy(cornerRadius = modifier.props.rawValue.toDouble())
                is LineStyleModifier,
                is InterpolationMethodModifier,
                is SymbolModifier,
                is SymbolSizeModifier,
                is AnnotationModifier,
                -> {
                    diagnostics.add(
                        ChartDiagnostic(
                            ChartDiagnostic.Severity.Error,
                            "Cartesian-only mark modifier ${componentName(modifier)} cannot be attached to $sliceName",
                            path,
                        )
                    )
                    current
                }
                else -> {
                    if (isSliceAccessibilityModifier(modifier)) {
                        return current
                    }
                    if (isPieChartLevelModifier(modifier) || ChartCollector.isChartLevelModifier(modifier)) {
                        diagnostics.add(
                            ChartDiagnostic(
                                ChartDiagnostic.Severity.Error,
                                "Chart-level modifier ${componentName(modifier)} cannot be attached to $sliceName",
                                path,
                            )
                        )
                    } else {
                        diagnostics.add(
                            ChartDiagnostic(
                                ChartDiagnostic.Severity.Warning,
                                "Ignoring unsupported pie slice modifier ${componentName(modifier)}",
                                path,
                            )
                        )
                    }
                    current
                }
            }

        private fun foldSliceAccessibility(
            modifier: ComponentModifier<*>,
            current: ChartMarkAccessibility,
        ): ChartMarkAccessibility =
            when (modifier) {
                is AccessibilityLabelModifier -> current.copy(label = modifier.props.rawValue)
                is AccessibilityHintModifier -> current.copy(description = modifier.props.rawValue)
                is AccessibilityValueModifier -> current.copy(value = modifier.props.rawValue)
                else -> current
            }

        fun applyPieChartModifier(modifier: ComponentModifier<*>, path: String) {
            when (modifier) {
                is ChartForegroundStyleScaleModifier -> {
                    val scale = modifier.props.scale.orEmpty()
                        .mapValues { (_, value) -> value.asPieColorString() ?: value.toString() }
                    style = style.copy(foregroundStyleScale = scale)
                }
                is ChartLegendModifier -> {
                    val raw = modifier.props.rawValue as? String
                    legend = legend.copy(
                        hidden = modifier.props.hidden == true ||
                            modifier.props.visibility == "hidden" ||
                            modifier.props.position == "hidden" ||
                            raw == "hidden"
                    )
                }
                is ChartSelectionModifier -> selection = PieSelectionBinding.from(
                    modifier.props.value ?: modifier.props.rawValue,
                    modifier.props.onChangeId,
                )
                is ChartXSelectionModifier,
                is ChartYSelectionModifier,
                -> diagnostics.add(
                    ChartDiagnostic(
                        ChartDiagnostic.Severity.Error,
                        "${componentName(modifier)} is not supported on PieChart; use chartSelection instead",
                        path,
                    )
                )
                is AccessibilityLabelModifier -> accessibility =
                    accessibility.copy(label = modifier.props.rawValue)
                is AccessibilityHintModifier -> accessibility =
                    accessibility.copy(description = modifier.props.rawValue)
                else -> {
                    if (ChartCollector.isChartLevelModifier(modifier)) {
                        diagnostics.add(
                            ChartDiagnostic(
                                ChartDiagnostic.Severity.Error,
                                "Cartesian chart modifier ${componentName(modifier)} is not supported on PieChart",
                                path,
                            )
                        )
                    } else if (modifier !is AccessibilityValueModifier) {
                        diagnostics.add(
                            ChartDiagnostic(
                                ChartDiagnostic.Severity.Warning,
                                "Ignoring unsupported pie chart modifier ${componentName(modifier)}",
                                path,
                            )
                        )
                    }
                }
            }
        }

        private fun parseForegroundStyle(modifier: ForegroundStyleModifier): ChartForegroundStyle? {
            val props = modifier.props
            props.by?.let { by ->
                ChartChannel.from(by, defaultLabel = "series")?.let {
                    return ChartForegroundStyle.SeriesValue(it)
                }
            }
            props.color?.let { return ChartForegroundStyle.ColorValue(it) }

            val raw = props.rawValue
            if (raw is Map<*, *>) {
                raw["by"]?.let { by ->
                    ChartChannel.from(by, defaultLabel = "series")?.let {
                        return ChartForegroundStyle.SeriesValue(it)
                    }
                }
                raw["color"].asPieColorString()?.let { return ChartForegroundStyle.ColorValue(it) }
            }

            return raw.asPieColorString()?.let { ChartForegroundStyle.ColorValue(it) }
        }

        private fun isSliceAccessibilityModifier(modifier: ComponentModifier<*>): Boolean =
            modifier is AccessibilityLabelModifier ||
                modifier is AccessibilityHintModifier ||
                modifier is AccessibilityValueModifier
    }

    private fun unwrapModified(
        component: BaseComponent<*>,
        modifiers: MutableList<ComponentModifier<*>>,
    ): BaseComponent<*> {
        var current = component
        while (current is ModifiedComponent && current.props.content?.size == 1) {
            current.props.modifier?.let { modifiers.add(it) }
            val next = current.props.content.firstOrNull() ?: break
            current = next
        }
        return current
    }
}

private fun Any?.asPieColorString(): String? {
    return when (this) {
        is String -> this
        is Map<*, *> -> {
            val type = this["type"] as? String
            if (type == "Color") {
                val props = this["props"] as? Map<*, *>
                props?.get("rawValue") as? String ?: props?.get("value") as? String
            } else {
                this["color"] as? String
            }
        }
        else -> null
    }
}

private fun componentName(component: Any): String =
    component::class.simpleName ?: component.toString()
