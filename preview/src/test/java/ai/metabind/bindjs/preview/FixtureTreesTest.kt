package ai.metabind.bindjs.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Keeps the committed component-fixture trees in step with the fixtures and the bundled
 * runtime. Only component fixtures have trees, since only they have screenshot tests.
 *
 * Normally it re-renders every fixture with node and fails on any difference, so a
 * changed fixture, or a re-synced `script.js` that emits a different tree, cannot leave
 * the screenshot tests painting stale input. With `-PupdateTrees` it rewrites the
 * committed files instead:
 *
 *     ./gradlew :preview:testDebugUnitTest -PupdateTrees --tests '*FixtureTreesTest*'
 *
 * Skipped, rather than failed, when node is not installed: the screenshot tests only
 * need the committed trees, and a machine without node can still run those.
 */
class FixtureTreesTest {

    @Test
    fun `every fixture has a committed tree`() {
        val missing = componentFixtures.filterNot { FixtureTrees.committedFile(it).isFile }.map { it.name }
        assertTrue(
            "No committed tree for $missing. Run the test with -PupdateTrees to render them.",
            missing.isEmpty()
        )
    }

    @Test
    fun `committed trees match what the runtime renders`() {
        assumeTrue("node is not on the PATH; cannot re-render the fixture trees", FixtureTrees.nodeAvailable())

        val fresh = FixtureTrees.render(componentFixtures)

        if (System.getProperty("bindjs.updateTrees") == "true") {
            FixtureTrees.treesDir.mkdirs()
            componentFixtures.forEach { FixtureTrees.committedFile(it).writeText(fresh.getValue(it.name)) }
            return
        }

        componentFixtures.forEach { fixture ->
            assertEquals(
                "Tree for '${fixture.name}' is stale. Re-run with -PupdateTrees and commit the result.",
                fresh.getValue(fixture.name),
                FixtureTrees.committed(fixture)
            )
        }
    }
}
