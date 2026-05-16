package ai.metabind.bindjs.model.chart

data class ChartModel(
    val marks: List<ChartMark> = emptyList(),
    val axes: ChartAxisOptions = ChartAxisOptions(),
    val scales: ChartScaleOptions = ChartScaleOptions(),
    val legend: ChartLegendOptions = ChartLegendOptions(),
    val style: ChartStyleOptions = ChartStyleOptions(),
    val selection: ChartSelectionOptions? = null,
    val accessibility: ChartAccessibilityOptions = ChartAccessibilityOptions(),
    val diagnostics: List<ChartDiagnostic> = emptyList(),
)

data class PieChartModel(
    val slices: List<PieSliceMark> = emptyList(),
    val innerRadius: Double? = null,
    val legend: ChartLegendOptions = ChartLegendOptions(),
    val style: ChartStyleOptions = ChartStyleOptions(),
    val selection: PieSelectionBinding? = null,
    val accessibility: ChartAccessibilityOptions = ChartAccessibilityOptions(),
    val diagnostics: List<ChartDiagnostic> = emptyList(),
)

data class PieSliceMark(
    val id: String,
    val value: Double,
    val label: String? = null,
    val style: PieSliceStyle = PieSliceStyle(),
    val accessibility: ChartMarkAccessibility = ChartMarkAccessibility(),
)

data class PieSliceStyle(
    val foregroundStyle: ChartForegroundStyle? = null,
    val cornerRadius: Double? = null,
)

data class PieSelectionBinding(
    val value: String? = null,
    val onChangeId: String? = null,
) {
    companion object {
        fun from(rawValue: Any?, onChangeId: String?): PieSelectionBinding =
            PieSelectionBinding(
                value = rawValue as? String,
                onChangeId = onChangeId,
            )
    }
}

data class ChartMark(
    val id: String,
    val kind: ChartMarkKind,
    val channels: ChartMarkChannels,
    val style: ChartMarkStyle = ChartMarkStyle(),
    val accessibility: ChartMarkAccessibility = ChartMarkAccessibility(),
)

data class ChartMarkChannels(
    val x: ChartChannel? = null,
    val y: ChartChannel? = null,
    val x2: ChartChannel? = null,
    val y2: ChartChannel? = null,
)

data class ChartChannel(
    val value: ChartValue,
    val label: String? = null,
) {
    companion object {
        fun from(raw: Any?, defaultLabel: String? = null): ChartChannel? {
            val map = raw as? Map<*, *>
            if (map != null) {
                val value = ChartValue.from(map["value"]) ?: return null
                return ChartChannel(value = value, label = map["label"] as? String ?: defaultLabel)
            }

            return ChartValue.from(raw)?.let { ChartChannel(value = it, label = defaultLabel) }
        }
    }
}

sealed class ChartValue {
    data class NumberValue(val value: Double) : ChartValue()
    data class StringValue(val value: String) : ChartValue()
    data class BoolValue(val value: Boolean) : ChartValue()

    val displayText: String
        get() = when (this) {
            is NumberValue -> if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
            is StringValue -> value
            is BoolValue -> value.toString()
        }

    fun numberOrNull(): Double? = (this as? NumberValue)?.value

    val isAxisValue: Boolean
        get() = this is NumberValue || this is StringValue

    companion object {
        fun from(raw: Any?): ChartValue? {
            return when (raw) {
                is Boolean -> BoolValue(raw)
                is Number -> NumberValue(raw.toDouble())
                is String -> raw.toDoubleOrNull()?.let { NumberValue(it) } ?: StringValue(raw)
                else -> null
            }
        }
    }
}

data class ChartMarkStyle(
    val foregroundStyle: ChartForegroundStyle? = null,
    val lineStyle: ChartLineStyle? = null,
    val interpolationMethod: ChartInterpolation? = null,
    val cornerRadius: Double? = null,
    val stacking: ChartStacking = ChartStacking.Standard,
    val symbol: ChartSymbolName? = null,
    val symbolSize: Double? = null,
    val annotation: ChartAnnotation? = null,
)

sealed class ChartForegroundStyle {
    data class ColorValue(val color: String) : ChartForegroundStyle()
    data class SeriesValue(val channel: ChartChannel) : ChartForegroundStyle()
}

enum class ChartStacking {
    Standard,
    Unstacked;

    companion object {
        fun from(raw: String?): ChartStacking =
            if (raw == "unstacked") Unstacked else Standard
    }
}

enum class ChartInterpolation {
    Linear,
    Monotone,
    Cardinal,
    CatmullRom,
    StepStart,
    StepCenter,
    StepEnd;

    companion object {
        fun from(raw: String?): ChartInterpolation? = when (raw) {
            "linear" -> Linear
            "monotone" -> Monotone
            "cardinal" -> Cardinal
            "catmullRom" -> CatmullRom
            "stepStart" -> StepStart
            "stepCenter" -> StepCenter
            "stepEnd" -> StepEnd
            else -> null
        }
    }
}

