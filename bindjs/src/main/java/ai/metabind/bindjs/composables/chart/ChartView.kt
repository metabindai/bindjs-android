package ai.metabind.bindjs.composables.chart

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerDimensions
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerPadding
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Position
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.hasFrame
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.ColorProps
import ai.metabind.bindjs.model.chart.ChartComponent
import ai.metabind.bindjs.model.chart.ChartDiagnostic
import ai.metabind.bindjs.model.chart.ChartAnnotation
import ai.metabind.bindjs.model.chart.ChartAxisValues
import ai.metabind.bindjs.model.chart.ChartForegroundStyle
import ai.metabind.bindjs.model.chart.ChartInterpolation
import ai.metabind.bindjs.model.chart.ChartMark
import ai.metabind.bindjs.model.chart.ChartMarkKind
import ai.metabind.bindjs.model.chart.ChartMarkStyle
import ai.metabind.bindjs.model.chart.ChartModel
import ai.metabind.bindjs.model.chart.ChartStacking
import ai.metabind.bindjs.model.chart.ChartSymbolName
import ai.metabind.bindjs.model.chart.ChartValue
import ai.metabind.bindjs.model.modifier.ComponentModifier
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private const val TAG = "BindJSChartView"

@Composable
fun ChartView(
    component: ChartComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    val model = remember(component, modifiers) { ChartCollector.collect(component, modifiers) }
    val prepared = remember(model) { PreparedChartData.from(model) }
    val rectangleChart = remember(model) { PreparedRectangleChartData.from(model) }
    val accessibilityDescription = remember(model) { model.accessibilityDescription() }

    LaunchedEffect(model.diagnostics) {
        model.diagnostics.forEach { diagnostic ->
            val message = "${diagnostic.path}: ${diagnostic.message}"
            if (diagnostic.severity == ChartDiagnostic.Severity.Error) {
                Log.e(TAG, message)
            } else {
                Log.w(TAG, message)
            }
        }
    }

    if (prepared.isEmpty && rectangleChart == null) {
        Box(modifier = modifiers.buildModifier(onUiEvent)) {
            Text("Unsupported chart content")
        }
        return
    }

    val chartModifier = modifiers
        .buildModifier(onUiEvent)
        .then(if (!modifiers.hasFrame()) Modifier.fillMaxWidth().height(240.dp) else Modifier)
        .then(prepared.selectionModifier(onUiEvent))
        .then(
            if (accessibilityDescription != null) {
                Modifier.semantics {
                    contentDescription = accessibilityDescription
                }
            } else {
                Modifier
            }
        )

    val modelProducer = remember { CartesianChartModelProducer() }
    if (rectangleChart != null) {
        RectangleChartView(
            data = rectangleChart,
            modifier = chartModifier,
            showLegend = !model.legend.hidden && rectangleChart.legendEntries.isNotEmpty(),
        )
        return
    }

    LaunchedEffect(prepared) {
        modelProducer.runTransaction {
            if (prepared.columns.isNotEmpty()) {
                columnSeries {
                    prepared.columns.forEach { series(x = it.xValues, y = it.yValues) }
                }
            }
            if (prepared.lines.isNotEmpty()) {
                lineSeries {
                    prepared.lines.forEach { series(x = it.xValues, y = it.yValues) }
                }
            }
            if (prepared.areas.isNotEmpty()) {
                lineSeries {
                    prepared.areas.forEach { series(x = it.xValues, y = it.yValues) }
                }
            }
            if (prepared.points.isNotEmpty()) {
                lineSeries {
                    prepared.points.forEach { series(x = it.xValues, y = it.yValues) }
                }
            }
            if (prepared.xRules.isNotEmpty()) {
                lineSeries {
                    prepared.xRules.forEach { series(x = it.xValues, y = it.yValues) }
                }
            }
            prepared.placeholder?.let { placeholder ->
                lineSeries { series(x = placeholder.xValues, y = placeholder.yValues) }
            }
        }
    }

    val xValueFormatter = remember(prepared.xLabels) {
        CartesianValueFormatter { _, value, _ -> prepared.xLabel(value) }
    }
    val yValueFormatter = remember(prepared.yLabels) {
        CartesianValueFormatter { _, value, _ -> prepared.yLabel(value) }
    }
    val xItemPlacer = remember(prepared.xLabels) {
        FiniteHorizontalAxisItemPlacer(prepared.xLabels.keys)
    }
    val axisTitleComponent = rememberTextComponent()
    val xTitle = model.axes.x?.label?.takeIf { it.isNotBlank() }
    val yTitle = model.axes.y?.label?.takeIf { it.isNotBlank() }
    val bottomAxis =
        if (model.axes.x?.hidden == true || model.axes.x?.position == "top") null
        else HorizontalAxis.rememberBottom(
            valueFormatter = xValueFormatter,
            itemPlacer = xItemPlacer,
            titleComponent = axisTitleComponent,
            title = { xTitle },
        )
    val topAxis =
        if (model.axes.x?.hidden == true || model.axes.x?.position != "top") null
        else HorizontalAxis.rememberTop(
            valueFormatter = xValueFormatter,
            itemPlacer = xItemPlacer,
            titleComponent = axisTitleComponent,
            title = { xTitle },
        )
    val startAxis =
        if (model.axes.y?.hidden == true || model.axes.y?.position == "trailing") null
        else VerticalAxis.rememberStart(
            valueFormatter = yValueFormatter,
            titleComponent = axisTitleComponent,
            title = { yTitle },
        )
    val endAxis =
        if (model.axes.y?.hidden == true || model.axes.y?.position != "trailing") null
        else VerticalAxis.rememberEnd(
            valueFormatter = yValueFormatter,
            titleComponent = axisTitleComponent,
            title = { yTitle },
        )

    // Honor an explicit y-domain when one is supplied; otherwise add headroom above the
    // data so the topmost line/bar doesn't sit flush against the top edge (matches the
    // horizontal breathing room and SwiftUI's automatic y-scaling).
    val yRangeProvider = remember(model) { chartYRangeProvider(model) }

    val layers = mutableListOf<CartesianLayer<*>>()
    if (prepared.columns.isNotEmpty()) {
        val columns = prepared.columns.map { series ->
            rememberLineComponent(
                fill = Fill(chartSeriesColor(series.colorName, series.autoPaletteIndex)),
                thickness = 14.dp,
            )
        }
        layers.add(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(columns),
                mergeMode = {
                    if (prepared.columnsStacked) ColumnCartesianLayer.MergeMode.Stacked
                    else ColumnCartesianLayer.MergeMode.Grouped()
                },
                rangeProvider = yRangeProvider,
            )
        )
    }
    if (prepared.lines.isNotEmpty()) {
        layers.add(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    prepared.lines.map { it.rememberVicoLine(includeArea = false, includePoints = false) }
                ),
                rangeProvider = yRangeProvider,
            )
        )
    }
    if (prepared.areas.isNotEmpty()) {
        layers.add(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    prepared.areas.map { it.rememberVicoLine(includeArea = true, includePoints = false) }
                ),
                rangeProvider = yRangeProvider,
            )
        )
    }
    if (prepared.points.isNotEmpty()) {
        layers.add(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    prepared.points.map { it.rememberVicoLine(includeArea = false, includePoints = true, lineVisible = false) }
                ),
                rangeProvider = yRangeProvider,
            )
        )
    }
    if (prepared.xRules.isNotEmpty()) {
        layers.add(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    prepared.xRules.map { it.rememberVicoLine(includeArea = false, includePoints = false) }
                ),
                rangeProvider = yRangeProvider,
            )
        )
    }
    prepared.placeholder?.let {
        layers.add(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    it.rememberVicoLine(includeArea = false, includePoints = false, lineVisible = false)
                ),
                rangeProvider = yRangeProvider,
            )
        )
    }

    val ruleDecorations = prepared.rules.map { rule ->
        HorizontalLine(
            y = { rule.y },
            line = rememberLineComponent(
                fill = Fill(chartSeriesColor(rule.colorName, rule.autoPaletteIndex)),
                thickness = rule.width.dp,
            ),
            label = { rule.label },
        )
    }

    // Line/area/point layers place their first and last points flush against the plot
    // edges, which pushes the outermost x-axis labels off-canvas. Bars inset themselves,
    // so only the non-column layers need horizontal breathing room.
    val needsEdgeInset = prepared.lines.isNotEmpty() ||
        prepared.areas.isNotEmpty() ||
        prepared.points.isNotEmpty() ||
        prepared.xRules.isNotEmpty()

    val chart = rememberCartesianChart(
        *layers.toTypedArray(),
        startAxis = startAxis,
        bottomAxis = bottomAxis,
        topAxis = topAxis,
        endAxis = endAxis,
        decorations = ruleDecorations,
        layerPadding = {
            if (needsEdgeInset) {
                CartesianLayerPadding(unscalableStart = 24.dp, unscalableEnd = 24.dp)
            } else {
                CartesianLayerPadding()
            }
        },
    )

    val showLegend = !model.legend.hidden && prepared.legendEntries.isNotEmpty()
    Column(modifier = chartModifier) {
        CartesianChartHost(
            chart = chart,
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        if (showLegend) {
            ChartLegend(prepared.legendEntries)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartLegend(entries: List<ChartLegendEntry>) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        entries.forEach { entry ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(chartSymbolShape(entry.symbol))
                        .background(chartSeriesColor(entry.colorName, entry.autoPaletteIndex))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(entry.label)
            }
        }
    }
}

