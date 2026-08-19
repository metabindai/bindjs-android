package ai.metabind.bindjs.composables.chart

import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.Component
import ai.metabind.bindjs.model.ForEachComponent
import ai.metabind.bindjs.model.GroupComponent
import ai.metabind.bindjs.model.ModifiedComponent
import ai.metabind.bindjs.model.chart.ChartAccessibilityOptions
import ai.metabind.bindjs.model.chart.ChartAnnotation
import ai.metabind.bindjs.model.chart.ChartAxisOption
import ai.metabind.bindjs.model.chart.ChartAxisOptions
import ai.metabind.bindjs.model.chart.ChartAxisValues
import ai.metabind.bindjs.model.chart.ChartChannel
import ai.metabind.bindjs.model.chart.ChartComponent
import ai.metabind.bindjs.model.chart.ChartDiagnostic
import ai.metabind.bindjs.model.chart.ChartForegroundStyle
import ai.metabind.bindjs.model.chart.ChartInterpolation
import ai.metabind.bindjs.model.chart.ChartLegendOptions
import ai.metabind.bindjs.model.chart.ChartLineStyle
import ai.metabind.bindjs.model.chart.ChartMark
import ai.metabind.bindjs.model.chart.ChartMarkAccessibility
import ai.metabind.bindjs.model.chart.ChartMarkChannels
import ai.metabind.bindjs.model.chart.ChartMarkComponent
import ai.metabind.bindjs.model.chart.ChartMarkStyle
import ai.metabind.bindjs.model.chart.ChartModel
import ai.metabind.bindjs.model.chart.ChartScaleOption
import ai.metabind.bindjs.model.chart.ChartScaleOptions
import ai.metabind.bindjs.model.chart.ChartSelectionBinding
import ai.metabind.bindjs.model.chart.ChartSelectionOptions
import ai.metabind.bindjs.model.chart.ChartStacking
import ai.metabind.bindjs.model.chart.ChartStyleOptions
import ai.metabind.bindjs.model.chart.ChartSymbolName
import ai.metabind.bindjs.model.chart.ChartValue
import ai.metabind.bindjs.model.chart.ChartValueFormatter
import ai.metabind.bindjs.model.modifier.AccessibilityHintModifier
import ai.metabind.bindjs.model.modifier.AccessibilityLabelModifier
import ai.metabind.bindjs.model.modifier.AccessibilityValueModifier
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.CornerRadiusModifier
import ai.metabind.bindjs.model.modifier.ForegroundStyleModifier
import ai.metabind.bindjs.model.modifier.chart.AnnotationModifier
import ai.metabind.bindjs.model.modifier.chart.ChartAxisLabelModifierProps
import ai.metabind.bindjs.model.modifier.chart.ChartAxisModifierProps
import ai.metabind.bindjs.model.modifier.chart.ChartForegroundStyleScaleModifier
import ai.metabind.bindjs.model.modifier.chart.ChartLegendModifier
import ai.metabind.bindjs.model.modifier.chart.ChartScaleModifierProps
import ai.metabind.bindjs.model.modifier.chart.ChartSelectionModifier
import ai.metabind.bindjs.model.modifier.chart.ChartSymbolScaleModifier
import ai.metabind.bindjs.model.modifier.chart.ChartXAxisLabelModifier
import ai.metabind.bindjs.model.modifier.chart.ChartXAxisModifier
import ai.metabind.bindjs.model.modifier.chart.ChartXScaleModifier
import ai.metabind.bindjs.model.modifier.chart.ChartXSelectionModifier
import ai.metabind.bindjs.model.modifier.chart.ChartYAxisLabelModifier
import ai.metabind.bindjs.model.modifier.chart.ChartYAxisModifier
import ai.metabind.bindjs.model.modifier.chart.ChartYScaleModifier
import ai.metabind.bindjs.model.modifier.chart.ChartYSelectionModifier
import ai.metabind.bindjs.model.modifier.chart.InterpolationMethodModifier
import ai.metabind.bindjs.model.modifier.chart.LineStyleModifier
import ai.metabind.bindjs.model.modifier.chart.SymbolModifier
import ai.metabind.bindjs.model.modifier.chart.SymbolSizeModifier