data class ChartLineStyle(
    val width: Double? = null,
    val dash: List<Double>? = null,
)

enum class ChartSymbolName {
    Circle,
    Square,
    Diamond,
    Triangle,
    Plus,
    Cross;

    companion object {
        fun from(raw: String?): ChartSymbolName? = when (raw) {
            "circle" -> Circle
            "square" -> Square
            "diamond" -> Diamond
            "triangle" -> Triangle
            "plus" -> Plus
            "cross" -> Cross
            else -> null
        }
    }
}

data class ChartAnnotation(
    val text: String,
    val position: Position = Position.Top,
) {
    enum class Position {
        Top,
        Bottom,
        Leading,
        Trailing,
        Center;

        companion object {
            fun from(raw: String?): Position = when (raw) {
                "bottom" -> Bottom
                "leading" -> Leading
                "trailing" -> Trailing
                "center" -> Center
                else -> Top
            }
        }
    }
}

data class ChartMarkAccessibility(
    val label: String? = null,
    val description: String? = null,
    val value: String? = null,
)

data class ChartAxisOptions(
    val x: ChartAxisOption? = null,
    val y: ChartAxisOption? = null,
)

data class ChartAxisOption(
    val hidden: Boolean = false,
    val values: ChartAxisValues? = null,
    val position: String? = null,
    val label: String? = null,
    val labelsHidden: Boolean = false,
    val ticksHidden: Boolean = false,
    val gridHidden: Boolean = false,
    val formatter: ChartValueFormatter? = null,
)

sealed class ChartAxisValues {
    data object Automatic : ChartAxisValues()
    data class Values(val values: List<ChartValue>) : ChartAxisValues()

    companion object {
        fun from(raw: Any?): ChartAxisValues? {
            if (raw == "automatic") return Automatic
            val list = raw as? List<*> ?: return null
            return Values(list.mapNotNull { ChartValue.from(it) }.filter { it.isAxisValue })
        }
    }
}

sealed class ChartValueFormatter {
    data class NumberFormatter(
        val minimumFractionDigits: Int? = null,
        val maximumFractionDigits: Int? = null,
    ) : ChartValueFormatter()

    data class PercentFormatter(
        val minimumFractionDigits: Int? = null,
        val maximumFractionDigits: Int? = null,
    ) : ChartValueFormatter()

    data class CurrencyFormatter(
        val currency: String,
        val minimumFractionDigits: Int? = null,
        val maximumFractionDigits: Int? = null,
    ) : ChartValueFormatter()

    data class DateFormatter(
        val dateStyle: String? = null,
        val timeStyle: String? = null,
    ) : ChartValueFormatter()

    companion object {
        fun from(raw: Any?): ChartValueFormatter? {
            val map = raw as? Map<*, *> ?: return null
            val minimum = (map["minimumFractionDigits"] as? Number)?.toInt()
            val maximum = (map["maximumFractionDigits"] as? Number)?.toInt()
            return when (map["style"] as? String) {
                "number" -> NumberFormatter(minimum, maximum)
                "percent" -> PercentFormatter(minimum, maximum)
                "currency" -> {
                    val currency = map["currency"] as? String ?: return null
                    CurrencyFormatter(currency, minimum, maximum)
                }
                "date" -> DateFormatter(
                    dateStyle = map["dateStyle"] as? String,
                    timeStyle = map["timeStyle"] as? String,
                )
                else -> null
            }
        }
    }
}

data class ChartScaleOptions(
    val x: ChartScaleOption? = null,
    val y: ChartScaleOption? = null,
)

data class ChartScaleOption(
    val type: String? = null,
    val domain: List<ChartValue>? = null,
)

data class ChartLegendOptions(
    val hidden: Boolean = false,
)

data class ChartStyleOptions(
    val foregroundStyleScale: Map<String, String> = emptyMap(),
    val symbolScale: Map<String, ChartSymbolName> = emptyMap(),
)

data class ChartSelectionOptions(
    val x: ChartSelectionBinding? = null,
    val y: ChartSelectionBinding? = null,
)

data class ChartSelectionBinding(
    val value: ChartValue? = null,
    val onChangeId: String? = null,
) {
    companion object {
        fun from(rawValue: Any?, onChangeId: String?): ChartSelectionBinding =
            ChartSelectionBinding(
                value = ChartValue.from(rawValue)?.takeIf { it.isAxisValue },
                onChangeId = onChangeId,
            )
    }
}

data class ChartAccessibilityOptions(
    val label: String? = null,
    val description: String? = null,
)

data class ChartDiagnostic(
    val severity: Severity,
    val message: String,
    val path: String,
) {
    enum class Severity {
        Warning,
        Error
    }
}
