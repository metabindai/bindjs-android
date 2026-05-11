package ai.metabind.bindjs.model

class MaterialComponent(
    props: MaterialProps
) : BaseComponent<MaterialProps>(props) {
    companion object {
        private val MATERIALS = listOf<String>(
            "thin",
            "regular",
            "thick",
            "ultraThin",
            "bar",
            "chrome"
        )
    }

    fun isMaterial(): Boolean {
        return props.rawValue in MATERIALS
    }
}

class MaterialProps(
    children: List<BaseComponent<*>>?,
    val rawValue: String?,
) : Props(children = children) {
    override fun toString(): String {
        return "MaterialProps(rawValue=$rawValue)"
    }
}
