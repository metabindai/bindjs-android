package ai.metabind.bindjs

import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * A button style is handed the label the button was written with.
 *
 * `callButtonStyleHandler` used to rebuild the label as `Text(label.props.rawValue)`, so
 * any label that was not plain Text reached the style as a blank Text: a ChoiceList row
 * drew its card with nothing in it. The bundled runtime is run through node here.
 */
class ButtonStyleLabelScriptTest {

    @Test
    fun `a composite label reaches the style intact`() {
        val label = styledLabel(
            """Button(HStack({ spacing: 12 }, [Indicator({ on: true }), Text('Email')]), () => {})"""
        )
        assertEquals("HStack", label["type"])
        val children = label.props()["children"] as List<*>
        assertEquals("ComponentCall", (children[0] as Map<*, *>)["type"])
        assertEquals("Email", (children[1] as Map<*, *>).props()["rawValue"])
    }

    @Test
    fun `a text label still reaches the style`() {
        val label = styledLabel("""Button('Copy', () => {})""")
        assertEquals("Text", label["type"])
        assertEquals("Copy", label.props()["rawValue"])
    }

    /**
     * Renders [button] under a style that wraps its label in padding, hands the rendered
     * label back through `callButtonStyleHandler` the way the Kotlin side does, and
     * returns the label as the style drew it.
     */
    private fun styledLabel(button: String): Map<*, *> {
        assumeTrue("node is not installed", nodeAvailable())
        val component = """
            const Indicator = defineComponent({ body: (props) => Circle() });
            const Card = defineButtonStyle({ body: (configuration) => configuration.label.padding(12) });
            exports.default = defineComponent({ body: () => $button.buttonStyle(Card()) });
        """.trimIndent()
        val harness = """
            const fs = require('fs');
            console.log = () => {};
            require('vm').runInThisContext(fs.readFileSync(process.argv[1], 'utf8'));
            setComponents({ Root: ${GsonProvider.get().toJson(component)} });
            willRender();
            const find = (node, type) => {
              if (node == null || typeof node !== 'object') return null;
              if (node.type === type) return node;
              for (const value of Object.values(node)) {
                const found = find(value, type);
                if (found) return found;
              }
              return null;
            };
            const tree = JSON.parse(callComponent(['Root', {}]));
            const style = find(tree, 'buttonStyle');
            const button = find(tree, 'Button');
            const styled = JSON.parse(callButtonStyleHandler(style.props.handlerId, button.props.label, false));
            // The style wraps the label in .padding(12); the label is what that pads.
            process.stdout.write(JSON.stringify(find(styled, 'ModifiedComponent').props.content[0]));
        """.trimIndent()
        val script = File("src/main/res/raw/script.js").absolutePath
        val process = ProcessBuilder("node", "-e", harness, script).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0) { output }
        return GsonProvider.get().fromJson(output, Map::class.java)
    }

    private fun Map<*, *>.props(): Map<*, *> = this["props"] as Map<*, *>

    private fun nodeAvailable(): Boolean = runCatching {
        ProcessBuilder("node", "--version").redirectErrorStream(true).start().let {
            it.inputStream.readBytes(); it.waitFor(30, TimeUnit.SECONDS) && it.exitValue() == 0
        }
    }.getOrDefault(false)
}
