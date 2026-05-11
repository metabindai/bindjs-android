package ai.metabind.bindjs.model.props

import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.Props

class OptionalStringValueProps(
    @SerializedName(value = "rawValue", alternate = ["value", "label"])
    val rawValue: String?,
    children: List<BaseComponent<*>>?,
) : Props(children = children) {
    override fun toString(): String {
        return "OptionalStringValueProps(value=$rawValue)"
    }
}