@Composable
private fun ChartSeries.rememberVicoLine(
    includeArea: Boolean,
    includePoints: Boolean,
    lineVisible: Boolean = true,
): LineCartesianLayer.Line {
    val color = chartSeriesColor(colorName, autoPaletteIndex)
    val strokeWidth = if (lineVisible) (style.lineStyle?.width ?: 2.0).toFloat().dp else 0.dp
    val stroke = if (style.lineStyle?.dash?.isNotEmpty() == true) {
        val dash = style.lineStyle.dash
        LineCartesianLayer.LineStroke.Dashed(
            thickness = strokeWidth,
            dashLength = (dash.firstOrNull() ?: 4.0).toFloat().dp,
            gapLength = (dash.getOrNull(1) ?: dash.firstOrNull() ?: 2.0).toFloat().dp,
        )
    } else {
        LineCartesianLayer.LineStroke.Continuous(thickness = strokeWidth)
    }
    val pointProvider = if (includePoints) {
        // Remember one marker component per symbol shape. The set is fixed, so the number of
        // composable calls is stable across recompositions; each point then selects the marker
        // its resolved symbol maps to. Circle is the default when no symbol is set.
        val pointBySymbol = mapOf(
            ChartSymbolName.Circle to rememberSymbolPoint(color, ChartSymbolName.Circle, style.symbolSize),
            ChartSymbolName.Square to rememberSymbolPoint(color, ChartSymbolName.Square, style.symbolSize),
            ChartSymbolName.Diamond to rememberSymbolPoint(color, ChartSymbolName.Diamond, style.symbolSize),
            ChartSymbolName.Triangle to rememberSymbolPoint(color, ChartSymbolName.Triangle, style.symbolSize),
            ChartSymbolName.Plus to rememberSymbolPoint(color, ChartSymbolName.Plus, style.symbolSize),
            ChartSymbolName.Cross to rememberSymbolPoint(color, ChartSymbolName.Cross, style.symbolSize),
        )
        fun pointFor(symbol: ChartSymbolName?) = pointBySymbol.getValue(symbol ?: ChartSymbolName.Circle)

        val distinctSymbols = symbolByX.values.map { it ?: ChartSymbolName.Circle }.distinct()
        if (distinctSymbols.size <= 1) {
            LineCartesianLayer.PointProvider.single(pointFor(distinctSymbols.firstOrNull()))
        } else {
            SymbolPointProvider(
                pointsByX = symbolByX.mapValues { pointFor(it.value) },
                fallback = pointFor(distinctSymbols.first()),
            )
        }
    } else {
        null
    }

    // Mark annotations (e.g. `.annotation("Peak")`) render as data labels positioned beside
    // the mark; points without an annotation resolve to an empty (invisible) label.
    val annotationLabel = if (annotationsByY.isNotEmpty()) rememberTextComponent() else null
    val annotationFormatter = remember(annotationsByY) {
        CartesianValueFormatter { _, value, _ -> annotationsByY[value].orEmpty() }
    }

    return LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(if (lineVisible) color else Color.Transparent)),
        stroke = stroke,
        areaFill = if (includeArea) {
            LineCartesianLayer.AreaFill.single(Fill(applyAlpha(color, 0.4f)))
        } else {
            null
        },
        pointProvider = pointProvider,
        interpolator = style.interpolationMethod.toVicoInterpolator(),
        dataLabel = annotationLabel,
        dataLabelPosition = annotationPosition.toVerticalPosition(),
        dataLabelValueFormatter = annotationFormatter,
    )
}