object ChartCollector {
    fun collect(
        chart: ChartComponent,
        modifiers: List<ComponentModifier<*>> = emptyList(),
    ): ChartModel {
        val collector = Collector()
        collector.collectChildren(chart.props.children, path = "Chart.children")
        modifiers.forEach { collector.applyChartModifier(it, path = "Chart") }
        return collector.build()
    }

    fun isChartLevelModifier(modifier: ComponentModifier<*>): Boolean =
        modifier is ChartXAxisModifier ||
            modifier is ChartYAxisModifier ||
            modifier is ChartXScaleModifier ||
            modifier is ChartYScaleModifier ||
            modifier is ChartForegroundStyleScaleModifier ||
            modifier is ChartSymbolScaleModifier ||
            modifier is ChartSelectionModifier ||
            modifier is ChartXSelectionModifier ||
            modifier is ChartYSelectionModifier ||
            modifier is ChartLegendModifier ||
            modifier is ChartXAxisLabelModifier ||
            modifier is ChartYAxisLabelModifier

    private class Collector {
        private val marks = mutableListOf<ChartMark>()
        private val diagnostics = mutableListOf<ChartDiagnostic>()
        private var axes = ChartAxisOptions()
        private var scales = ChartScaleOptions()
        private var legend = ChartLegendOptions()
        private var style = ChartStyleOptions()
        private var selection: ChartSelectionOptions? = null
        private var accessibility = ChartAccessibilityOptions()

        fun collectChildren(children: List<BaseComponent<*>?>?, path: String) {
            children?.forEachIndexed { index, child ->
                if (child != null) collectChild(child, "$path[$index]")
            }
        }

        fun build(): ChartModel {
            val hasRectangleMarks = marks.any { it.kind == ai.metabind.bindjs.model.chart.ChartMarkKind.Rectangle }
            val hasNonRectangleMarks = marks.any { it.kind != ai.metabind.bindjs.model.chart.ChartMarkKind.Rectangle }
            if (hasRectangleMarks && hasNonRectangleMarks) {
                diagnostics.add(
                    ChartDiagnostic(
                        ChartDiagnostic.Severity.Error,
                        "RectangleMark cannot be mixed with other Cartesian marks on Android; render rectangle ranges in a dedicated Chart.",
                        "Chart",
                    )
                )
            }

            return ChartModel(
                marks = marks,
                axes = axes,
                scales = scales,
                legend = legend,
                style = style,
                selection = selection,
                accessibility = accessibility,
                diagnostics = diagnostics,
            )
        }

        private fun collectChild(component: BaseComponent<*>, path: String) {
            when (component) {
                is GroupComponent -> collectChildren(component.props.children, "$path.Group")
                is ForEachComponent -> collectChildren(component.props.children, "$path.ForEach")
                is ModifiedComponent -> collectModifiedChild(component, path)
                is Component -> collectChildren(component.props.children, "$path.${component.type}")
                is ChartMarkComponent -> appendMark(component.chartMark(path), path)
                else -> diagnostics.add(
                    ChartDiagnostic(
                        ChartDiagnostic.Severity.Error,
                        "Chart children must be chart marks, Group, or materialized ForEach; found ${componentName(component)}",
                        path,
                    )
                )
            }
        }

        private fun collectModifiedChild(component: ModifiedComponent, path: String) {
            val modifiers = mutableListOf<ComponentModifier<*>>()
            val base = unwrapModified(component, modifiers)
            val mark = base as? ChartMarkComponent
            if (mark == null) {
                collectChild(base, path)
                return
            }

            var style = mark.baseStyle()
            var accessibility = ChartMarkAccessibility()
            modifiers.asReversed().forEach { modifier ->
                style = foldMarkStyle(modifier, style, markName = componentName(base), path = path)
                accessibility = foldMarkAccessibility(modifier, accessibility)
            }
            appendMark(mark.chartMark(path, style, accessibility), path)
        }

