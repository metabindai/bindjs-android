package ai.metabind.bindjs.preview

import com.google.gson.Gson
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory

/**
 * The committed component trees the screenshot tests render, and the node step that
 * produces them.
 *
 * A fixture is JS, and the JS isolate only runs inside Android's WebView sandbox, which
 * Robolectric does not have. So the trees are rendered once with node against the bundled
 * runtime and committed under `src/test/resources/trees`; the screenshot tests then only
 * exercise the Compose half. [FixtureTreesTest] keeps the two in step. Only component
 * fixtures are covered.
 *
 * Paths are relative to the module directory, which is where Gradle runs unit tests.
 */
object FixtureTrees {
    val treesDir = File("src/test/resources/trees")
    private val runtimeScript = File("../bindjs/src/main/res/raw/script.js")
    private val renderer = File("src/test/node/render-fixtures.js")

    fun committedFile(fixture: PreviewFixture): File = File(treesDir, "${fixture.name}.json")

    fun committed(fixture: PreviewFixture): String = committedFile(fixture).readText()

    /** Whether `node` is on the PATH; without it the trees can be read but not rebuilt. */
    fun nodeAvailable(): Boolean = runCatching {
        ProcessBuilder("node", "--version").redirectErrorStream(true).start().let {
            it.inputStream.readBytes(); it.waitFor(30, TimeUnit.SECONDS) && it.exitValue() == 0
        }
    }.getOrDefault(false)

    /** Renders every fixture with node, returning `name → tree JSON`. Throws on any failure. */
    fun render(fixtures: List<PreviewFixture>): Map<String, String> {
        val work = createTempDirectory("bindjs-fixture-trees").toFile()
        try {
            val input = File(work, "fixtures.json")
            input.writeText(Gson().toJson(fixtures.map {
                mapOf("name" to it.name, "componentName" to it.componentName, "source" to it.componentSource())
            }))
            val out = File(work, "trees")
            val process = ProcessBuilder(
                "node", renderer.absolutePath, runtimeScript.absolutePath, input.absolutePath, out.absolutePath
            ).redirectErrorStream(true).start()
            val log = process.inputStream.bufferedReader().readText()
            check(process.waitFor(2, TimeUnit.MINUTES)) { "node timed out rendering fixture trees" }
            check(process.exitValue() == 0) { "node failed rendering fixture trees:\n$log" }
            return fixtures.associate { it.name to File(out, "${it.name}.json").readText() }
        } finally {
            work.deleteRecursively()
        }
    }
}
