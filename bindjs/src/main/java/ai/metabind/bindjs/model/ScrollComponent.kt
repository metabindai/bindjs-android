package ai.metabind.bindjs.model

import com.google.gson.annotations.SerializedName

class ScrollComponent(
    props: ScrollProps
): BaseComponent<ScrollProps>(props)

class ScrollProps(
    @SerializedName("showsIndicators")
    private val _showsIndicators: Boolean?,
    @SerializedName("axis")
    private val _axis: String?,
    children: List<BaseComponent<*>>?,
) : Props(children = children) {
    val showsIndicators: Boolean
        get() = _showsIndicators == true
    val axis: ScrollAxis
        get() {
            return if (_axis == "horizontal") {
                ScrollAxis.HORIZONTAL
            } else {
                ScrollAxis.VERTICAL
            }
        }

    override fun toString(): String {
        return "ScrollProps(_showsIndicators=$_showsIndicators, _axis=$_axis, showsIndicators=$showsIndicators, axis=$axis)"
    }

}

enum class ScrollAxis {
    HORIZONTAL,
    VERTICAL
}