// SwiftUI's `.symbolSize` is the symbol's ink area (in square points). Convert it to a
// bounding-box side, scaling per shape so different symbols of the same `symbolSize` carry
// roughly equal ink (a diamond's box is larger than a square's for the same area). A null
// size keeps a fixed default box so unsized markers stay uniform.
@Composable
private fun rememberSymbolPoint(
    color: Color,
    symbol: ChartSymbolName,
    symbolSize: Double?,
): LineCartesianLayer.Point {
    val boxSide = if (symbolSize == null) {
        DEFAULT_SYMBOL_BOX_DP
    } else {
        kotlin.math.sqrt(symbolSize.coerceAtLeast(0.0) * symbol.boxAreaFactor())
    }
    return LineCartesianLayer.Point(
        component = rememberShapeComponent(fill = Fill(color), shape = chartSymbolShape(symbol)),
        size = boxSide.toFloat().dp,
    )
}

private const val DEFAULT_SYMBOL_BOX_DP = 8.0
private const val SYMBOL_ARM_RATIO = 0.34

// Ratio of bounding-box area to filled ink area for each symbol, so `side = sqrt(area * factor)`
// yields equal ink across shapes.
private fun ChartSymbolName.boxAreaFactor(): Double =
    when (this) {
        ChartSymbolName.Circle -> 4.0 / Math.PI
        ChartSymbolName.Square -> 1.0
        ChartSymbolName.Diamond, ChartSymbolName.Triangle -> 2.0
        ChartSymbolName.Plus, ChartSymbolName.Cross -> 1.0 / (SYMBOL_ARM_RATIO * (2 - SYMBOL_ARM_RATIO))
    }

private fun ChartAnnotation.Position?.toVerticalPosition(): Position.Vertical =
    when (this) {
        ChartAnnotation.Position.Bottom -> Position.Vertical.Bottom
        ChartAnnotation.Position.Center -> Position.Vertical.Center
        else -> Position.Vertical.Top
    }

// Renders a different marker shape per point within one series, selecting by the entry's x
// value. Used when a series mixes symbols; single-symbol series use PointProvider.single.
private class SymbolPointProvider(
    private val pointsByX: Map<Double, LineCartesianLayer.Point>,
    private val fallback: LineCartesianLayer.Point,
) : LineCartesianLayer.PointProvider {
    override fun getPoint(
        entry: LineCartesianLayerModel.Entry,
        seriesIndex: Int,
        extraStore: ExtraStore,
    ): LineCartesianLayer.Point = pointsByX[entry.x] ?: fallback

    override fun getLargestPoint(extraStore: ExtraStore): LineCartesianLayer.Point = fallback
}

private fun ChartInterpolation?.toVicoInterpolator(): LineCartesianLayer.Interpolator =
    when (this) {
        ChartInterpolation.Monotone,
        ChartInterpolation.Cardinal,
        -> LineCartesianLayer.Interpolator.cubic()
        ChartInterpolation.CatmullRom -> LineCartesianLayer.Interpolator.catmullRom()
        ChartInterpolation.Linear,
        ChartInterpolation.StepStart,
        ChartInterpolation.StepCenter,
        ChartInterpolation.StepEnd,
        null,
        -> LineCartesianLayer.Interpolator.Sharp
    }

// Resolves a series/legend color: palette-by-order when the series has no explicit
// color, otherwise the named/hex color it was given.
@Composable
private fun chartSeriesColor(colorName: String, autoPaletteIndex: Int?): Color =
    if (autoPaletteIndex != null) palette[autoPaletteIndex % palette.size] else chartColor(colorName)

@Composable
private fun chartColor(name: String): Color =
    when {
        name == "clear" -> Color.Transparent
        name.startsWith("#") || namedColors.contains(name) -> Color(ColorComponent(ColorProps(rawValue = name)).color)
        else -> paletteColor(name)
    }

private fun isExplicitChartColor(name: String): Boolean =
    name == "clear" || name.startsWith("#") || namedColors.contains(name)

