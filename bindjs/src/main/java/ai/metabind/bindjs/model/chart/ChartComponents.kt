package ai.metabind.bindjs.model.chart

import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.Props

class ChartComponent(
    props: ChartProps,
) : BaseComponent<ChartProps>(props)

class ChartProps(
    children: List<BaseComponent<*>?>?,
) : Props(children = children)

class PieChartComponent(
    props: PieChartProps,
) : BaseComponent<PieChartProps>(props)

class PieChartProps(
    children: List<BaseComponent<*>?>?,
    val innerRadius: Double? = null,
) : Props(children = children)

interface ChartMarkComponent {
    val markKind: ChartMarkKind
    val markProps: ChartMarkProps
}

enum class ChartMarkKind {
    Bar,
    Line,
    Area,
    Point,
    Rule,
    Rectangle,
}

open class ChartMarkProps(
    children: List<BaseComponent<*>?>?,
    @SerializedName(value = "id")
    val explicitId: String? = null,
    val x: Any? = null,
    val y: Any? = null,
    val x2: Any? = null,
    val y2: Any? = null,
    val stacking: String? = null,
) : Props(children = children)

class BarMarkComponent(
    props: ChartMarkProps,
) : BaseComponent<ChartMarkProps>(props), ChartMarkComponent {
    override val markKind: ChartMarkKind get() = ChartMarkKind.Bar
    override val markProps: ChartMarkProps get() = props
}

class LineMarkComponent(
    props: ChartMarkProps,
) : BaseComponent<ChartMarkProps>(props), ChartMarkComponent {
    override val markKind: ChartMarkKind get() = ChartMarkKind.Line
    override val markProps: ChartMarkProps get() = props
}

class AreaMarkComponent(
    props: ChartMarkProps,
) : BaseComponent<ChartMarkProps>(props), ChartMarkComponent {
    override val markKind: ChartMarkKind get() = ChartMarkKind.Area
    override val markProps: ChartMarkProps get() = props
}

class PointMarkComponent(
    props: ChartMarkProps,
) : BaseComponent<ChartMarkProps>(props), ChartMarkComponent {
    override val markKind: ChartMarkKind get() = ChartMarkKind.Point
    override val markProps: ChartMarkProps get() = props
}

class RuleMarkComponent(
    props: ChartMarkProps,
) : BaseComponent<ChartMarkProps>(props), ChartMarkComponent {
    override val markKind: ChartMarkKind get() = ChartMarkKind.Rule
    override val markProps: ChartMarkProps get() = props
}

class RectangleMarkComponent(
    props: ChartMarkProps,
) : BaseComponent<ChartMarkProps>(props), ChartMarkComponent {
    override val markKind: ChartMarkKind get() = ChartMarkKind.Rectangle
    override val markProps: ChartMarkProps get() = props
}

interface PieSliceMarkComponentProtocol {
    val sliceProps: PieSliceMarkProps
}

class PieSliceMarkProps(
    children: List<BaseComponent<*>?>?,
    @SerializedName(value = "id")
    val explicitId: String? = null,
    val value: Double? = null,
    val label: String? = null,
) : Props(children = children)

class PieSliceMarkComponent(
    props: PieSliceMarkProps,
) : BaseComponent<PieSliceMarkProps>(props), PieSliceMarkComponentProtocol {
    override val sliceProps: PieSliceMarkProps get() = props
}
