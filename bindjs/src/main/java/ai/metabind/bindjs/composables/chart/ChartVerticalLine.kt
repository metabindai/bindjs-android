package ai.metabind.bindjs.composables.chart

import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.common.component.LineComponent

/**
 * A [Decoration] that draws a vertical line at an _x_ value, spanning the full height of the plot
 * area — the counterpart to Vico's own [com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine],
 * which Vico ships only for _y_.
 *
 * A `RuleMark({ x })` used to be rendered as a two-point line series running from the data's
 * minimum _y_ to its maximum. That put it in the layer's coordinate space, so it stopped at the
 * outermost data points — and the y-range provider adds 12% headroom past those, leaving the line
 * visibly short of the plot at both ends where SwiftUI Charts runs it edge to edge. A decoration
 * draws against [CartesianDrawingContext.layerBounds] instead, which is the plot itself.
 *
 * The _x_ mapping mirrors the one every layer uses (`LineCartesianLayer.getDrawX`), so the line
 * lands on the same pixel as the point it marks: from the start of the plot, past the layer's
 * start padding, then [CartesianLayerDimensions.xSpacing] per _x_ step.
 */
internal data class VerticalLine(val x: Double, val line: LineComponent) : Decoration {
    override fun drawOverLayers(context: CartesianDrawingContext) {
        with(context) {
            val plotStart = if (isLtr) layerBounds.left else layerBounds.right
            val drawingStart =
                plotStart + layoutDirectionMultiplier * layerDimensions.startPadding - scroll
            val canvasX = drawingStart +
                layoutDirectionMultiplier * layerDimensions.xSpacing *
                ((x - ranges.minX) / ranges.xStep).toFloat()
            // Decorations aren't clipped to the plot, so an x outside the visible range would
            // paint over the axis labels rather than scroll out of view.
            if (canvasX < layerBounds.left || canvasX > layerBounds.right) return
            line.drawVertical(
                context = context,
                x = canvasX,
                top = layerBounds.top,
                bottom = layerBounds.bottom,
            )
        }
    }
}
