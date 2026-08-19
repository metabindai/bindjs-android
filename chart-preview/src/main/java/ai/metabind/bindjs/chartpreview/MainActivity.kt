package ai.metabind.bindjs.chartpreview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.DesignerComponent
import ai.metabind.bindjs.JsRuntimeImpl
import ai.metabind.bindjs.composables.BindJSView
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChartPreviewApp(initialFixtureName = intent.getStringExtra("fixture"))
                }
            }
        }
    }
}

@Composable
private fun ChartPreviewApp(initialFixtureName: String?) {
    val context = LocalContext.current
    val runtime = remember { JsRuntimeImpl.getInstance(context) }
    val initialIndex = remember(initialFixtureName) {
        chartFixtures.indexOfFirst { it.name == initialFixtureName }.coerceAtLeast(0)
    }
    var selectedIndex by remember(initialFixtureName) { mutableIntStateOf(initialIndex) }
    var component by remember { mutableStateOf<BaseComponent<*>?>(null) }
    var version by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var componentsRegistered by remember { mutableStateOf(false) }
    val fixture = chartFixtures[selectedIndex]

    LaunchedEffect(Unit) {
        runtime.awaitReady()
        for (item in chartFixtures) {
            runtime.setComponents(
                DesignerComponent(
                    name = item.name,
                    content = item.componentSource()
                )
            )
        }
        componentsRegistered = true
    }

    suspend fun render() {
        try {
            runtime.awaitReady()
            runtime.willRender()
            component = runtime.callComponent(fixture.name)
            version += 1
            error = null
        } catch (throwable: Throwable) {
            component = null
            error = throwable.message ?: throwable::class.java.simpleName
        }
    }

    LaunchedEffect(selectedIndex, componentsRegistered) {
        if (!componentsRegistered) return@LaunchedEffect
        render()
    }

    // Interactive fixtures need the same loop the SDK hosts run: an event reaches its JS
    // handler, the handler's `setState` asks for a re-render, and the component body runs
    // again. Without it a fixture like `selection-mark-churn` can never change what it
    // draws, and the renderer only ever sees one shape of chart.
    val scope = rememberCoroutineScope()
    DisposableEffect(runtime, selectedIndex) {
        runtime.setOnRerenderRequested { scope.launch { render() } }
        onDispose { runtime.setOnRerenderRequested(null) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Header(fixture = fixture)
        FixtureTabs(
            selectedIndex = selectedIndex,
            onSelect = { selectedIndex = it }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .background(Color.White, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                error != null -> Text("Render error: $error", color = MaterialTheme.colorScheme.error)
                component == null -> CircularProgressIndicator()
                else -> BindJSView(
                    jsRuntime = runtime,
                    component = component!!,
                    version = version,
                    onUiEvent = { event ->
                        scope.launch {
                            when (event) {
                                is UiEvent.OnChartSelection ->
                                    runtime.callEventHandler(event.handlerId, arrayOf(event.value))
                                is UiEvent.OnTap ->
                                    runtime.callEventHandler(event.handlerId)
                                else -> Unit
                            }
                        }
                    },
                )
            }
        }
        Text(
            text = "Rendered through JsRuntimeImpl, Android Compose, and Vico.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun Header(fixture: ChartFixture) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "BindJS Chart Preview",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = fixture.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = fixture.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FixtureTabs(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chartFixtures.forEachIndexed { index, fixture ->
            AssistChip(
                onClick = { onSelect(index) },
                label = { Text(fixture.name) },
                enabled = true,
                leadingIcon = {
                    if (index == selectedIndex) {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(8.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                        )
                    }
                }
            )
        }
    }
}

private data class ChartFixture(
    val name: String,
    val description: String,
    val source: String,
) {
    fun componentSource(): String =
        """
        const metadata = {
          title: "Chart fixture: $name",
          description: ${description.jsonString()},
        };
        const body = () => ($source).frame({ height: 320 });
        const previews = [Self().previewName("Default")];
        exports.default = defineComponent({ metadata, body, previews });
        """.trimIndent()
}

private fun String.jsonString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

private val chartFixtures = listOf(
    ChartFixture(
        name = "bar-single-series",
        description = "Single-series bar chart with visible axes.",
        source = "Chart({}, [ForEach([{ month: 'Jan', value: 12 }, { month: 'Feb', value: 18 }], row => BarMark({ x: { value: row.month, label: 'Month' }, y: { value: row.value, label: 'Revenue' } }))]).chartXAxisLabel('Month').chartYAxisLabel('Revenue')"
    ),
    ChartFixture(
        name = "bar-multi-series",
        description = "Grouped bar data colored by series.",
        source = "Chart({}, [ForEach([{ month: 'Jan', region: 'North', value: 12 }, { month: 'Jan', region: 'South', value: 9 }, { month: 'Feb', region: 'North', value: 18 }, { month: 'Feb', region: 'South', value: 15 }], row => BarMark({ x: { value: row.month, label: 'Month' }, y: { value: row.value, label: 'Revenue' }, stacking: 'unstacked' }).foregroundStyle({ by: { value: row.region, label: 'Region' } }))]).chartForegroundStyleScale({ North: 'blue', South: 'green' })"
    ),
    ChartFixture(
        name = "bar-stacked",
        description = "Stacked bar data colored by series.",
        source = "Chart({}, [ForEach([{ month: 'Jan', region: 'North', value: 12 }, { month: 'Jan', region: 'South', value: 9 }, { month: 'Feb', region: 'North', value: 18 }, { month: 'Feb', region: 'South', value: 15 }], row => BarMark({ x: { value: row.month }, y: { value: row.value }, stacking: 'standard' }).foregroundStyle({ by: row.region }))])"
    ),
    ChartFixture(
        name = "line-single",
        description = "Single-series line chart over date values.",
        source = "Chart({}, [ForEach([{ date: '2026-01-01', value: 12 }, { date: '2026-02-01', value: 15 }, { date: '2026-03-01', value: 21 }], row => LineMark({ x: { value: row.date, label: 'Date' }, y: { value: row.value, label: 'Value' } }))]).chartXScale({ type: 'date' })"
    ),
    ChartFixture(
        name = "line-multi-series",
        description = "Multi-series line chart using foregroundStyle({ by }).",
        source = "Chart({}, [ForEach([{ date: '2026-01-01', region: 'North', value: 12 }, { date: '2026-02-01', region: 'North', value: 15 }, { date: '2026-01-01', region: 'South', value: 8 }, { date: '2026-02-01', region: 'South', value: 13 }], row => LineMark({ x: { value: row.date }, y: { value: row.value } }).foregroundStyle({ by: row.region }))]).chartXScale({ type: 'date' })"
    ),
    ChartFixture(
        name = "line-with-points",
        description = "Layered LineMark and PointMark.",
        source = "Chart({}, [ForEach([{ month: 'Jan', value: 12 }, { month: 'Feb', value: 18 }, { month: 'Mar', value: 14 }], row => Group([LineMark({ x: { value: row.month }, y: { value: row.value } }), PointMark({ x: { value: row.month }, y: { value: row.value } })]))])"
    ),
    ChartFixture(
        name = "area-stacked",
        description = "Stacked AreaMark series.",
        source = "Chart({}, [ForEach([{ month: 'Jan', region: 'North', value: 12 }, { month: 'Jan', region: 'South', value: 9 }, { month: 'Feb', region: 'North', value: 18 }, { month: 'Feb', region: 'South', value: 15 }], row => AreaMark({ x: { value: row.month }, y: { value: row.value }, stacking: 'standard' }).foregroundStyle({ by: row.region }))])"
    ),
    ChartFixture(
        name = "line-with-rule",
        description = "Line chart with a y-value reference rule.",
        source = "Chart({}, [ForEach([{ month: 'Jan', value: 12 }, { month: 'Feb', value: 18 }, { month: 'Mar', value: 14 }], row => LineMark({ x: { value: row.month }, y: { value: row.value } })), RuleMark({ y: { value: 15, label: 'Average' } }).foregroundStyle(Color('red')).lineStyle({ dash: [4, 2] })])"
    ),
    ChartFixture(
        name = "hidden-axis",
        description = "Line chart with hidden x-axis.",
        source = "Chart({}, [ForEach([{ month: 'Jan', value: 12 }, { month: 'Feb', value: 18 }], row => LineMark({ x: { value: row.month }, y: { value: row.value } }))]).chartXAxis({ hidden: true })"
    ),
    ChartFixture(
        name = "custom-domain",
        description = "Bar chart with an explicit y-domain.",
        source = "Chart({}, [ForEach([{ month: 'Jan', value: 42 }, { month: 'Feb', value: 64 }], row => BarMark({ x: { value: row.month }, y: { value: row.value } }))]).chartYScale({ domain: [0, 100] })"
    ),
    ChartFixture(
        name = "legend-hidden",
        description = "Multi-series chart with legend hidden.",
        source = "Chart({}, [ForEach([{ month: 'Jan', region: 'North', value: 12 }, { month: 'Jan', region: 'South', value: 9 }], row => BarMark({ x: { value: row.month }, y: { value: row.value } }).foregroundStyle({ by: row.region }))]).chartLegend({ hidden: true })"
    ),
    ChartFixture(
        name = "interpolation-monotone",
        description = "Line chart using monotone interpolation.",
        source = "Chart({}, [ForEach([{ month: 'Jan', value: 12 }, { month: 'Feb', value: 18 }, { month: 'Mar', value: 14 }], row => LineMark({ x: { value: row.month }, y: { value: row.value } }).interpolationMethod('monotone'))])"
    ),
    ChartFixture(
        name = "accessibility-labeled",
        description = "Chart and mark accessibility labels.",
        source = "Chart({}, [BarMark({ x: { value: 'Jan' }, y: { value: 12 } }).accessibilityLabel('January revenue')]).accessibilityLabel('Revenue by month').accessibilityHint('Bar chart of monthly revenue')"
    ),
    ChartFixture(
        name = "x-rule-reference",
        description = "Tier 2A x-value reference rule.",
        source = "Chart({}, [BarMark({ x: { value: 'Jan' }, y: { value: 12 } }), BarMark({ x: { value: 'Feb' }, y: { value: 18 } }), RuleMark({ x: { value: 'Feb', label: 'Release' } }).foregroundStyle(Color('red')).lineStyle({ dash: [4, 2] })])"
    ),
    ChartFixture(
        name = "heatmap-cells",
        description = "Tier 2A RectangleMark heatmap cells.",
        source = "Chart({}, [RectangleMark({ x: { value: 'Jan', label: 'Month' }, y: { value: 'North', label: 'Region' } }).foregroundStyle({ by: { value: 'High', label: 'Intensity' } }), RectangleMark({ x: { value: 'Feb', label: 'Month' }, y: { value: 'South', label: 'Region' } }).foregroundStyle({ by: { value: 'Low', label: 'Intensity' } })]).chartForegroundStyleScale({ High: 'red', Low: 'blue' })"
    ),
    ChartFixture(
        name = "rectangle-ranges",
        description = "Tier 2A RectangleMark range rectangles with optional secondary channels.",
        source = "Chart({}, [RectangleMark({ x: { value: 'Jan', label: 'Start' }, x2: { value: 'Feb', label: 'End' }, y: { value: 'North', label: 'Start region' }, y2: { value: 'South', label: 'End region' } }).foregroundStyle(Color('blue')), RectangleMark({ x: { value: 'Feb', label: 'Start' }, x2: { value: 'Mar', label: 'End' }, y: { value: 'South', label: 'Start region' }, y2: { value: 'West', label: 'End region' } }).foregroundStyle(Color('green'))])"
    ),
    ChartFixture(
        name = "axis-explicit-values",
        description = "Tier 2A explicit axis values and top axis position.",
        source = "Chart({}, [ForEach([{ month: 'Jan', value: 12 }, { month: 'Feb', value: 18 }, { month: 'Mar', value: 14 }], row => LineMark({ x: { value: row.month, label: 'Month' }, y: { value: row.value, label: 'Revenue' } }))]).chartXAxis({ values: ['Jan', 'Feb', 'Mar'], position: 'top' })"
    ),
    ChartFixture(
        name = "axis-formatter-currency",
        description = "Tier 2A declarative currency formatter metadata.",
        source = "Chart({}, [BarMark({ x: { value: 'Jan' }, y: { value: 1200 } }), BarMark({ x: { value: 'Feb' }, y: { value: 1800 } })]).chartYAxis({ formatter: { style: 'currency', currency: 'USD', maximumFractionDigits: 0 } })"
    ),
    ChartFixture(
        name = "axis-grid-tick-hidden",
        description = "Tier 2A declarative axis label, tick, and grid visibility.",
        source = "Chart({}, [LineMark({ x: { value: 'Jan' }, y: { value: 12 } }), LineMark({ x: { value: 'Feb' }, y: { value: 18 } })]).chartXAxis({ ticksHidden: true, gridHidden: true }).chartYAxis({ labelsHidden: true })"
    ),
    ChartFixture(
        name = "point-symbols",
        description = "Tier 2A point symbols, symbol size, annotation, and symbol scale.",
        source = "Chart({}, [PointMark({ x: { value: 'Jan', label: 'Month' }, y: { value: 12, label: 'Revenue' } }).foregroundStyle({ by: { value: 'North', label: 'Region' } }).symbol('diamond').symbolSize(96).annotation({ text: 'Peak', position: 'top' })]).chartSymbolScale({ North: 'diamond' })"
    ),
    ChartFixture(
        name = "symbol-scale-series",
        description = "Tier 2A finite portable symbol scale across all supported symbols.",
        source = "Chart({}, [ForEach([{ month: 'Jan', region: 'circle', value: 10 }, { month: 'Feb', region: 'square', value: 12 }, { month: 'Mar', region: 'diamond', value: 14 }, { month: 'Apr', region: 'triangle', value: 16 }, { month: 'May', region: 'plus', value: 18 }, { month: 'Jun', region: 'cross', value: 20 }], row => PointMark({ x: { value: row.month }, y: { value: row.value } }).foregroundStyle({ by: { value: row.region, label: 'Symbol' } }))]).chartSymbolScale({ circle: 'circle', square: 'square', diamond: 'diamond', triangle: 'triangle', plus: 'plus', cross: 'cross' })"
    ),
    ChartFixture(
        name = "mark-text-annotation",
        description = "Tier 2A text-only mark annotation.",
        source = "Chart({}, [LineMark({ x: { value: 'Jan' }, y: { value: 12 } }), PointMark({ x: { value: 'Feb' }, y: { value: 18 } }).annotation({ text: 'Peak', position: 'top' })])"
    ),
    ChartFixture(
        name = "x-selection-controlled",
        description = "Tier 2A controlled x-axis selection bridge metadata.",
        source = "Chart({}, [PointMark({ x: { value: 'Jan', label: 'Month' }, y: { value: 12, label: 'Revenue' } }), PointMark({ x: { value: 'Feb', label: 'Month' }, y: { value: 18, label: 'Revenue' } })]).chartXSelection({ value: 'Jan', onChange: value => value })"
    ),
    // Selecting a point makes this chart emit marks it did not have before, which is what real
    // components do to draw a selection highlight. It changes both counts the renderer is
    // sensitive to: how many Vico *layers* the chart has (a point layer appears) and how many
    // *series* a layer holds (a second, red line series appears). Both used to shift Compose's
    // remembered values out from under each other. Drag across the plot to exercise it. The rule
    // is a decoration rather than a layer, so it spans the full plot height and adds no layer.
    ChartFixture(
        name = "selection-mark-churn",
        description = "Selection adds a series, a rule, and a point, changing counts per render.",
        source = "(() => { const rows = [{ month: 'Jan', value: 12 }, { month: 'Feb', value: 18 }, { month: 'Mar', value: 14 }]; const [selected, setSelected] = useState(null); const marks = [ForEach(rows, row => LineMark({ x: { value: row.month }, y: { value: row.value } }))]; const hit = rows.find(row => row.month === selected); if (hit) { marks.push(ForEach(rows, row => LineMark({ x: { value: row.month }, y: { value: row.value * 0.5 } }).foregroundStyle(Color('red')))); marks.push(RuleMark({ x: { value: hit.month } }).foregroundStyle(Color('red'))); marks.push(PointMark({ x: { value: hit.month }, y: { value: hit.value } }).foregroundStyle(Color('red'))); } return Chart({}, marks).chartXSelection({ value: selected, onChange: value => setSelected(value) }); })()"
    ),
    ChartFixture(
        name = "pie-basic",
        description = "Tier 2B basic pie chart with literal slice values.",
        source = "PieChart({}, [PieSliceMark({ id: 'product', value: 45, label: 'Product' }), PieSliceMark({ id: 'services', value: 35, label: 'Services' }), PieSliceMark({ id: 'support', value: 20, label: 'Support' })])"
    ),
    ChartFixture(
        name = "pie-color-scale",
        description = "Tier 2B pie chart using foregroundStyle series keys and a color scale.",
        source = "PieChart({}, [PieSliceMark({ id: 'product', value: 45, label: 'Product' }).foregroundStyle({ by: 'Product' }), PieSliceMark({ id: 'services', value: 35, label: 'Services' }).foregroundStyle({ by: 'Services' }), PieSliceMark({ id: 'support', value: 20, label: 'Support' }).foregroundStyle({ by: 'Support' })]).chartForegroundStyleScale({ Product: 'blue', Services: 'green', Support: 'orange' })"
    ),
    ChartFixture(
        name = "donut-basic",
        description = "Tier 2B donut chart using normalized innerRadius.",
        source = "PieChart({ innerRadius: 0.55 }, [PieSliceMark({ id: 'north', value: 40, label: 'North' }), PieSliceMark({ id: 'south', value: 25, label: 'South' }), PieSliceMark({ id: 'west', value: 35, label: 'West' })])"
    ),
    ChartFixture(
        name = "pie-legend-hidden",
        description = "Tier 2B pie chart with legend hidden.",
        source = "PieChart({}, [PieSliceMark({ id: 'north', value: 40, label: 'North' }).foregroundStyle({ by: 'North' }), PieSliceMark({ id: 'south', value: 60, label: 'South' }).foregroundStyle({ by: 'South' })]).chartForegroundStyleScale({ North: 'blue', South: 'green' }).chartLegend({ hidden: true })"
    ),
    ChartFixture(
        name = "pie-accessibility-labeled",
        description = "Tier 2B pie chart and slice accessibility metadata.",
        source = "PieChart({}, [PieSliceMark({ id: 'product', value: 45, label: 'Product' }).accessibilityLabel('Product revenue share').accessibilityValue('45 percent'), PieSliceMark({ id: 'services', value: 35, label: 'Services' }).accessibilityLabel('Services revenue share').accessibilityValue('35 percent')]).accessibilityLabel('Revenue share').accessibilityHint('Pie chart of revenue by business line')"
    ),
    ChartFixture(
        name = "pie-selection-controlled",
        description = "Tier 2B controlled single-slice pie selection bridge metadata.",
        source = "PieChart({}, [PieSliceMark({ id: 'product', value: 45, label: 'Product' }), PieSliceMark({ id: 'services', value: 35, label: 'Services' }), PieSliceMark({ id: 'support', value: 20, label: 'Support' })]).chartSelection({ value: 'product', onChange: value => value })"
    ),
)
