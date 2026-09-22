package ai.metabind.bindjs

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * A picked tag reaches its setter unchanged, whatever characters it holds.
 *
 * The script used to paste the tag between single quotes, so choosing a "Men's" option
 * produced `callEventHandler('id', 'Men's',[])` — a syntax error, the setter never ran
 * and the picker stayed on its old value. The script is run through node here, since
 * only a JavaScript parser can say whether it parses and what it passes.
 */
class PickerSetterScriptTest {

    private val gson = GsonProvider.get()

    @Test
    fun `tags reach the setter exactly as written`() {
        val tags = listOf(
            "plain",
            "Men's",
            "say \"hi\"",
            "back\\slash",
            "two\nlines",
            "</script> & <b>",
            "emoji 🍒",
        )
        tags.forEach { tag ->
            val script = pickerSetterScript(gson, "0,Root_0,Picker_0,Picker,0", tag)
            assertEquals(
                "for tag ${gson.toJson(tag)}",
                listOf("0,Root_0,Picker_0,Picker,0", tag),
                evaluate(script),
            )
        }
    }

    /** Runs [script] in node with a `callEventHandler` that reports what it was given. */
    private fun evaluate(script: String): List<String> {
        assumeTrue("node is not installed", nodeAvailable())
        val harness = "const callEventHandler = (...args) => " +
            "process.stdout.write(JSON.stringify(args));\n$script"
        val process = ProcessBuilder("node", "-e", harness).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0) {
            "script failed to run:\n$script\n$output"
        }
        return gson.fromJson(output, Array<String>::class.java).toList()
    }

    private fun nodeAvailable(): Boolean = runCatching {
        ProcessBuilder("node", "--version").redirectErrorStream(true).start().let {
            it.inputStream.readBytes(); it.waitFor(30, TimeUnit.SECONDS) && it.exitValue() == 0
        }
    }.getOrDefault(false)
}
