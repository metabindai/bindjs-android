package ai.metabind.bindjs.composables.chart

import ai.metabind.bindjs.model.chart.ChartValue
import ai.metabind.bindjs.model.chart.PieSelectionBinding
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.Interaction
import com.patrykandpatrick.vico.compose.common.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The touch half of `chartXSelection` on Android: which touches drive a selection, and
 * what the marker target that comes back turns into for the JS handler.
 */
class ChartSelectionTest {

    @Test
    fun theMarkerFollowsTheFinger() {
        val controller = SelectionMarkerController()

        assertTrue(controller.accept(Interaction.Press(Point(10f, 10f))))
        assertTrue(controller.shows(Interaction.Press(Point(10f, 10f))))
        assertTrue(controller.accept(Interaction.Move(Point(90f, 10f))))
        assertTrue(controller.shows(Interaction.Move(Point(90f, 10f))))
    }

    @Test
    fun releasingEndsTheSelection() {
        val controller = SelectionMarkerController()

        controller.accept(Interaction.Press(Point(10f, 10f)))

        assertTrue("the release has to be acted on, not ignored", controller.accept(Interaction.Release(Point(10f, 10f))))
        assertFalse(controller.shows(Interaction.Release(Point(10f, 10f))))
    }

    @Test
    fun aMoveWithNoFingerDownIsNotASelection() {
        val controller = SelectionMarkerController()

        // A hover, or the tail of a gesture that has already ended.
        assertFalse(controller.accept(Interaction.Move(Point(90f, 10f))))
    }

    @Test
    fun listenerReportsTheRowUnderTheTarget() {
        val dispatched = mutableListOf<ChartSelectionPayload>()
        val listener = listener(dispatched)

        listener.onShown(SelectionOnlyMarker, listOf(target(1.0)))

        assertEquals(
            listOf(
                ChartSelectionPayload("selectMonth", "Feb"),
                ChartSelectionPayload("selectValue", 20.0),
            ),
            dispatched,
        )
    }

    @Test
    fun listenerSnapsToTheNearestRow() {
        val dispatched = mutableListOf<ChartSelectionPayload>()
        val listener = listener(dispatched)

        listener.onShown(SelectionOnlyMarker, listOf(target(1.6)))

        assertEquals(
            listOf(ChartSelectionPayload("selectMonth", "Mar"), ChartSelectionPayload("selectValue", 30.0)),
            dispatched,
        )
    }

    @Test
    fun listenerDoesNotRepeatItselfWhileScrubbingOneColumn() {
        val dispatched = mutableListOf<ChartSelectionPayload>()
        val listener = listener(dispatched)

        listener.onShown(SelectionOnlyMarker, listOf(target(0.0)))
        listener.onUpdated(SelectionOnlyMarker, listOf(target(0.0)))
        listener.onUpdated(SelectionOnlyMarker, listOf(target(1.0)))

        assertEquals(
            listOf(
                ChartSelectionPayload("selectMonth", "Jan"),
                ChartSelectionPayload("selectValue", 10.0),
                ChartSelectionPayload("selectMonth", "Feb"),
                ChartSelectionPayload("selectValue", 20.0),
            ),
            dispatched,
        )
    }

    @Test
    fun theMarkerGoingAwayClearsTheSelection() {
        val dispatched = mutableListOf<ChartSelectionPayload>()
        val listener = listener(dispatched)

        listener.onShown(SelectionOnlyMarker, listOf(target(0.0)))
        dispatched.clear()
        listener.onHidden(SelectionOnlyMarker)

        assertEquals(
            listOf(
                ChartSelectionPayload("selectMonth", null),
                ChartSelectionPayload("selectValue", null),
            ),
            dispatched,
        )
    }

    @Test
    fun anAlreadyClearChartIsNotClearedAgain() {
        val dispatched = mutableListOf<ChartSelectionPayload>()
        val listener = listener(dispatched)

        listener.onHidden(SelectionOnlyMarker)
        listener.onShown(SelectionOnlyMarker, listOf(target(0.0)))
        listener.onHidden(SelectionOnlyMarker)
        listener.onHidden(SelectionOnlyMarker)

        assertEquals(
            listOf(
                ChartSelectionPayload("selectMonth", "Jan"),
                ChartSelectionPayload("selectValue", 10.0),
                ChartSelectionPayload("selectMonth", null),
                ChartSelectionPayload("selectValue", null),
            ),
            dispatched,
        )
    }

    @Test
    fun aClearedSliceStillCarriesItsHandler() {
        val payload = pieSelectionPayload(
            selection = PieSelectionBinding(value = "north", onChangeId = "selectRegion"),
            sliceId = null,
        )

        assertEquals(ChartSelectionPayload("selectRegion", null), payload)
    }

    private fun listener(into: MutableList<ChartSelectionPayload>) =
        ChartSelectionListener(
            rows = listOf(
                ChartSelectionRow(0.0, ChartValue.StringValue("Jan"), ChartValue.NumberValue(10.0)),
                ChartSelectionRow(1.0, ChartValue.StringValue("Feb"), ChartValue.NumberValue(20.0)),
                ChartSelectionRow(2.0, ChartValue.StringValue("Mar"), ChartValue.NumberValue(30.0)),
            ),
            xSelectionHandlerId = "selectMonth",
            ySelectionHandlerId = "selectValue",
            onSelection = { into.add(it) },
        )

    private fun target(x: Double): CartesianMarker.Target =
        object : CartesianMarker.Target {
            override val x = x
            override val canvasX = 0f
        }

    private fun SelectionMarkerController.accept(interaction: Interaction) =
        shouldAcceptInteraction(interaction, emptyList())

    private fun SelectionMarkerController.shows(interaction: Interaction) =
        shouldShowMarker(interaction, emptyList())
}
