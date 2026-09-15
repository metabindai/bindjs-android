package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.modifier.ComponentModifierProps
import java.io.Serializable

/**
 * SwiftUI's `Path` — an arbitrary vector outline built by the JS-side path
 * builder (`Path(p => { p.move(0, 0); p.line(100, 50); p.close() })`, see
 * `PathComponent` in script.js).
 *
 * `fill` / `stroke` are not children of the directive: the `.fill(...)` and
 * `.stroke(...)` modifiers fold themselves into the shape's props on the JS
 * side, exactly as they do for [CapsuleComponent].
 */
class PathComponent(
    props: PathComponentProps,
) : BaseComponent<PathComponentProps>(props)

class PathComponentProps(
    val elements: List<PathElement>? = null,
    val fill: ForegroundStyleComponent? = null,
    val stroke: StrokeStyle? = null,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    override fun toString(): String {
        return "PathComponentProps(elements=${elements?.size ?: 0}, fill=$fill, stroke=$stroke)"
    }
}

/**
 * One entry of a [PathComponent]'s element list.
 *
 * The JS builder emits a flat `{ op, ... }` object per call, with a different
 * subset of keys per `op`, so this is one nullable-field class rather than a
 * sealed hierarchy — the alternative is a second [ai.metabind.bindjs.RuntimeTypeAdapterFactory]
 * keyed on `op`, which buys nothing for ten shapes that never overlap.
 *
 * Coordinates are SwiftUI points; the renderer converts them to pixels.
 */
class PathElement(
    val op: String? = null,
    val x: Float? = null,
    val y: Float? = null,
    val width: Float? = null,
    val height: Float? = null,
    // quadCurve
    val controlX: Float? = null,
    val controlY: Float? = null,
    // curve
    val control1X: Float? = null,
    val control1Y: Float? = null,
    val control2X: Float? = null,
    val control2Y: Float? = null,
    // arc
    val centerX: Float? = null,
    val centerY: Float? = null,
    val radius: Float? = null,
    val startAngle: Float? = null,
    val endAngle: Float? = null,
    val clockwise: Boolean? = null,
    // roundedRect
    val cornerWidth: Float? = null,
    val cornerHeight: Float? = null,
    // lines — an array of [x, y] pairs
    val points: List<List<Float>>? = null,
) : Serializable {
    override fun toString(): String = "PathElement(op=$op)"
}
