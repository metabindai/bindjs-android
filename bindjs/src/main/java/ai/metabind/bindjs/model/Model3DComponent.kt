package ai.metabind.bindjs.model

class Model3DComponent(
    props: Model3DProps
) : BaseComponent<Model3DProps>(props)

class Model3DProps(
    val url: String?,
    val autoRotate: Boolean? = false,
    val cameraControls: Boolean? = true,
    children: List<BaseComponent<*>>?,
) : Props(children = children) {
    override fun toString(): String {
        return "Model3DProps(url=$url, autoRotate=$autoRotate, cameraControls=$cameraControls)"
    }
}