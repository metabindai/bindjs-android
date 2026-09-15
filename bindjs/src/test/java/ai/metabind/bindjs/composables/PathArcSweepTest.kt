package ai.metabind.bindjs.composables

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SwiftUI's `clockwise` flag and Compose's signed sweep disagree about which way
 * round the screen's flipped y-axis makes "clockwise", so every arc that reaches
 * [arcSweepDegrees] with the flag set has to change sign.
 */
class PathArcSweepTest {

    @Test
    fun `counter-clockwise flag sweeps in the increasing-angle direction`() {
        assertEquals(90f, arcSweepDegrees(0f, 90f, clockwise = false), 0f)
        assertEquals(180f, arcSweepDegrees(0f, 180f, clockwise = false), 0f)
    }

    @Test
    fun `clockwise flag sweeps the other way round the circle`() {
        // The complement, not the same arc mirrored: `0 → 90, clockwise` is the
        // long way round, which is what SwiftUI draws.
        assertEquals(-270f, arcSweepDegrees(0f, 90f, clockwise = true), 0f)
        assertEquals(-90f, arcSweepDegrees(0f, 270f, clockwise = true), 0f)
    }

    @Test
    fun `a descending angle pair is normalised for the counter-clockwise case`() {
        assertEquals(270f, arcSweepDegrees(90f, 0f, clockwise = false), 0f)
        assertEquals(-90f, arcSweepDegrees(90f, 0f, clockwise = true), 0f)
    }

    @Test
    fun `a full turn stays a full turn`() {
        // Normalising these into (-360, 360) would collapse the sweep to zero and
        // draw nothing at all where a whole circle was asked for.
        assertEquals(360f, arcSweepDegrees(0f, 360f, clockwise = false), 0f)
        assertEquals(-360f, arcSweepDegrees(0f, 360f, clockwise = true), 0f)
        assertEquals(360f, arcSweepDegrees(0f, 720f, clockwise = false), 0f)
    }

    @Test
    fun `a degenerate arc sweeps nothing`() {
        assertEquals(0f, arcSweepDegrees(45f, 45f, clockwise = false), 0f)
        assertEquals(0f, arcSweepDegrees(45f, 45f, clockwise = true), 0f)
    }

    @Test
    fun `an ordinary arc is a single arcTo`() {
        assertEquals(listOf(0f to 90f), arcSegments(0f, 90f, clockwise = false))
        assertEquals(listOf(90f to -90f), arcSegments(90f, 0f, clockwise = true))
    }

    @Test
    fun `a full turn is split into two halves`() {
        // `android.graphics.Path.arcTo` takes the sweep mod 360, so handing it a whole
        // turn in one call drew nothing at all — the circle fixture came out blank.
        assertEquals(listOf(0f to 180f, 180f to 180f), arcSegments(0f, 360f, clockwise = false))
        assertEquals(listOf(0f to -180f, -180f to -180f), arcSegments(0f, 360f, clockwise = true))
        assertEquals(listOf(45f to 180f, 225f to 180f), arcSegments(45f, 405f, clockwise = false))
    }
}
