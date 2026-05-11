package ai.metabind.bindjs.model.modifier

import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.model.BaseComponent

class FloatModifierProps(
    @SerializedName(value = "rawValue", alternate = ["value"])
    val rawValue: Float,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)