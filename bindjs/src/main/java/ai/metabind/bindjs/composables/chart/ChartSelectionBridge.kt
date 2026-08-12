package ai.metabind.bindjs.composables.chart

import ai.metabind.bindjs.model.chart.ChartValue
import ai.metabind.bindjs.model.chart.PieSelectionBinding
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.compose.cartesian.marker.Interaction
import kotlin.math.abs

internal data class ChartSelectionPayload(
    val handlerId: String,
    // `null` clears the selection — see [UiEvent.OnChartSelection].
    val value: Any?,
)

/**
 * A marker that draws nothing. Vico only reports marker targets — the press-and-drag
 * hit-testing selection needs — for a chart that has a marker, so this stands in for
 * one. The visible highlight belongs to the JS component, which re-renders off the
 * selection it gets back.
 */
internal object SelectionOnlyMarker : CartesianMarker

/**
 * Selection follows the finger and ends with it, which is what SwiftUI's
 * `.chartXSelection` does on iOS: press a column and its value reads out, drag and the
 * readout scrubs across the plot, lift and the selection is gone.
 *
 * That last part is why this exists rather than Vico's own show-on-press controller,
 * which is otherwise the same shape: `Lock.Position` re-resolves the marker's x when
 * the plot moves, and the JS component moves it on every selection by re-rendering
 * with an extra mark — so the selection would walk along the axis on its own.
 */
internal class SelectionMarkerController : CartesianMarkerController {
    private var pressed = false

    override val acceptsLongPress = false

    override val lock = CartesianMarkerController.Lock.X

    override fun shouldAcceptInteraction(
        interaction: Interaction,
        targets: List<CartesianMarker.Target>,
    ): Boolean =
        when (interaction) {
            is Interaction.Press -> {
                pressed = true
                true
            }

            is Interaction.Move -> pressed
            is Interaction.Release -> {
                pressed = false
                true
            }

            else -> false
        }

    override fun shouldShowMarker(
        interaction: Interaction,
        targets: List<CartesianMarker.Target>,
    ): Boolean = interaction !is Interaction.Release
}

/**
 * Turns Vico marker targets into `chartXSelection`/`chartYSelection` handler calls.
 *
 * The marker going away is a selection change too, not the absence of one: iOS hands
 * the binding's nil straight to the handler when the gesture ends, so the readout
 * falls back to its placeholder on release. Repeats are dropped in both directions, so
 * a drag across one column doesn't re-enter JS on every pointer sample and a touch on
 * an already-clear chart doesn't clear it again.
 */
internal class ChartSelectionListener(
    private val rows: List<ChartSelectionRow>,
    private val xSelectionHandlerId: String?,
    private val ySelectionHandlerId: String?,
    private val onSelection: (ChartSelectionPayload) -> Unit,
) : CartesianMarkerVisibilityListener {
    private var lastX: Double? = null

    override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
        report(targets)
    }

    override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
        report(targets)
    }

    override fun onHidden(marker: CartesianMarker) {
        if (lastX == null) return
        lastX = null
        chartSelectionClearPayloads(xSelectionHandlerId, ySelectionHandlerId).forEach(onSelection)
    }

    private fun report(targets: List<CartesianMarker.Target>) {
        val x = targets.firstOrNull()?.x ?: return
        if (x == lastX) return
        lastX = x
        // Grouped columns and multi-series lines put several rows on one x; the first
        // match is the topmost series, the same one SwiftUI's selection reports.
        val row = rows.minByOrNull { abs(it.x - x) } ?: return
        chartSelectionPayloads(
            xSelectionHandlerId = xSelectionHandlerId,
            ySelectionHandlerId = ySelectionHandlerId,
            xValue = row.xValue,
            yValue = row.yValue,
        ).forEach(onSelection)
    }
}

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

/** The same handlers, told the selection is over. */
internal fun chartSelectionClearPayloads(
    xSelectionHandlerId: String?,
    ySelectionHandlerId: String?,
): List<ChartSelectionPayload> =
    listOfNotNull(xSelectionHandlerId, ySelectionHandlerId)
        .map { ChartSelectionPayload(it, null) }

/** A `null` [sliceId] is the cleared selection, not a missing one. */
internal fun pieSelectionPayload(
    selection: PieSelectionBinding?,
    sliceId: String?,
): ChartSelectionPayload? {
    val handlerId = selection?.onChangeId ?: return null
    return ChartSelectionPayload(handlerId, sliceId)
}

private val ChartValue.eventValue: Any
    get() = when (this) {
        is ChartValue.NumberValue -> value
        is ChartValue.StringValue -> value
        is ChartValue.BoolValue -> value
    }