private fun chartYRangeProvider(model: ChartModel): CartesianLayerRangeProvider {
    val domain = model.scales.y?.domain?.mapNotNull { it.numberOrNull() }
    if (domain != null && domain.size == 2) {
        return CartesianLayerRangeProvider.fixed(
            minY = minOf(domain[0], domain[1]),
            maxY = maxOf(domain[0], domain[1]),
        )
    }
    return HeadroomChartRangeProvider
}

// Extends the auto y-range by a small fraction so the topmost (or bottommost) value isn't
// flush against the plot edge, then rounds to a clean bound so axis labels stay tidy.
private object HeadroomChartRangeProvider : CartesianLayerRangeProvider {
    private const val FRACTION = 0.12

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        if (minY == 0.0 && maxY == 0.0) return 1.0
        if (maxY <= 0.0) return 0.0
        val base = if (minY >= 0.0) 0.0 else minY
        return niceCeil(maxY + (maxY - base) * FRACTION)
    }

    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        if (minY >= 0.0) return 0.0
        val top = if (maxY <= 0.0) 0.0 else maxY
        return -niceCeil(-minY + (top - minY) * FRACTION)
    }

    private fun niceCeil(value: Double): Double {
        if (value <= 0.0) return 0.0
        val base = 10.0.pow(floor(log10(value)) - 1).coerceAtLeast(1.0)
        return ceil(value / base) * base
    }
}

private val namedColors = setOf(
    "red",
    "orange",
    "yellow",
    "green",
    "mint",
    "teal",
    "cyan",
    "blue",
    "indigo",
    "purple",
    "pink",
    "brown",
    "black",
    "white",
    "gray",
    "primary",
    "secondary",
    "tertiary",
    "accent",
    "accentColor",
)

// Mirrors SwiftUI Charts' automatic ordering (blue, green, orange, …).
private val palette = listOf(
    Color(AndroidColor.rgb(50, 120, 247)),
    Color(AndroidColor.rgb(101, 196, 102)),
    Color(AndroidColor.rgb(255, 149, 0)),
    Color(AndroidColor.rgb(126, 87, 194)),
    Color(AndroidColor.rgb(235, 78, 62)),
    Color(AndroidColor.rgb(0, 150, 136)),
)

private fun paletteColor(key: String): Color = palette[kotlin.math.abs(key.hashCode()) % palette.size]

private fun applyAlpha(color: Color, alpha: Float): Color = color.copy(alpha = alpha)

// Maps a SwiftUI chart symbol to a Compose shape used for point-mark markers and legend
// swatches. A null symbol falls back to a circle, matching SwiftUI's default PointMark.
private fun chartSymbolShape(symbol: ChartSymbolName?): Shape =
    when (symbol) {
        ChartSymbolName.Circle, null -> CircleShape
        ChartSymbolName.Square -> RectangleShape
        ChartSymbolName.Diamond -> DiamondSymbolShape
        ChartSymbolName.Triangle -> TriangleSymbolShape
        ChartSymbolName.Plus -> PlusSymbolShape
        ChartSymbolName.Cross -> CrossSymbolShape
    }

private val DiamondSymbolShape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    moveTo(cx, 0f)
    lineTo(size.width, cy)
    lineTo(cx, size.height)
    lineTo(0f, cy)
    close()
}

private val TriangleSymbolShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

// A filled "+" glyph; the cross is the same glyph rotated 45°.
private val PlusSymbolShape = GenericShape { size, _ -> addSymbolGlyph(size, rotationDeg = 0f) }
private val CrossSymbolShape = GenericShape { size, _ -> addSymbolGlyph(size, rotationDeg = 45f) }

