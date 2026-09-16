package ai.metabind.bindjs.preview

/** A drawer section. The order here is the order in the drawer. */
enum class PreviewSection(val title: String) {
    Components("Components"),
    Charts("Charts"),
}

/**
 * One entry in the drawer: a BindJS body that the app registers as a component and renders
 * when selected. [fixedHeight] wraps the body in `.frame({ height })`, which charts need
 * because a chart has no intrinsic height of its own.
 */
class PreviewFixture(
    val name: String,
    val description: String,
    val section: PreviewSection,
    val source: String,
    val fixedHeight: Int? = null,
) {
    /**
     * The name the fixture is registered and called under. Prefixed because a fixture is
     * naturally named after the component it shows, and a user component registered as
     * `Slider` shadows the built-in `Slider` directive: the body's `Slider({...})` call then
     * recurses into itself until the JS sandbox dies of a stack overflow.
     */
    val componentName: String
        get() = "Fixture_" + name.filter { it.isLetterOrDigit() || it == '_' }

    fun componentSource(): String {
        val body = if (fixedHeight != null) "($source).frame({ height: $fixedHeight })" else "($source)"
        return """
        const metadata = {
          title: ${"Fixture: $name".jsonString()},
          description: ${description.jsonString()},
        };
        const body = () => $body;
        const previews = [Self().previewName("Default")];
        exports.default = defineComponent({ metadata, body, previews });
        """.trimIndent()
    }
}

private fun String.jsonString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

/** Every fixture, in drawer order. */
val allFixtures: List<PreviewFixture> = componentFixtures + chartFixtures

fun List<PreviewFixture>.inSection(section: PreviewSection): List<PreviewFixture> =
    filter { it.section == section }
