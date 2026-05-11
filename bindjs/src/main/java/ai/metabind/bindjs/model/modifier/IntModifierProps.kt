package ai.metabind.bindjs.model.modifier

import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.model.BaseComponent

class IntModifierProps(
    @SerializedName(value = "rawValue", alternate = ["value"])
    val rawValue: Int,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children)