        private fun appendMark(mark: ChartMark, path: String) {
            when (mark.kind) {
                ai.metabind.bindjs.model.chart.ChartMarkKind.Rule -> {
                    val hasX = mark.channels.x != null
                    val hasY = mark.channels.y != null
                    if (hasX == hasY) {
                        diagnostics.add(
                            ChartDiagnostic(
                                ChartDiagnostic.Severity.Error,
                                "RuleMark requires exactly one of x or y.",
                                path,
                            )
                        )
                    } else {
                        marks.add(mark)
                    }
                }
                ai.metabind.bindjs.model.chart.ChartMarkKind.Rectangle -> {
                    if (mark.channels.x == null || mark.channels.y == null) {
                        diagnostics.add(
                            ChartDiagnostic(
                                ChartDiagnostic.Severity.Error,
                                "RectangleMark requires x and y channels.",
                                path,
                            )
                        )
                    } else {
                        marks.add(mark)
                    }
                }
                else -> marks.add(mark)
            }
        }

        private fun ChartMarkComponent.chartMark(
            path: String,
            style: ChartMarkStyle = baseStyle(),
            accessibility: ChartMarkAccessibility = ChartMarkAccessibility(),
        ): ChartMark {
            val props = markProps
            return ChartMark(
                id = props.explicitId ?: path,
                kind = markKind,
                channels = ChartMarkChannels(
                    x = ChartChannel.from(props.x, defaultLabel = "x"),
                    y = ChartChannel.from(props.y, defaultLabel = "y"),
                    x2 = ChartChannel.from(props.x2, defaultLabel = "x2"),
                    y2 = ChartChannel.from(props.y2, defaultLabel = "y2"),
                ),
                style = style,
                accessibility = accessibility,
            )
        }

        private fun ChartMarkComponent.baseStyle(): ChartMarkStyle =
            ChartMarkStyle(stacking = ChartStacking.from(markProps.stacking))

        private fun foldMarkStyle(
            modifier: ComponentModifier<*>,
            current: ChartMarkStyle,
            markName: String,
            path: String,
        ): ChartMarkStyle {
            return when (modifier) {
                is ForegroundStyleModifier -> current.copy(
                    foregroundStyle = parseForegroundStyle(modifier) ?: current.foregroundStyle
                )

                is LineStyleModifier -> current.copy(
                    lineStyle = ChartLineStyle(
                        width = modifier.props.width,
                        dash = modifier.props.dash,
                    )
                )

                is InterpolationMethodModifier -> {
                    val raw = modifier.props.method ?: modifier.props.rawValue
                    val interpolation = ChartInterpolation.from(raw)
                    interpolation?.androidApproximationMessage(raw)?.let { message ->
                        diagnostics.add(
                            ChartDiagnostic(
                                ChartDiagnostic.Severity.Warning,
                                message,
                                path,
                            )
                        )
                    }
                    current.copy(interpolationMethod = interpolation ?: current.interpolationMethod)
                }

                is CornerRadiusModifier -> current.copy(cornerRadius = modifier.props.rawValue.toDouble())
                is SymbolModifier -> {
                    val raw = modifier.props.symbol ?: modifier.props.rawValue
                    val symbol = ChartSymbolName.from(raw)
                    if (symbol == null && raw != null) {
                        diagnostics.add(
                            ChartDiagnostic(
                                ChartDiagnostic.Severity.Error,
                                "Unknown chart symbol '$raw'. Expected circle, square, diamond, triangle, plus, or cross.",
                                path,
                            )
                        )
                    }
                    current.copy(symbol = symbol ?: current.symbol)
                }
                is SymbolSizeModifier -> current.copy(
                    symbolSize = modifier.props.size ?: modifier.props.rawValue ?: current.symbolSize
                )
                is AnnotationModifier -> {
                    val annotation = annotation(modifier.props.rawValue, modifier.props.text, modifier.props.position)
                    if (annotation == null) {
                        diagnostics.add(
                            ChartDiagnostic(
                                ChartDiagnostic.Severity.Error,
                                "chart annotation requires text.",
                                path,
                            )
                        )
                    }
                    current.copy(annotation = annotation ?: current.annotation)
                }
                else -> {
                    if (isChartLevelModifier(modifier)) {
                        diagnostics.add(
                            ChartDiagnostic(
                                ChartDiagnostic.Severity.Error,
                                "Chart-level modifier ${componentName(modifier)} cannot be attached to $markName",
                                path,
                            )
                        )
                    } else if (!isMarkAccessibilityModifier(modifier)) {
                        diagnostics.add(
                            ChartDiagnostic(
                                ChartDiagnostic.Severity.Warning,
                                "Ignoring unsupported chart mark modifier ${componentName(modifier)}",
                                path,
                            )
                        )
                    }
                    current
                }
            }
        }

