package ai.metabind.bindjs.model

class VideoComponent(
    props: VideoProps
) : BaseComponent<VideoProps>(props)

class VideoProps(
    val url: String?,
    val autoplay: Boolean?,
    val muted: Boolean?,
    val controls: Boolean?,
    val loop: Boolean?,
    val contentMode: String?,
    children: List<BaseComponent<*>>?,
) : Props(children = children) {
    override fun toString(): String {
        return "VideoProps(url=$url, autoplay=$autoplay, muted=$muted, controls=$controls, loop=$loop, contentMode=$contentMode)"
    }
}