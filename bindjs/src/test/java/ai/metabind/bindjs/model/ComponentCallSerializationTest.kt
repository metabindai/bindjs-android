package ai.metabind.bindjs.model

import ai.metabind.bindjs.GsonProvider
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `callButtonStyleHandler` serializes a button's label to hand it back to JS. A label
 * holding a `ComponentCall` used to throw ("already defines a field named type"), and the
 * throw crashed the app from the button's `LaunchedEffect`.
 */
class ComponentCallSerializationTest {

    private val gson = GsonProvider.get()

    private val buttonJson = """{"type":"Button","props":{"handlerId":"h1","label":
        {"type":"HStack","props":{"children":[
          {"type":"ComponentCall","props":{"name":"Row","props":{"title":"A"},"children":[
            {"type":"Text","props":{"rawValue":"A","children":[]}}]}}]}},
        "children":[]}}"""

    @Test
    fun `a button label containing a component call serializes`() {
        val button = gson.fromJson(buttonJson, BaseComponent::class.java) as ButtonComponent

        val label = JsonParser.parseString(gson.toJson(button.props.label, BaseComponent::class.java)).asJsonObject
        val call = label["props"].asJsonObject["children"].asJsonArray[0].asJsonObject
        assertEquals("ComponentCall", call["type"].asString)
        assertEquals("Row", call["props"].asJsonObject["name"].asString)
    }

    @Test
    fun `a component call round-trips`() {
        val button = gson.fromJson(buttonJson, BaseComponent::class.java) as ButtonComponent
        val label = button.props.label

        val reparsed = gson.fromJson(gson.toJson(label, BaseComponent::class.java), BaseComponent::class.java)
        val call = reparsed.props.children!!.single()
        assertTrue(call is Component)
        assertEquals("ComponentCall", (call as Component).type)
        assertEquals("Row", call.props.name)
        assertEquals("A", call.props.propsMap["title"])
    }
}