        private fun foldMarkAccessibility(
            modifier: ComponentModifier<*>,
            current: ChartMarkAccessibility,
        ): ChartMarkAccessibility =
            when (modifier) {
                is AccessibilityLabelModifier -> current.copy(label = modifier.props.rawValue)
                is AccessibilityHintModifier -> current.copy(description = modifier.props.rawValue)
                is AccessibilityValueModifier -> current.copy(value = modifier.props.rawValue)
                else -> current
            }

        fun applyChartModifier(modifier: ComponentModifier<*>, path: String) {
            when (modifier) {
                is ChartXAxisModifier -> axes = axes.copy(x = axisOption(modifier.props))
                is ChartYAxisModifier -> axes = axes.copy(y = axisOption(modifier.props))
                is ChartXScaleModifier -> {
                    val scale = scaleOption(modifier.props)
                    validateScale(scale, axis = "x", path = path)
                    scales = scales.copy(x = scale)
                }
                is ChartYScaleModifier -> {
                    val scale = scaleOption(modifier.props)
                    validateScale(scale, axis = "y", path = path)
                    scales = scales.copy(y = scale)
                }
                is ChartForegroundStyleScaleModifier -> {
                    val scale = modifier.props.scale.orEmpty()
                        .mapValues { (_, value) -> value.asColorString() ?: value.toString() }
                    style = style.copy(foregroundStyleScale = scale)
                }
                is ChartSymbolScaleModifier -> {
                    style = style.copy(symbolScale = symbolScale(modifier.props.scale, modifier.props.rawValue))
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
                is ChartXAxisLabelModifier -> axes = axes.copy(
                    x = (axes.x ?: ChartAxisOption()).copy(label = axisLabel(modifier.props))
                )
                is ChartYAxisLabelModifier -> axes = axes.copy(
                    y = (axes.y ?: ChartAxisOption()).copy(label = axisLabel(modifier.props))
                )
                is ChartXSelectionModifier -> selection = (selection ?: ChartSelectionOptions()).copy(
                    x = ChartSelectionBinding.from(
                        modifier.props.value ?: modifier.props.rawValue,
                        modifier.props.onChangeId,
                    )
                )
                is ChartYSelectionModifier -> selection = (selection ?: ChartSelectionOptions()).copy(
                    y = ChartSelectionBinding.from(
                        modifier.props.value ?: modifier.props.rawValue,
                        modifier.props.onChangeId,
                    )
                )
                is ChartSelectionModifier -> diagnostics.add(
                    ChartDiagnostic(
                        ChartDiagnostic.Severity.Error,
                        "chartSelection is not supported on Chart; use chartXSelection or chartYSelection instead",
                        path,
                    )
                )
                is AccessibilityLabelModifier -> accessibility =
                    accessibility.copy(label = modifier.props.rawValue)
                is AccessibilityHintModifier -> accessibility =
                    accessibility.copy(description = modifier.props.rawValue)
                else -> {
                    if (isChartLevelModifier(modifier)) return
                    if (modifier !is AccessibilityValueModifier) {
                        diagnostics.add(
                            ChartDiagnostic(
                                ChartDiagnostic.Severity.Warning,
                                "Ignoring unsupported chart modifier ${componentName(modifier)}",
                                path,
                            )
                        )
                    }
                }
            }
        }

        private fun axisOption(props: ChartAxisModifierProps): ChartAxisOption {
            val raw = props.rawValue as? String
            return ChartAxisOption(
                hidden = props.hidden == true || props.visibility == "hidden" || raw == "hidden",
                values = ChartAxisValues.from(props.values ?: raw),
                position = props.position,
                label = props.label,
                labelsHidden = props.labelsHidden == true,
                ticksHidden = props.ticksHidden == true,
                gridHidden = props.gridHidden == true,
                formatter = ChartValueFormatter.from(props.formatter),
            )
        }

        private fun scaleOption(props: ChartScaleModifierProps): ChartScaleOption =
            ChartScaleOption(
                type = props.type,
                domain = props.domain?.mapNotNull { ChartValue.from(it) }?.filter { it.isAxisValue },
            )

        private fun validateScale(scale: ChartScaleOption, axis: String, path: String) {
            val domain = scale.domain ?: return
            if (domain.size == 2) {
                val lower = domain[0].numberOrNull()
                val upper = domain[1].numberOrNull()
                if (lower != null && upper != null && lower > upper) {
                    diagnostics.add(
                        ChartDiagnostic(
                            ChartDiagnostic.Severity.Error,
                            "Invalid chart $axis-scale domain: lower bound must be less than or equal to upper bound",
                            path,
                        )
                    )
                }
            }
        }

        private fun axisLabel(props: ChartAxisLabelModifierProps): String =
            props.label ?: props.rawValue?.toString() ?: ""

        private fun symbolScale(scale: Map<String, String>?, rawValue: Any?): Map<String, ChartSymbolName> {
            val rawMap = scale ?: (rawValue as? Map<*, *>)?.mapNotNull { (key, value) ->
                val name = key as? String ?: return@mapNotNull null
                name to value.toString()
            }?.toMap()

            return rawMap.orEmpty().mapNotNull { (key, value) ->
                val symbol = ChartSymbolName.from(value)
                if (symbol == null) {
                    diagnostics.add(
                        ChartDiagnostic(
                            ChartDiagnostic.Severity.Error,
                            "Unknown chart symbol '$value'. Expected circle, square, diamond, triangle, plus, or cross.",
                            "Chart",
                        )
                    )
                    null
                } else {
                    key to symbol
                }
            }.toMap()
        }

        private fun annotation(rawValue: Any?, text: String?, position: String?): ChartAnnotation? {
            val rawMap = rawValue as? Map<*, *>
            val annotationText = text
                ?: (rawMap?.get("text") as? String)
                ?: (rawValue as? String)
                ?: return null
            val rawPosition = position ?: (rawMap?.get("position") as? String)
            return ChartAnnotation(
                text = annotationText,
                position = ChartAnnotation.Position.from(rawPosition),
            )
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
                raw["color"]?.asColorString()?.let { return ChartForegroundStyle.ColorValue(it) }
            }

            return raw.asColorString()?.let { ChartForegroundStyle.ColorValue(it) }
        }

