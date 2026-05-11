package ai.metabind.bindjs.model

class GeometryReaderComponent(
    props: GeometryReaderProps
) : BaseComponent<GeometryReaderProps>(props)

class GeometryReaderProps(
    val handlerId: String?,
    val environmentId: String?,
    children: List<BaseComponent<*>>?
) : Props(children = emptyList()) {
    override fun toString(): String {
        return "GeometryReaderProps(handlerId='$handlerId')"
    }
}