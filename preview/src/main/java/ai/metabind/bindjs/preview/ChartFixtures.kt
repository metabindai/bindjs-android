package ai.metabind.bindjs.preview

/** Chart fixtures. Every chart is given a fixed height, since a chart has none of its own. */
private fun chart(name: String, description: String, source: String) = PreviewFixture(
    name = name,
    description = description,
    section = PreviewSection.Charts,
    source = source,
    fixedHeight = 320,
)

val chartFixtures = listOf(
    chart(
        name = "bar-single-series",
        description = "Single-series bar chart with visible axes.",
        source = "Chart({}, [ForEach([{ month: 'Jan', value: 12 }, { month: 'Feb', value: 18 }], row => BarMark({ x: { value: row.month, label: 'Month' }, y: { value: row.value, label: 'Revenue' } }))]).chartXAxisLabel('Month').chartYAxisLabel('Revenue')"
    ),
    chart(
        name = "bar-multi-series",
        description = "Grouped bar data colored by series.",
        source = "Chart({}, [ForEach([{ month: 'Jan', region: 'North', value: 12 }, { month: 'Jan', region: 'South', value: 9 }, { month: 'Feb', region: 'North', value: 18 }, { month: 'Feb', region: 'South', value: 15 }], row => BarMark({ x: { value: row.month, label: 'Month' }, y: { value: row.value, label: 'Revenue' }, stacking: 'unstacked' }).foregroundStyle({ by: { value: row.region, label: 'Region' } }))]).chartForegroundStyleScale({ North: 'blue', South: 'green' })"
    ),
    chart(
        name = "bar-stacked",
        description = "Stacked bar data colored by series.",
        source = "Chart({}, [ForEach([{ month: 'Jan', region: 'North', value: 12 }, { month: 'Jan', region: 'South', value: 9 }, { month: 'Feb', region: 'North', value: 18 }, { month: 'Feb', region: 'South', value: 15 }], row => BarMark({ x: { value: row.month }, y: { value: row.value }, stacking: 'standard' }).foregroundStyle({ by: row.region }))])"
    ),
    chart(
        name = "line-single",
        description = "Single-series line chart over date values.",
        source = "Chart({}, [ForEach([{ date: '2026-01-01', value: 12 }, { date: '2026-02-01', value: 15 }, { date: '2026-03-01', value: 21 }], row => LineMark({ x: { value: row.date, label: 'Date' }, y: { value: row.value, label: 'Value' } }))]).chartXScale({ type: 'date' })"
    ),
    chart(
        name = "line-multi-series",
        description = "Multi-series line chart using foregroundStyle({ by }).",
        source = "Chart({}, [ForEach([{ date: '2026-01-01', region: 'North', value: 12 }, { date: '2026-02-01', region: 'North', value: 15 }, { date: '2026-01-01', region: 'South', value: 8 }, { date: '2026-02-01', region: 'South', value: 13 }], row => LineMark({ x: { value: row.date }, y: { value: row.value } }).foregroundStyle({ by: row.region }))]).chartXScale({ type: 'date' })"
    ),
    chart(
        name = "line-with-points",
        description = "Layered LineMark and PointMark.",
        source = "Chart({}, [ForEach([{ month: 'Jan', value: 12 }, { month: 'Feb', value: 18 }, { month: 'Mar', value: 14 }], row => Group([LineMark({ x: { value: row.month }, y: { value: row.value } }), PointMark({ x: { value: row.month }, y: { value: row.value } })]))])"
    ),
    chart(
        name = "area-stacked",
        description = "Stacked AreaMark series.",
        source = "Chart({}, [ForEach([{ month: 'Jan', region: 'North', value: 12 }, { month: 'Jan', region: 'South', value: 9 }, { month: 'Feb', region: 'North', value: 18 }, { month: 'Feb', region: 'South', value: 15 }], row => AreaMark({ x: { value: row.month }, y: { value: row.value }, stacking: 'standard' }).foregroundStyle({ by: row.region }))])"
    ),
    chart(
        name = "line-with-rule",
        description = "Line chart with a y-value reference rule.",
        source = "Chart({}, [ForEach([{ month: 'Jan', value: 12 }, { month: 'Feb', value: 18 }, { month: 'Mar', value: 14 }], row => LineMark({ x: { value: row.month }, y: { value: row.value } })), RuleMark({ y: { value: 15, label: 'Average' } }).foregroundStyle(Color('red')).lineStyle({ dash: [4, 2] })])"
    ),
    chart(
        name = "hidden-axis",
        description = "Line chart with hidden x-axis.",
        source = "Chart({}, [ForEach([{ month: 'Jan', value: 12 }, { month: 'Feb', value: 18 }], row => LineMark({ x: { value: row.month }, y: { value: row.value } }))]).chartXAxis({ hidden: true })"
    ),
    chart(
        name = "custom-domain",
        description = "Bar chart with an explicit y-domain.",
        source = "Chart({}, [ForEach([{ month: 'Jan', value: 42 }, { month: 'Feb', value: 64 }], row => BarMark({ x: { value: row.month }, y: { value: row.value } }))]).chartYScale({ domain: [0, 100] })"
    ),
    chart(
        name = "legend-hidden",
        description = "Multi-series chart with legend hidden.",
        source = "Chart({}, [ForEach([{ month: 'Jan', region: 'North', value: 12 }, { month: 'Jan', region: 'South', value: 9 }], row => BarMark({ x: { value: row.month }, y: { value: row.value } }).foregroundStyle({ by: row.region }))]).chartLegend({ hidden: true })"
    ),
    chart(
        name = "interpolation-monotone",
        description = "Line chart using monotone interpolation.",
        source = "Chart({}, [ForEach([{ month: 'Jan', value: 12 }, { month: 'Feb', value: 18 }, { month: 'Mar', value: 14 }], row => LineMark({ x: { value: row.month }, y: { value: row.value } }).interpolationMethod('monotone'))])"
    ),
    chart(
        name = "accessibility-labeled",
        description = "Chart and mark accessibility labels.",
        source = "Chart({}, [BarMark({ x: { value: 'Jan' }, y: { value: 12 } }).accessibilityLabel('January revenue')]).accessibilityLabel('Revenue by month').accessibilityHint('Bar chart of monthly revenue')"
    ),
    chart(
        name = "x-rule-reference",
        description = "Tier 2A x-value reference rule.",
        source = "Chart({}, [BarMark({ x: { value: 'Jan' }, y: { value: 12 } }), BarMark({ x: { value: 'Feb' }, y: { value: 18 } }), RuleMark({ x: { value: 'Feb', label: 'Release' } }).foregroundStyle(Color('red')).lineStyle({ dash: [4, 2] })])"
    ),
    chart(
        name = "heatmap-cells",
        description = "Tier 2A RectangleMark heatmap cells.",
        source = "Chart({}, [RectangleMark({ x: { value: 'Jan', label: 'Month' }, y: { value: 'North', label: 'Region' } }).foregroundStyle({ by: { value: 'High', label: 'Intensity' } }), RectangleMark({ x: { value: 'Feb', label: 'Month' }, y: { value: 'South', label: 'Region' } }).foregroundStyle({ by: { value: 'Low', label: 'Intensity' } })]).chartForegroundStyleScale({ High: 'red', Low: 'blue' })"
    ),
    chart(
        name = "rectangle-ranges",
        description = "Tier 2A RectangleMark range rectangles with optional secondary channels.",
        source = "Chart({}, [RectangleMark({ x: { value: 'Jan', label: 'Start' }, x2: { value: 'Feb', label: 'End' }, y: { value: 'North', label: 'Start region' }, y2: { value: 'South', label: 'End region' } }).foregroundStyle(Color('blue')), RectangleMark({ x: { value: 'Feb', label: 'Start' }, x2: { value: 'Mar', label: 'End' }, y: { value: 'South', label: 'Start region' }, y2: { value: 'West', label: 'End region' } }).foregroundStyle(Color('green'))])"
    ),
    chart(
        name = "axis-explicit-values",
        description = "Tier 2A explicit axis values and top axis position.",
        source = "Chart({}, [ForEach([{ month: 'Jan', value: 12 }, { month: 'Feb', value: 18 }, { month: 'Mar', value: 14 }], row => LineMark({ x: { value: row.month, label: 'Month' }, y: { value: row.value, label: 'Revenue' } }))]).chartXAxis({ values: ['Jan', 'Feb', 'Mar'], position: 'top' })"
    ),
    chart(
        name = "axis-formatter-currency",
        description = "Tier 2A declarative currency formatter metadata.",
        source = "Chart({}, [BarMark({ x: { value: 'Jan' }, y: { value: 1200 } }), BarMark({ x: { value: 'Feb' }, y: { value: 1800 } })]).chartYAxis({ formatter: { style: 'currency', currency: 'USD', maximumFractionDigits: 0 } })"
    ),
    chart(
        name = "axis-grid-tick-hidden",
        description = "Tier 2A declarative axis label, tick, and grid visibility.",
        source = "Chart({}, [LineMark({ x: { value: 'Jan' }, y: { value: 12 } }), LineMark({ x: { value: 'Feb' }, y: { value: 18 } })]).chartXAxis({ ticksHidden: true, gridHidden: true }).chartYAxis({ labelsHidden: true })"
    ),
    chart(
        name = "point-symbols",
        description = "Tier 2A point symbols, symbol size, annotation, and symbol scale.",
        source = "Chart({}, [PointMark({ x: { value: 'Jan', label: 'Month' }, y: { value: 12, label: 'Revenue' } }).foregroundStyle({ by: { value: 'North', label: 'Region' } }).symbol('diamond').symbolSize(96).annotation({ text: 'Peak', position: 'top' })]).chartSymbolScale({ North: 'diamond' })"
    ),
    chart(
        name = "symbol-scale-series",
        description = "Tier 2A finite portable symbol scale across all supported symbols.",
        source = "Chart({}, [ForEach([{ month: 'Jan', region: 'circle', value: 10 }, { month: 'Feb', region: 'square', value: 12 }, { month: 'Mar', region: 'diamond', value: 14 }, { month: 'Apr', region: 'triangle', value: 16 }, { month: 'May', region: 'plus', value: 18 }, { month: 'Jun', region: 'cross', value: 20 }], row => PointMark({ x: { value: row.month }, y: { value: row.value } }).foregroundStyle({ by: { value: row.region, label: 'Symbol' } }))]).chartSymbolScale({ circle: 'circle', square: 'square', diamond: 'diamond', triangle: 'triangle', plus: 'plus', cross: 'cross' })"
    ),
    chart(
        name = "mark-text-annotation",
        description = "Tier 2A text-only mark annotation.",
        source = "Chart({}, [LineMark({ x: { value: 'Jan' }, y: { value: 12 } }), PointMark({ x: { value: 'Feb' }, y: { value: 18 } }).annotation({ text: 'Peak', position: 'top' })])"
    ),
    chart(
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
    chart(
        name = "selection-mark-churn",
        description = "Selection adds a series, a rule, and a point, changing counts per render.",
        source = "(() => { const rows = [{ month: 'Jan', value: 12 }, { month: 'Feb', value: 18 }, { month: 'Mar', value: 14 }]; const [selected, setSelected] = useState(null); const marks = [ForEach(rows, row => LineMark({ x: { value: row.month }, y: { value: row.value } }))]; const hit = rows.find(row => row.month === selected); if (hit) { marks.push(ForEach(rows, row => LineMark({ x: { value: row.month }, y: { value: row.value * 0.5 } }).foregroundStyle(Color('red')))); marks.push(RuleMark({ x: { value: hit.month } }).foregroundStyle(Color('red'))); marks.push(PointMark({ x: { value: hit.month }, y: { value: hit.value } }).foregroundStyle(Color('red'))); } return Chart({}, marks).chartXSelection({ value: selected, onChange: value => setSelected(value) }); })()"
    ),
    chart(
        name = "pie-basic",
        description = "Tier 2B basic pie chart with literal slice values.",
        source = "PieChart({}, [PieSliceMark({ id: 'product', value: 45, label: 'Product' }), PieSliceMark({ id: 'services', value: 35, label: 'Services' }), PieSliceMark({ id: 'support', value: 20, label: 'Support' })])"
    ),
    chart(
        name = "pie-color-scale",
        description = "Tier 2B pie chart using foregroundStyle series keys and a color scale.",
        source = "PieChart({}, [PieSliceMark({ id: 'product', value: 45, label: 'Product' }).foregroundStyle({ by: 'Product' }), PieSliceMark({ id: 'services', value: 35, label: 'Services' }).foregroundStyle({ by: 'Services' }), PieSliceMark({ id: 'support', value: 20, label: 'Support' }).foregroundStyle({ by: 'Support' })]).chartForegroundStyleScale({ Product: 'blue', Services: 'green', Support: 'orange' })"
    ),
    chart(
        name = "donut-basic",
        description = "Tier 2B donut chart using normalized innerRadius.",
        source = "PieChart({ innerRadius: 0.55 }, [PieSliceMark({ id: 'north', value: 40, label: 'North' }), PieSliceMark({ id: 'south', value: 25, label: 'South' }), PieSliceMark({ id: 'west', value: 35, label: 'West' })])"
    ),
    chart(
        name = "pie-legend-hidden",
        description = "Tier 2B pie chart with legend hidden.",
        source = "PieChart({}, [PieSliceMark({ id: 'north', value: 40, label: 'North' }).foregroundStyle({ by: 'North' }), PieSliceMark({ id: 'south', value: 60, label: 'South' }).foregroundStyle({ by: 'South' })]).chartForegroundStyleScale({ North: 'blue', South: 'green' }).chartLegend({ hidden: true })"
    ),
    chart(
        name = "pie-accessibility-labeled",
        description = "Tier 2B pie chart and slice accessibility metadata.",
        source = "PieChart({}, [PieSliceMark({ id: 'product', value: 45, label: 'Product' }).accessibilityLabel('Product revenue share').accessibilityValue('45 percent'), PieSliceMark({ id: 'services', value: 35, label: 'Services' }).accessibilityLabel('Services revenue share').accessibilityValue('35 percent')]).accessibilityLabel('Revenue share').accessibilityHint('Pie chart of revenue by business line')"
    ),
    chart(
        name = "pie-selection-controlled",
        description = "Tier 2B controlled single-slice pie selection bridge metadata.",
        source = "PieChart({}, [PieSliceMark({ id: 'product', value: 45, label: 'Product' }), PieSliceMark({ id: 'services', value: 35, label: 'Services' }), PieSliceMark({ id: 'support', value: 20, label: 'Support' })]).chartSelection({ value: 'product', onChange: value => value })"
    ),
)
