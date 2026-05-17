package ai.metabind.bindjs.composables.chart

import android.graphics.Color as AndroidColor
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.pie.PieChart as VicoPieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.hasFrame
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.ColorProps
import ai.metabind.bindjs.model.chart.ChartDiagnostic
import ai.metabind.bindjs.model.chart.ChartForegroundStyle
import ai.metabind.bindjs.model.chart.PieChartComponent
import ai.metabind.bindjs.model.chart.PieChartModel
import ai.metabind.bindjs.model.chart.PieSliceMark
import ai.metabind.bindjs.model.modifier.ComponentModifier
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

private const val TAG = "BindJSPieChartView"

@Composable
fun PieChartView(
    component: PieChartComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    val model = remember(component, modifiers) { PieChartCollector.collect(component, modifiers) }
    val prepared = remember(model) { PreparedPieChartData.from(model) }
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

    if (prepared.slices.isEmpty()) {
        Text(
            text = "Unsupported pie chart content",
            modifier = modifiers.buildModifier(onUiEvent),
        )
        return
    }

    val baseModifier = modifiers
        .buildModifier(onUiEvent)
        .then(if (!modifiers.hasFrame()) Modifier.fillMaxWidth().height(240.dp) else Modifier)
        .then(prepared.selectionModifier(model, onUiEvent))
        .then(
            if (accessibilityDescription != null) {
                Modifier.semantics {
                    contentDescription = accessibilityDescription
                }
            } else {
                Modifier
            }
        )

    val modelProducer = remember { PieChartModelProducer() }
    LaunchedEffect(prepared) {
        modelProducer.runTransaction {
            pieSeries {
                series(prepared.slices.map { it.value })
            }
        }
    }

    Column(modifier = baseModifier) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val minDimension = if (maxWidth < maxHeight) maxWidth else maxHeight
            val innerSize = if (prepared.innerRadius > 0f) {
                PieSize.Inner.fixed(minDimension * prepared.innerRadius * 0.5f)
            } else {
                PieSize.Inner.Zero
            }
            val slices = prepared.slices.map { slice ->
                VicoPieChart.Slice(fill = Fill(pieChartColor(slice.colorName)))
            }
            PieChartHost(
                chart = rememberPieChart(
                    sliceProvider = VicoPieChart.SliceProvider.series(slices),
                    spacing = 0.dp,
                    outerSize = PieSize.Outer.Fill,
                    innerSize = innerSize,
                    startAngle = 0f,
                    legend = null,
                ),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!model.legend.hidden) {
            PieLegend(prepared.slices)
        }
    }
}

@Composable
private fun PieLegend(slices: List<PreparedPieSlice>) {
    Column {
        slices.forEach { slice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(pieChartColor(slice.colorName))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(slice.label ?: slice.id)
            }
        }
    }
}

@Composable
private fun pieChartColor(name: String): Color =
    when {
        name == "clear" -> Color.Transparent
        name.startsWith("#") || pieNamedColors.contains(name) -> Color(ColorComponent(ColorProps(rawValue = name)).color)
        else -> piePaletteColor(name)
    }

private val pieNamedColors = setOf(
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

private val piePalette = listOf(
    Color(AndroidColor.rgb(50, 120, 247)),
    Color(AndroidColor.rgb(235, 78, 62)),
    Color(AndroidColor.rgb(101, 196, 102)),
    Color(AndroidColor.rgb(255, 149, 0)),
    Color(AndroidColor.rgb(126, 87, 194)),
    Color(AndroidColor.rgb(0, 150, 136)),
)

private fun piePaletteColor(key: String): Color =
    piePalette[kotlin.math.abs(key.hashCode()) % piePalette.size]

private data class PreparedPieChartData(
    val slices: List<PreparedPieSlice>,
    val innerRadius: Float,
) {
    fun selectionModifier(model: PieChartModel, onUiEvent: (UiEvent) -> Unit): Modifier {
        val selection = model.selection
        if (selection?.onChangeId == null) return Modifier
        return Modifier.pointerInput(slices, selection.onChangeId) {
            detectTapGestures { offset ->
                val sliceId = sliceIdAt(offset, size.width.toFloat(), size.height.toFloat()) ?: return@detectTapGestures
                pieSelectionPayload(selection, sliceId)?.let { payload ->
                    onUiEvent(UiEvent.OnChartSelection(payload.handlerId, payload.value))
                }
            }
        }
    }

    private fun sliceIdAt(offset: Offset, width: Float, height: Float): String? {
        val radius = minOf(width, height) / 2f
        if (radius <= 0f) return null
        val centerX = width / 2f
        val centerY = height / 2f
        val dx = offset.x - centerX
        val dy = offset.y - centerY
        val distance = hypot(dx, dy)
        if (distance > radius || distance < radius * innerRadius) return null

        val normalizedAngle = ((atan2(dy, dx).toDouble() + (2.0 * PI)) % (2.0 * PI)) / (2.0 * PI)
        val total = slices.sumOf { it.value }
        val tappedValue = normalizedAngle * total
        var cursor = 0.0
        slices.forEach { slice ->
            cursor += slice.value
            if (tappedValue <= cursor) return slice.id
        }
        return slices.lastOrNull()?.id
    }

    companion object {
        fun from(model: PieChartModel): PreparedPieChartData =
            PreparedPieChartData(
                slices = model.slices
                    .filter { it.value > 0.0 }
                    .map { slice ->
                        PreparedPieSlice(
                            id = slice.id,
                            value = slice.value,
                            label = slice.label,
                            colorName = colorName(slice, model),
                        )
                    },
                innerRadius = (model.innerRadius ?: 0.0).coerceIn(0.0, 1.0).toFloat(),
            )

        private fun colorName(slice: PieSliceMark, model: PieChartModel): String {
            val fallback = slice.label ?: slice.id
            return when (val foreground = slice.style.foregroundStyle) {
                is ChartForegroundStyle.ColorValue -> foreground.color
                is ChartForegroundStyle.SeriesValue -> model.style.foregroundStyleScale[foreground.channel.value.displayText]
                    ?: foreground.channel.value.displayText
                null -> model.style.foregroundStyleScale[fallback] ?: fallback
            }
        }
    }
}

private data class PreparedPieSlice(
    val id: String,
    val value: Double,
    val label: String?,
    val colorName: String,
)

private fun PieChartModel.accessibilityDescription(): String? =
    listOfNotNull(
        accessibility.label.nonBlankOrNull(),
        accessibility.description.nonBlankOrNull(),
        slices.mapNotNull { it.accessibilityDescription() }
            .takeIf { it.isNotEmpty() }
            ?.joinToString("; "),
    )
        .joinToString(". ")
        .nonBlankOrNull()

private fun PieSliceMark.accessibilityDescription(): String? =
    listOfNotNull(
        accessibility.label.nonBlankOrNull(),
        accessibility.value.nonBlankOrNull(),
        accessibility.description.nonBlankOrNull(),
    )
        .joinToString(", ")
        .nonBlankOrNull()

private fun String?.nonBlankOrNull(): String? =
    this?.takeIf { it.isNotBlank() }