// Builds the 12-vertex plus polygon, rotated by [rotationDeg], then uniformly scaled so it
// fits the marker's bounding box (rotation alone would push the cross's arms past the edges).
private fun Path.addSymbolGlyph(size: Size, rotationDeg: Float) {
    val half = minOf(size.width, size.height) / 2f
    val arm = half * SYMBOL_ARM_RATIO.toFloat()
    val base = listOf(
        -arm to -half, arm to -half,
        arm to -arm, half to -arm,
        half to arm, arm to arm,
        arm to half, -arm to half,
        -arm to arm, -half to arm,
        -half to -arm, -arm to -arm,
    )
    val rad = Math.toRadians(rotationDeg.toDouble())
    val cos = kotlin.math.cos(rad).toFloat()
    val sin = kotlin.math.sin(rad).toFloat()
    val rotated = base.map { (px, py) -> (px * cos - py * sin) to (px * sin + py * cos) }
    val maxExtent = rotated.maxOf { (x, y) -> maxOf(abs(x), abs(y)) }
    val scale = if (maxExtent > 0f) half / maxExtent else 1f
    val cx = size.width / 2f
    val cy = size.height / 2f
    rotated.forEachIndexed { index, (px, py) ->
        val x = cx + px * scale
        val y = cy + py * scale
        if (index == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@Composable
private fun RectangleChartView(
    data: PreparedRectangleChartData,
    modifier: Modifier,
    showLegend: Boolean,
) {
    val marks = data.marks.map { mark ->
        RectangleChartDrawMark(
            x = mark.x,
            y = mark.y,
            x2 = mark.x2,
            y2 = mark.y2,
            hasX2 = mark.hasX2,
            hasY2 = mark.hasY2,
            color = chartColor(mark.colorName),
            cornerRadius = mark.cornerRadius,
        )
    }

    Column(modifier = modifier) {
        RectangleChartCanvas(
            data = data,
            marks = marks,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        if (showLegend) {
            ChartLegend(data.legendEntries)
        }
    }
}

@Composable
private fun RectangleChartCanvas(
    data: PreparedRectangleChartData,
    marks: List<RectangleChartDrawMark>,
    modifier: Modifier,
) {
    Canvas(modifier = modifier) {
        val leftPadding = 72.dp.toPx()
        val topPadding = 22.dp.toPx()
        val rightPadding = 18.dp.toPx()
        val bottomPadding = 38.dp.toPx()
        val plotLeft = leftPadding
        val plotTop = topPadding
        val plotRight = size.width - rightPadding
        val plotBottom = size.height - bottomPadding
        val plotWidth = (plotRight - plotLeft).coerceAtLeast(1f)
        val plotHeight = (plotBottom - plotTop).coerceAtLeast(1f)
        val xStart = -0.5
        val xEnd = (data.xLabels.size - 0.5).coerceAtLeast(0.5)
        val yStart = -0.5
        val yEnd = (data.yLabels.size - 0.5).coerceAtLeast(0.5)

        fun xPosition(value: Double): Float =
            (plotLeft + ((value - xStart) / (xEnd - xStart)).toFloat() * plotWidth)
                .coerceIn(plotLeft, plotRight)

        fun yPosition(value: Double): Float =
            (plotTop + ((value - yStart) / (yEnd - yStart)).toFloat() * plotHeight)
                .coerceIn(plotTop, plotBottom)

        val gridColor = Color(AndroidColor.rgb(221, 221, 221))
        val axisColor = Color(AndroidColor.rgb(185, 190, 196))

        data.xLabels.forEach { label ->
            val x = xPosition(label.value)
            drawLine(gridColor, Offset(x, plotTop), Offset(x, plotBottom), strokeWidth = 1.dp.toPx())
        }
        data.yLabels.forEach { label ->
            val y = yPosition(label.value)
            drawLine(gridColor, Offset(plotLeft, y), Offset(plotRight, y), strokeWidth = 1.dp.toPx())
        }

        marks.forEach { mark ->
            val leftValue = if (mark.hasX2) minOf(mark.x, mark.x2) else mark.x - 0.42
            val rightValue = if (mark.hasX2) maxOf(mark.x, mark.x2) else mark.x + 0.42
            val topValue = if (mark.hasY2) minOf(mark.y, mark.y2) else mark.y - 0.42
            val bottomValue = if (mark.hasY2) maxOf(mark.y, mark.y2) else mark.y + 0.42
            val left = xPosition(leftValue)
            val right = xPosition(rightValue)
            val top = yPosition(topValue)
            val bottom = yPosition(bottomValue)
            val radius = (mark.cornerRadius ?: 0.0).toFloat().dp.toPx()

            drawRoundRect(
                color = mark.color,
                topLeft = Offset(left, top),
                size = Size((right - left).coerceAtLeast(1f), (bottom - top).coerceAtLeast(1f)),
                cornerRadius = CornerRadius(radius, radius),
            )
        }

        drawLine(axisColor, Offset(plotLeft, plotTop), Offset(plotLeft, plotBottom), strokeWidth = 1.dp.toPx())
        drawLine(axisColor, Offset(plotLeft, plotBottom), Offset(plotRight, plotBottom), strokeWidth = 1.dp.toPx())

        drawIntoCanvas { canvas ->
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.rgb(32, 32, 32)
                textSize = 12.sp.toPx()
            }
            data.xLabels.forEach { label ->
                labelPaint.textAlign = Paint.Align.CENTER
                canvas.nativeCanvas.drawText(
                    label.label,
                    xPosition(label.value),
                    plotBottom + 22.dp.toPx(),
                    labelPaint,
                )
            }
            data.yLabels.forEach { label ->
                labelPaint.textAlign = Paint.Align.RIGHT
                canvas.nativeCanvas.drawText(
                    label.label,
                    plotLeft - 8.dp.toPx(),
                    yPosition(label.value) + labelPaint.textSize / 3f,
                    labelPaint,
                )
            }
        }
    }
}

private class FiniteHorizontalAxisItemPlacer(values: Collection<Double>) : HorizontalAxis.ItemPlacer {
    private val values = values.distinct().sorted()

    override fun getLabelValues(
        context: CartesianDrawingContext,
        visibleXRange: ClosedFloatingPointRange<Double>,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ): List<Double> = visibleValues(visibleXRange)

    override fun getWidthMeasurementLabelValues(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        fullXRange: ClosedFloatingPointRange<Double>,
    ): List<Double> = values

    override fun getHeightMeasurementLabelValues(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ): List<Double> = values

    override fun getLineValues(
        context: CartesianDrawingContext,
        visibleXRange: ClosedFloatingPointRange<Double>,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ): List<Double> = visibleValues(visibleXRange)

    override fun getStartLayerMargin(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        tickThickness: Float,
        maxLabelWidth: Float,
    ): Float = (maxLabelWidth / 2f - layerDimensions.unscalableStartPadding).coerceAtLeast(0f)

    override fun getEndLayerMargin(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        tickThickness: Float,
        maxLabelWidth: Float,
    ): Float = (maxLabelWidth / 2f - layerDimensions.unscalableEndPadding).coerceAtLeast(0f)

    private fun visibleValues(visibleXRange: ClosedFloatingPointRange<Double>): List<Double> =
        values.filter { value ->
            value >= visibleXRange.start && value <= visibleXRange.endInclusive
        }
}

private data class PreparedChartData(
    val columns: List<ChartSeries>,
    val lines: List<ChartSeries>,
    val areas: List<ChartSeries>,
    val points: List<ChartSeries>,
    val xRules: List<ChartSeries>,
    val rules: List<ChartRule>,
    val xLabels: Map<Double, String>,
    val selectionRows: List<ChartSelectionRow>,
    val xSelectionHandlerId: String?,
    val ySelectionHandlerId: String?,
    val columnsStacked: Boolean,
    val placeholder: ChartSeries?,
    val yLabels: Map<Double, String>,
    val legendEntries: List<ChartLegendEntry>,
) {
    val isEmpty: Boolean
        get() = columns.isEmpty() &&
            lines.isEmpty() &&
            areas.isEmpty() &&
            points.isEmpty() &&
            xRules.isEmpty() &&
            rules.isEmpty()

    fun xLabel(value: Double): String =
        xLabels[value] ?: if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    fun yLabel(value: Double): String =
        yLabels[value] ?: if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    fun selectionModifier(onUiEvent: (UiEvent) -> Unit): Modifier {
        if (selectionRows.isEmpty() || (xSelectionHandlerId == null && ySelectionHandlerId == null)) {
            return Modifier
        }

        return Modifier.pointerInput(selectionRows, xSelectionHandlerId, ySelectionHandlerId) {
            detectTapGestures { offset ->
                val nearest = nearestSelectionRow(offset.x, size.width.toFloat()) ?: return@detectTapGestures
                chartSelectionPayloads(
                    xSelectionHandlerId = xSelectionHandlerId,
                    ySelectionHandlerId = ySelectionHandlerId,
                    xValue = nearest.xValue,
                    yValue = nearest.yValue,
                ).forEach { payload ->
                    onUiEvent(UiEvent.OnChartSelection(payload.handlerId, payload.value))
                }
            }
        }
    }

    private fun nearestSelectionRow(offsetX: Float, width: Float): ChartSelectionRow? {
        if (selectionRows.isEmpty()) return null
        val minX = selectionRows.minOf { it.x }
        val maxX = selectionRows.maxOf { it.x }
        val tapped = if (width <= 0f) {
            0.5
        } else {
            (offsetX / width).coerceIn(0f, 1f).toDouble()
        }
        return selectionRows.minByOrNull { row ->
            abs(normalizedX(row.x, minX, maxX) - tapped)
        }
    }

    private fun normalizedX(value: Double, min: Double, max: Double): Double =
        if (min == max) 0.5 else ((value - min) / (max - min)).coerceIn(0.0, 1.0)

    companion object {
        fun from(model: ChartModel): PreparedChartData {
            val builder = PreparedChartDataBuilder(model)
            return PreparedChartData(
                columns = builder.seriesFor(ChartMarkKind.Bar),
                lines = builder.seriesFor(ChartMarkKind.Line),
                areas = builder.seriesFor(ChartMarkKind.Area),
                points = builder.seriesFor(ChartMarkKind.Point),
                xRules = builder.xRules(),
                rules = builder.rules(),
                xLabels = builder.xLabels(),
                selectionRows = builder.selectionRows(),
                xSelectionHandlerId = model.selection?.x?.onChangeId,
                ySelectionHandlerId = model.selection?.y?.onChangeId,
                columnsStacked = model.marks
                    .filter { it.kind == ChartMarkKind.Bar }
                    .any { it.style.stacking == ChartStacking.Standard },
                placeholder = builder.placeholderSeries(),
                yLabels = builder.yLabels(),
                legendEntries = builder.legendEntries(),
            )
        }
    }
}

private class PreparedChartDataBuilder(
    private val model: ChartModel,
) {
    private val xCategories = linkedMapOf<String, Double>()
    private val xLabels = linkedMapOf<Double, String>()
    private val yCategories = linkedMapOf<String, Double>()
    private val yLabels = linkedMapOf<Double, String>()

    // Series that don't carry an explicit color get palette colors assigned by the
    // order their distinct keys first appear, matching SwiftUI Charts' automatic scale.
    private val autoColorDomain: List<String> =
        model.marks
            .map { colorName(seriesKey(it), it.style) }
            .filterNot { isExplicitChartColor(it) }
            .distinct()

    private fun autoPaletteIndex(colorName: String): Int? =
        autoColorDomain.indexOf(colorName).takeIf { it >= 0 }

    init {
        (model.axes.x?.values as? ChartAxisValues.Values)?.values?.forEach { value ->
            xNumber(value)
        }
        model.scales.x?.domain?.forEach { value ->
            if (value is ChartValue.StringValue || value is ChartValue.BoolValue) {
                xNumber(value)
            }
        }
    }

    fun seriesFor(kind: ChartMarkKind): List<ChartSeries> {
        val grouped = linkedMapOf<String, MutableList<ChartPoint>>()
        val styles = linkedMapOf<String, ChartMarkStyle>()
        model.marks.filter { it.kind == kind }.forEach { mark ->
            val y = yNumber(mark) ?: return@forEach
            val x = xNumber(mark.channels.x?.value)
            val key = seriesKey(mark)
            val markStyle = mark.preparedStyle()
            // A symbol can come from a per-mark `.symbol(...)` modifier or from the chart-level
            // `chartSymbolScale`, keyed by the series' foregroundStyle(by:) value.
            val symbol = markStyle.symbol ?: model.style.symbolScale[key]
            grouped.getOrPut(key) { mutableListOf() }.add(ChartPoint(x, y, symbol, markStyle.annotation))
            styles.putIfAbsent(key, markStyle)
        }
        return grouped.map { (key, points) ->
            val style = styles[key] ?: ChartMarkStyle()
            val name = colorName(key, style)
            ChartSeries(
                key = key,
                colorName = name,
                autoPaletteIndex = autoPaletteIndex(name),
                xValues = points.map { it.x },
                yValues = points.map { it.y },
                style = style,
                symbolByX = points.associate { it.x to it.symbol },
                annotationsByY = points.mapNotNull { p -> p.annotation?.let { p.y to it.text } }.toMap(),
                annotationPosition = points.firstNotNullOfOrNull { it.annotation?.position },
            )
        }
    }

    fun rules(): List<ChartRule> =
        model.marks.filter { it.kind == ChartMarkKind.Rule }.mapNotNull { mark ->
            val y = mark.channels.y?.value?.numberOrNull() ?: return@mapNotNull null
            val name = colorName(seriesKey(mark), mark.style)
            ChartRule(
                y = y,
                colorName = name,
                autoPaletteIndex = autoPaletteIndex(name),
                width = (mark.style.lineStyle?.width ?: 1.0).toFloat(),
                label = mark.style.annotation?.text.orEmpty(),
            )
        }

    fun xRules(): List<ChartSeries> {
        val yRange = yRange()
        return model.marks.filter { it.kind == ChartMarkKind.Rule }.mapNotNull { mark ->
            val x = mark.channels.x?.value ?: return@mapNotNull null
            val name = colorName(seriesKey(mark), mark.style)
            ChartSeries(
                key = "x-rule-${mark.id}",
                colorName = name,
                autoPaletteIndex = autoPaletteIndex(name),
                xValues = listOf(xNumber(x), xNumber(x)),
                yValues = listOf(yRange.first, yRange.second),
                style = mark.style,
            )
        }
    }

    fun xLabels(): Map<Double, String> = xLabels

    fun yLabels(): Map<Double, String> = yLabels

    // SwiftUI shows a legend whenever marks are differentiated by a categorical
    // foregroundStyle(by:) channel. Mirror that: one entry per distinct series key.
    fun legendEntries(): List<ChartLegendEntry> {
        val entries = linkedMapOf<String, ChartLegendEntry>()
        model.marks.forEach { mark ->
            if (mark.style.foregroundStyle !is ChartForegroundStyle.SeriesValue) return@forEach
            val key = seriesKey(mark)
            val name = colorName(key, mark.style)
            entries.putIfAbsent(
                key,
                ChartLegendEntry(
                    label = key,
                    colorName = name,
                    autoPaletteIndex = autoPaletteIndex(name),
                    symbol = mark.style.symbol ?: model.style.symbolScale[key],
                ),
            )
        }
        return entries.values.toList()
    }

    fun selectionRows(): List<ChartSelectionRow> =
        model.marks.filter { it.kind != ChartMarkKind.Rule }.mapNotNull { mark ->
            val xValue = mark.channels.x?.value
            val yValue = mark.channels.y?.value
            if (xValue == null && yValue == null) return@mapNotNull null
            ChartSelectionRow(
                x = xNumber(xValue),
                xValue = xValue,
                yValue = yValue,
            )
        }

    fun placeholderSeries(): ChartSeries? {
        val hasVicoLayer = model.marks.any { it.kind != ChartMarkKind.Rule } || xRules().isNotEmpty()
        if (hasVicoLayer || rules().isEmpty()) return null
        val y = rules().firstOrNull()?.y ?: 0.0
        return ChartSeries(
            key = "_rules",
            colorName = "clear",
            autoPaletteIndex = null,
            xValues = listOf(0.0),
            yValues = listOf(y),
            style = ChartMarkStyle(),
        )
    }

    private fun yRange(): Pair<Double, Double> {
        val values = mutableListOf<Double>()
        model.scales.y?.domain?.mapNotNullTo(values) { it.numberOrNull() }
        model.marks.forEach { mark ->
            if (mark.kind == ChartMarkKind.Rectangle) {
                yNumber(mark)?.let(values::add)
                yNumber(mark.channels.y2?.value)?.let(values::add)
            } else {
                mark.channels.y?.value?.numberOrNull()?.let(values::add)
                mark.channels.y2?.value?.numberOrNull()?.let(values::add)
            }
        }
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 1.0
        if (min == max) return (min - 1.0) to (max + 1.0)
        return min to max
    }

    private fun xNumber(value: ChartValue?): Double {
        val number = value?.numberOrNull()
        if (number != null) {
            xLabels.putIfAbsent(number, value.displayText)
            return number
        }

        val label = value?.displayText ?: xCategories.size.toString()
        val x = xCategories.getOrPut(label) { xCategories.size.toDouble() }
        xLabels.putIfAbsent(x, label)
        return x
    }

    private fun yNumber(mark: ChartMark): Double? =
        if (mark.kind == ChartMarkKind.Rectangle) {
            yNumber(mark.channels.y?.value)
        } else {
            mark.channels.y?.value?.numberOrNull()
        }

    private fun yNumber(value: ChartValue?): Double? {
        val number = value?.numberOrNull()
        if (number != null) {
            yLabels.putIfAbsent(number, value.displayText)
            return number
        }

        val label = value?.displayText ?: return null
        // Vico reserves numeric zero naturally on Y axes; string categories start at 1
        // so the first category does not collapse onto the baseline.
        val y = yCategories.getOrPut(label) { (yCategories.size + 1).toDouble() }
        yLabels.putIfAbsent(y, label)
        return y
    }

    private fun seriesKey(mark: ChartMark): String =
        when (val foreground = mark.style.foregroundStyle) {
            is ChartForegroundStyle.SeriesValue -> foreground.channel.value.displayText
            is ChartForegroundStyle.ColorValue -> foreground.color
            null -> mark.kind.name
        }

    private fun ChartMark.preparedStyle(): ChartMarkStyle =
        if (kind == ChartMarkKind.Rectangle && style.symbolSize == null) {
            style.copy(symbolSize = 48.0)
        } else {
            style
        }

    private fun colorName(key: String, style: ChartMarkStyle): String =
        when (val foreground = style.foregroundStyle) {
            is ChartForegroundStyle.ColorValue -> foreground.color
            is ChartForegroundStyle.SeriesValue -> model.style.foregroundStyleScale[foreground.channel.value.displayText]
                ?: key
            null -> model.style.foregroundStyleScale[key] ?: key
        }
}

private data class PreparedRectangleChartData(
    val marks: List<RectangleChartMark>,
    val xLabels: List<RectangleAxisLabel>,
    val yLabels: List<RectangleAxisLabel>,
    val legendEntries: List<ChartLegendEntry>,
) {
    companion object {
        fun from(model: ChartModel): PreparedRectangleChartData? {
            val rectangleMarks = model.marks.filter { it.kind == ChartMarkKind.Rectangle }
            if (rectangleMarks.isEmpty() || rectangleMarks.size != model.marks.size) return null

            val xCategories = linkedMapOf<String, Double>()
            val yCategories = linkedMapOf<String, Double>()

            fun categoryIndex(value: ChartValue?, categories: LinkedHashMap<String, Double>): Double? {
                val label = value?.displayText ?: return null
                return categories.getOrPut(label) { categories.size.toDouble() }
            }

            val marks = rectangleMarks.mapNotNull { mark ->
                val x = categoryIndex(mark.channels.x?.value, xCategories) ?: return@mapNotNull null
                val y = categoryIndex(mark.channels.y?.value, yCategories) ?: return@mapNotNull null
                val x2 = mark.channels.x2?.value?.let { categoryIndex(it, xCategories) } ?: x
                val y2 = mark.channels.y2?.value?.let { categoryIndex(it, yCategories) } ?: y
                val key = when (val foreground = mark.style.foregroundStyle) {
                    is ChartForegroundStyle.SeriesValue -> foreground.channel.value.displayText
                    is ChartForegroundStyle.ColorValue -> foreground.color
                    null -> mark.kind.name
                }
                val colorName = when (val foreground = mark.style.foregroundStyle) {
                    is ChartForegroundStyle.ColorValue -> foreground.color
                    is ChartForegroundStyle.SeriesValue -> model.style.foregroundStyleScale[foreground.channel.value.displayText]
                        ?: key
                    null -> model.style.foregroundStyleScale[key] ?: key
                }

                RectangleChartMark(
                    x = x,
                    y = y,
                    x2 = x2,
                    y2 = y2,
                    hasX2 = mark.channels.x2 != null,
                    hasY2 = mark.channels.y2 != null,
                    colorName = colorName,
                    cornerRadius = mark.style.cornerRadius,
                )
            }

            if (marks.isEmpty()) return null

            // Heatmap cells are colored explicitly (via foregroundStyleScale), so legend
            // swatches use the same color resolution as the cells (chartColor on the
            // resolved name) rather than palette-by-order.
            val legendEntries = linkedMapOf<String, ChartLegendEntry>()
            rectangleMarks.forEach { mark ->
                val foreground = mark.style.foregroundStyle
                if (foreground !is ChartForegroundStyle.SeriesValue) return@forEach
                val key = foreground.channel.value.displayText
                val colorName = model.style.foregroundStyleScale[key] ?: key
                legendEntries.putIfAbsent(
                    key,
                    ChartLegendEntry(label = key, colorName = colorName, autoPaletteIndex = null),
                )
            }

            return PreparedRectangleChartData(
                marks = marks,
                xLabels = xCategories.map { (label, value) -> RectangleAxisLabel(value, label) },
                yLabels = yCategories.map { (label, value) -> RectangleAxisLabel(value, label) },
                legendEntries = legendEntries.values.toList(),
            )
        }
    }
}

private data class RectangleChartMark(
    val x: Double,
    val y: Double,
    val x2: Double,
    val y2: Double,
    val hasX2: Boolean,
    val hasY2: Boolean,
    val colorName: String,
    val cornerRadius: Double?,
)

private data class RectangleChartDrawMark(
    val x: Double,
    val y: Double,
    val x2: Double,
    val y2: Double,
    val hasX2: Boolean,
    val hasY2: Boolean,
    val color: Color,
    val cornerRadius: Double?,
)

private data class RectangleAxisLabel(
    val value: Double,
    val label: String,
)

private data class ChartPoint(
    val x: Double,
    val y: Double,
    val symbol: ChartSymbolName? = null,
    val annotation: ChartAnnotation? = null,
)

private data class ChartSeries(
    val key: String,
    val colorName: String,
    val autoPaletteIndex: Int?,
    val xValues: List<Double>,
    val yValues: List<Double>,
    val style: ChartMarkStyle,
    // Resolved symbol per x value. A PointMark's symbol can vary point-to-point within a
    // single series (e.g. several `.symbol(...)` marks that share no foregroundStyle).
    val symbolByX: Map<Double, ChartSymbolName?> = emptyMap(),
    // Annotation text keyed by y value, drawn as a data label beside the mark.
    val annotationsByY: Map<Double, String> = emptyMap(),
    val annotationPosition: ChartAnnotation.Position? = null,
)

private data class ChartLegendEntry(
    val label: String,
    val colorName: String,
    val autoPaletteIndex: Int?,
    val symbol: ChartSymbolName? = null,
)

private data class ChartSelectionRow(
    val x: Double,
    val xValue: ChartValue?,
    val yValue: ChartValue?,
)

private fun ChartModel.accessibilityDescription(): String? =
    listOfNotNull(
        accessibility.label.nonBlankOrNull(),
        accessibility.description.nonBlankOrNull(),
        marks.mapNotNull { it.accessibilityDescription() }
            .takeIf { it.isNotEmpty() }
            ?.joinToString("; "),
    )
        .joinToString(". ")
        .nonBlankOrNull()

private fun ChartMark.accessibilityDescription(): String? =
    listOfNotNull(
        accessibility.label.nonBlankOrNull(),
        accessibility.value.nonBlankOrNull(),
        accessibility.description.nonBlankOrNull(),
    )
        .joinToString(", ")
        .nonBlankOrNull()

private fun String?.nonBlankOrNull(): String? =
    this?.takeIf { it.isNotBlank() }

private data class ChartRule(
    val y: Double,
    val colorName: String,
    val autoPaletteIndex: Int?,
    val width: Float,
    val label: String,
)
