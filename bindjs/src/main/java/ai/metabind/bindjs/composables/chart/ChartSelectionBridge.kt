package ai.metabind.bindjs.composables.chart

import ai.metabind.bindjs.model.chart.ChartValue
import ai.metabind.bindjs.model.chart.PieSelectionBinding

internal data class ChartSelectionPayload(
    val handlerId: String,
    val value: Any,
)

internal fun chartSelectionPayloads(
    xSelectionHandlerId: String?,
    ySelectionHandlerId: String?,
    xValue: ChartValue?,
    yValue: ChartValue?,
): List<ChartSelectionPayload> =
    buildList {
        if (xSelectionHandlerId != null && xValue != null) {
            add(ChartSelectionPayload(xSelectionHandlerId, xValue.eventValue))
        }
        if (ySelectionHandlerId != null && yValue != null) {
            add(ChartSelectionPayload(ySelectionHandlerId, yValue.eventValue))
        }
    }

internal fun pieSelectionPayload(
    selection: PieSelectionBinding?,
    sliceId: String?,
): ChartSelectionPayload? {
    val handlerId = selection?.onChangeId ?: return null
    val value = sliceId ?: return null
    return ChartSelectionPayload(handlerId, value)
}

private val ChartValue.eventValue: Any
    get() = when (this) {
        is ChartValue.NumberValue -> value
        is ChartValue.StringValue -> value
        is ChartValue.BoolValue -> value
    }