        private fun isMarkAccessibilityModifier(modifier: ComponentModifier<*>): Boolean =
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

private fun Any?.asColorString(): String? {
    return when (this) {
        is String -> this
        is Map<*, *> -> {
            val type = this["type"] as? String
            if (type == "Color") {
                val props = this["props"] as? Map<*, *>
                props?.get("rawValue") as? String
                    ?: props?.get("value") as? String
                    ?: props?.rgbaHexOrNull()
            } else {
                this["color"] as? String
            }
        }
        else -> null
    }
}

// A colour built from components (`Color({ r, g, b, a })`) arrives as those channels rather
// than a name or a hex string. The chart pipeline keys a series' colour off a *string*, so
// pack the channels into `#RRGGBBAA` — the form `ChartView.chartColor` already resolves.
// Without this the mark read as having no `foregroundStyle` at all and fell back to the
// automatic palette, which painted an explicitly green series the palette's first colour
// (blue) instead.
private fun Map<*, *>.rgbaHexOrNull(): String? {
    val r = (this["r"] as? Number)?.toDouble() ?: return null
    val g = (this["g"] as? Number)?.toDouble() ?: return null
    val b = (this["b"] as? Number)?.toDouble() ?: return null
    val a = (this["a"] as? Number)?.toDouble() ?: 1.0
    fun channel(value: Double): Int = Math.round(value).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X%02X".format(channel(r), channel(g), channel(b), channel(a * 255.0))
}

private fun componentName(component: Any): String =
    component::class.simpleName ?: component.toString()

private fun ChartInterpolation.androidApproximationMessage(raw: String?): String? =
    when (this) {
        ChartInterpolation.StepStart,
        ChartInterpolation.StepCenter,
        ChartInterpolation.StepEnd,
        -> "Android renderer does not support '${raw ?: name}' interpolation exactly; rendering it as linear."
        ChartInterpolation.Monotone,
        ChartInterpolation.Cardinal,
        -> "Android renderer approximates '${raw ?: name}' interpolation with cubic interpolation."
        ChartInterpolation.Linear,
        ChartInterpolation.CatmullRom,
        -> null
    }
