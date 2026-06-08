package ai.metabind.bindjs.composables.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartAreaStackingTest {
    @Test
    fun stacksTwoSeriesOntoRunningTotal() {
        // North (bottom) then South: South accumulates onto North at each shared x.
        val north = listOf(0.0, 1.0) to listOf(12.0, 18.0)
        val south = listOf(0.0, 1.0) to listOf(9.0, 15.0)

        val stacked = cumulativeStackedYValues(listOf(north, south))

        assertEquals(listOf(12.0, 18.0), stacked[0])
        assertEquals(listOf(21.0, 33.0), stacked[1])
    }

    @Test
    fun accumulatesPerXIndependently() {
        val a = listOf(0.0, 1.0, 2.0) to listOf(1.0, 2.0, 3.0)
        val b = listOf(0.0, 1.0, 2.0) to listOf(10.0, 20.0, 30.0)
        val c = listOf(0.0, 1.0, 2.0) to listOf(100.0, 200.0, 300.0)

        val stacked = cumulativeStackedYValues(listOf(a, b, c))

        assertEquals(listOf(1.0, 2.0, 3.0), stacked[0])
        assertEquals(listOf(11.0, 22.0, 33.0), stacked[1])
        assertEquals(listOf(111.0, 222.0, 333.0), stacked[2])
    }

    @Test
    fun seriesWithDisjointXDoNotInterfere() {
        val a = listOf(0.0) to listOf(5.0)
        val b = listOf(1.0) to listOf(7.0)

        val stacked = cumulativeStackedYValues(listOf(a, b))

        // Different x means b starts from its own baseline rather than stacking on a.
        assertEquals(listOf(5.0), stacked[0])
        assertEquals(listOf(7.0), stacked[1])
    }
}
