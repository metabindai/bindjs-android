package ai.metabind.bindjs.model

class EmptyComponent(val type: String = "Unknown"): BaseComponent<Props>(Props(children = emptyList())) {
    override fun toString(): String {
        return "EmptyComponent (type=$type)"
    }
}