package ai.metabind.bindjs.model

class ImageComponent(
    props: ImageProps
) : BaseComponent<ImageProps>(props)

class ImageProps(
    val url: String?,
    val svg: String?,
    val systemName: String?,
    val contentMode: String?,
    children: List<BaseComponent<*>>?,
) : Props(children = children) {
    override fun toString(): String {
        return "ImageProps(url=$url, svg=$svg, systemName=$systemName)"
    }
}
