package ai.metabind.bindjs.model

import com.google.gson.annotations.SerializedName

class SectionComponent(
    props: SectionProps
): BaseComponent<SectionProps>(props)

class SectionProps(
    val value: BaseComponent<*>?,
    @SerializedName("header")
    private val _header: BaseComponent<*>?,
    val footer: BaseComponent<*>?,
    children: List<BaseComponent<*>>?,
) : Props(children = children) {
    val header: BaseComponent<*>?
        get() = _header ?: value

    override fun toString(): String {
        return "SectionProps(value=$value, _header=$_header, footer=$footer, header=$header)"
    }
}