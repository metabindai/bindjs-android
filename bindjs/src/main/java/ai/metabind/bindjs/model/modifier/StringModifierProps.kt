package ai.metabind.bindjs.model.modifier

import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.model.BaseComponent

class StringModifierProps(
    @SerializedName(value = "rawValue", alternate = ["value"])
    val rawValue: String,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)