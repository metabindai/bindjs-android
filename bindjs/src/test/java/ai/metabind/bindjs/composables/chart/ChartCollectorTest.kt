package ai.metabind.bindjs.composables.chart

import ai.metabind.bindjs.GsonProvider
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.ModifiedComponent
import ai.metabind.bindjs.model.ModifierProps
import ai.metabind.bindjs.model.TextComponent
import ai.metabind.bindjs.model.TextComponentProps
import ai.metabind.bindjs.model.chart.BarMarkComponent
import ai.metabind.bindjs.model.chart.ChartComponent
import ai.metabind.bindjs.model.chart.ChartDiagnostic
import ai.metabind.bindjs.model.chart.ChartForegroundStyle
import ai.metabind.bindjs.model.chart.ChartMarkKind
import ai.metabind.bindjs.model.chart.ChartMarkProps
import ai.metabind.bindjs.model.chart.ChartProps
import ai.metabind.bindjs.model.chart.ChartAnnotation
import ai.metabind.bindjs.model.chart.ChartAxisValues
import ai.metabind.bindjs.model.chart.ChartSelectionBinding
import ai.metabind.bindjs.model.chart.ChartStacking
import ai.metabind.bindjs.model.chart.ChartSymbolName
import ai.metabind.bindjs.model.chart.ChartValue
import ai.metabind.bindjs.model.chart.ChartValueFormatter
import ai.metabind.bindjs.model.chart.PieChartComponent
import ai.metabind.bindjs.model.chart.PieChartProps
import ai.metabind.bindjs.model.chart.PieSelectionBinding
import ai.metabind.bindjs.model.chart.PieSliceMarkComponent
import ai.metabind.bindjs.model.chart.PieSliceMarkProps
import ai.metabind.bindjs.model.chart.PointMarkComponent
import ai.metabind.bindjs.model.chart.RectangleMarkComponent
import ai.metabind.bindjs.model.chart.RuleMarkComponent
import ai.metabind.bindjs.model.modifier.AccessibilityHintModifier
import ai.metabind.bindjs.model.modifier.AccessibilityHintModifierProps
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.ForegroundStyleModifier
import ai.metabind.bindjs.model.modifier.ForegroundStyleProps
import ai.metabind.bindjs.model.modifier.chart.AnnotationModifier
import ai.metabind.bindjs.model.modifier.chart.AnnotationModifierProps
import ai.metabind.bindjs.model.modifier.chart.ChartAxisModifierProps
import ai.metabind.bindjs.model.modifier.chart.ChartForegroundStyleScaleModifier
import ai.metabind.bindjs.model.modifier.chart.ChartForegroundStyleScaleProps
import ai.metabind.bindjs.model.modifier.chart.ChartSelectionModifier
import ai.metabind.bindjs.model.modifier.chart.ChartSelectionModifierProps
import ai.metabind.bindjs.model.modifier.chart.ChartSymbolScaleModifier
import ai.metabind.bindjs.model.modifier.chart.ChartSymbolScaleProps
import ai.metabind.bindjs.model.modifier.chart.ChartXAxisModifier
import ai.metabind.bindjs.model.modifier.chart.ChartXSelectionModifier
import ai.metabind.bindjs.model.modifier.chart.ChartYScaleModifier
import ai.metabind.bindjs.model.modifier.chart.ChartYSelectionModifier
import ai.metabind.bindjs.model.modifier.chart.ChartScaleModifierProps
import ai.metabind.bindjs.model.modifier.chart.LineStyleModifier
import ai.metabind.bindjs.model.modifier.chart.LineStyleModifierProps
import ai.metabind.bindjs.model.modifier.chart.SymbolModifier
import ai.metabind.bindjs.model.modifier.chart.SymbolModifierProps
import ai.metabind.bindjs.model.modifier.chart.SymbolSizeModifier
import ai.metabind.bindjs.model.modifier.chart.SymbolSizeModifierProps
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChartCollectorTest {
    @Test
    fun collectsDirectMarks() {
        val chart = ChartComponent(ChartProps(children = listOf(bar("Jan", 12))))

        val model = ChartCollector.collect(chart)

        assertEquals(1, model.marks.size)
        assertEquals(ChartMarkKind.Bar, model.marks[0].kind)
        assertEquals(ChartValue.StringValue("Jan"), model.marks[0].channels.x?.value)
        assertEquals(ChartValue.NumberValue(12.0), model.marks[0].channels.y?.value)
    }

    @Test
    fun collectsTierTwoCartesianMarksAndModifiers() {
        val chart = ChartComponent(
            ChartProps(
                children = listOf(
                    rectangle("Jan", "North", y2 = "South"),
                    modified(
                        AnnotationModifier(AnnotationModifierProps(children = null, text = "Peak", position = "top")),
                        modified(
                            SymbolSizeModifier(SymbolSizeModifierProps(children = null, rawValue = 96.0)),
                            modified(
                                SymbolModifier(SymbolModifierProps(children = null, rawValue = "diamond")),
                                point("Jan", 12),
                            ),
                        ),
                    ),
                    modified(
                        AnnotationModifier(AnnotationModifierProps(children = null, rawValue = "Release", position = "trailing")),
                        rule(x = "Feb"),
                    ),
                )
            )
        )

        val model = ChartCollector.collect(
            chart,
            modifiers = listOf(
                ChartXAxisModifier(
                    ChartAxisModifierProps(
                        children = null,
                        values = listOf("Jan", "Feb", true),
                        position = "top",
                        labelsHidden = true,
                        formatter = mapOf("style" to "number", "maximumFractionDigits" to 0),
                    )
                ),
                ChartSymbolScaleModifier(
                    ChartSymbolScaleProps(children = null, scale = mapOf("North" to "circle", "South" to "square"))
                ),
                ChartXSelectionModifier(
                    ChartSelectionModifierProps(children = null, value = "Jan", onChangeId = "selectMonth")
                ),
                ChartYSelectionModifier(
                    ChartSelectionModifierProps(children = null, value = 12, onChangeId = "selectValue")
                ),
            )
        )

        assertEquals(listOf(ChartMarkKind.Rectangle, ChartMarkKind.Point, ChartMarkKind.Rule), model.marks.map { it.kind })
        assertEquals(ChartValue.StringValue("Jan"), model.marks[0].channels.x?.value)
        assertEquals(ChartValue.StringValue("North"), model.marks[0].channels.y?.value)
        assertEquals(ChartValue.StringValue("South"), model.marks[0].channels.y2?.value)
        assertEquals(ChartSymbolName.Diamond, model.marks[1].style.symbol)
        assertEquals(96.0, model.marks[1].style.symbolSize)
        assertEquals(ChartAnnotation("Peak", ChartAnnotation.Position.Top), model.marks[1].style.annotation)
        assertEquals(ChartValue.StringValue("Feb"), model.marks[2].channels.x?.value)
        assertEquals(ChartAnnotation("Release", ChartAnnotation.Position.Trailing), model.marks[2].style.annotation)
        assertEquals(ChartAxisValues.Values(listOf(ChartValue.StringValue("Jan"), ChartValue.StringValue("Feb"))), model.axes.x?.values)
        assertEquals("top", model.axes.x?.position)
        assertEquals(true, model.axes.x?.labelsHidden)
        assertEquals(ChartValueFormatter.NumberFormatter(maximumFractionDigits = 0), model.axes.x?.formatter)
        assertEquals(mapOf("North" to ChartSymbolName.Circle, "South" to ChartSymbolName.Square), model.style.symbolScale)
        assertEquals(ChartSelectionBinding(ChartValue.StringValue("Jan"), "selectMonth"), model.selection?.x)
        assertEquals(ChartSelectionBinding(ChartValue.NumberValue(12.0), "selectValue"), model.selection?.y)
        assertTrue(model.diagnostics.isEmpty(), model.diagnostics.toString())
    }

    @Test
    fun foldsMarkModifiers() {
        val mark = ModifiedComponent(
            ModifierProps(
                modifier = LineStyleModifier(
                    LineStyleModifierProps(children = null, width = 4.0, dash = listOf(5.0, 2.0))
                ),
                content = listOf(
                    ModifiedComponent(
                        ModifierProps(
                            modifier = ForegroundStyleModifier(
                                ForegroundStyleProps(
                                    children = null,
                                    rawValue = mapOf("by" to mapOf("value" to "North", "label" to "Region")),
                                )
                            ),
                            content = listOf(bar("Jan", 12)),
                            children = null,
                        )
                    )
                ),
                children = null,
            )
        )
        val chart = ChartComponent(ChartProps(children = listOf(mark)))

        val collected = ChartCollector.collect(chart).marks.single()

        assertEquals(listOf(5.0, 2.0), collected.style.lineStyle?.dash)
        val foreground = assertIs<ChartForegroundStyle.SeriesValue>(collected.style.foregroundStyle)
        assertEquals("North", foreground.channel.value.displayText)
        assertEquals("Region", foreground.channel.label)
    }

    @Test
    fun foldsChartLevelModifiers() {
        val chart = ChartComponent(ChartProps(children = listOf(bar("Jan", 12))))

        val model = ChartCollector.collect(
            chart,
            modifiers = listOf(
                ChartXAxisModifier(ChartAxisModifierProps(children = null, rawValue = "hidden")),
                ChartYScaleModifier(ChartScaleModifierProps(children = null, domain = listOf(0, 20))),
                ChartForegroundStyleScaleModifier(ChartForegroundStyleScaleProps(children = null, scale = mapOf("North" to "blue"))),
                AccessibilityHintModifier(AccessibilityHintModifierProps(children = null, rawValue = "Quarterly revenue")),
            )
        )

        assertEquals(true, model.axes.x?.hidden)
        assertEquals(listOf(ChartValue.NumberValue(0.0), ChartValue.NumberValue(20.0)), model.scales.y?.domain)
        assertEquals("blue", model.style.foregroundStyleScale["North"])
        assertEquals("Quarterly revenue", model.accessibility.description)
    }

    @Test
    fun rejectsInvalidScaleDomain() {
        val chart = ChartComponent(ChartProps(children = listOf(bar("Jan", 12))))

        val model = ChartCollector.collect(
            chart,
            modifiers = listOf(ChartYScaleModifier(ChartScaleModifierProps(children = null, domain = listOf(20, 0))))
        )

        assertTrue(model.diagnostics.any {
            it.severity == ChartDiagnostic.Severity.Error &&
                it.message.contains("Invalid chart y-scale domain")
        })
    }

    @Test
    fun rejectsInvalidTierTwoCartesianMarks() {
        val chart = ChartComponent(
            ChartProps(
                children = listOf(
                    rule(x = "Jan", y = 10),
                    RectangleMarkComponent(
                        ChartMarkProps(
                            children = null,
                            x = mapOf("value" to "Jan"),
                        )
                    ),
                )
            )
        )

        val model = ChartCollector.collect(chart)

        assertTrue(model.marks.isEmpty())
        assertTrue(model.diagnostics.any {
            it.severity == ChartDiagnostic.Severity.Error &&
                it.message.contains("RuleMark requires exactly one of x or y")
        })
        assertTrue(model.diagnostics.any {
            it.severity == ChartDiagnostic.Severity.Error &&
                it.message.contains("RectangleMark requires x and y channels")
        })
    }

    @Test
    fun chartSelectionBridgeBuildsPortablePayloads() {
        val payloads = chartSelectionPayloads(
            xSelectionHandlerId = "selectMonth",
            ySelectionHandlerId = "selectValue",
            xValue = ChartValue.StringValue("Feb"),
            yValue = ChartValue.NumberValue(12.0),
        )

        assertEquals(
            listOf(
                ChartSelectionPayload("selectMonth", "Feb"),
                ChartSelectionPayload("selectValue", 12.0),
            ),
            payloads,
        )
    }

    @Test
    fun collectsPieSlicesAndModifiers() {
        val chart = PieChartComponent(
            PieChartProps(
                innerRadius = 1.4,
                children = listOf(
                    modified(
                        AccessibilityHintModifier(
                            AccessibilityHintModifierProps(children = null, rawValue = "Forty percent")
                        ),
                        modified(
                            ForegroundStyleModifier(
                                ForegroundStyleProps(
                                    children = null,
                                    rawValue = mapOf("by" to mapOf("value" to "North", "label" to "Region")),
                                )
                            ),
                            pieSlice(id = "north", value = 40.0, label = "North"),
                        ),
                    ),
                    pieSlice(value = 25.0, label = "South"),
                ),
            )
        )

        val model = PieChartCollector.collect(
            chart,
            modifiers = listOf(
                ChartForegroundStyleScaleModifier(
                    ChartForegroundStyleScaleProps(children = null, scale = mapOf("North" to "blue", "South" to "green"))
                ),
                ChartSelectionModifier(
                    ChartSelectionModifierProps(children = null, value = "north", onChangeId = "selectRegion")
                ),
            )
        )

        assertEquals(1.0, model.innerRadius)
        assertEquals(listOf("north", "PieChart.children[1]"), model.slices.map { it.id })
        assertEquals(40.0, model.slices[0].value)
        assertEquals("North", model.slices[0].label)
        val foreground = assertIs<ChartForegroundStyle.SeriesValue>(model.slices[0].style.foregroundStyle)
        assertEquals("North", foreground.channel.value.displayText)
        assertEquals("Region", foreground.channel.label)
        assertEquals("Forty percent", model.slices[0].accessibility.description)
        assertEquals(mapOf("North" to "blue", "South" to "green"), model.style.foregroundStyleScale)
        assertEquals(PieSelectionBinding("north", "selectRegion"), model.selection)
        assertEquals(emptyList(), model.diagnostics)
    }

    @Test
    fun rejectsPieCartesianChildrenAndModifiers() {
        val chart = PieChartComponent(
            PieChartProps(
                children = listOf(
                    bar("Jan", 12),
                    modified(
                        LineStyleModifier(LineStyleModifierProps(children = null, width = 2.0)),
                        pieSlice(value = 10.0),
                    ),
                ),
            )
        )

        val model = PieChartCollector.collect(
            chart,
            modifiers = listOf(
                ChartXSelectionModifier(
                    ChartSelectionModifierProps(children = null, value = "Jan", onChangeId = "selectMonth")
                )
            )
        )

        assertTrue(model.diagnostics.any { it.severity == ChartDiagnostic.Severity.Error && it.message.contains("Cartesian mark") })
        assertTrue(model.diagnostics.any { it.severity == ChartDiagnostic.Severity.Error && it.message.contains("Cartesian-only mark modifier") })
        assertTrue(model.diagnostics.any { it.severity == ChartDiagnostic.Severity.Error && it.message.contains("use chartSelection") })
    }

    @Test
    fun pieSelectionBridgeBuildsSlicePayload() {
        val payload = pieSelectionPayload(
            selection = PieSelectionBinding(value = "north", onChangeId = "selectRegion"),
            sliceId = "south",
        )

        assertEquals(ChartSelectionPayload("selectRegion", "south"), payload)
    }

    @Test
    fun decodesPieDirectivesThroughGson() {
        val json = """
            {
              "type": "PieChart",
              "props": {
                "innerRadius": 0.5,
                "children": [
                  {
                    "type": "PieSliceMark",
                    "props": {
                      "id": "north",
                      "value": 40,
                      "label": "North",
                      "children": []
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val component = GsonProvider.get().fromJson(json, BaseComponent::class.java)
        val chart = assertIs<PieChartComponent>(component)
        val model = PieChartCollector.collect(chart)

        assertEquals(0.5, model.innerRadius)
        assertEquals("north", model.slices.single().id)
        assertEquals(40.0, model.slices.single().value)
    }

    @Test
    fun rejectsNonMarkChildrenAndChartModifiersOnMarks() {
        val chartLevelModifierOnMark = ModifiedComponent(
            ModifierProps(
                modifier = ChartXAxisModifier(ChartAxisModifierProps(children = null, rawValue = "hidden")),
                content = listOf(bar("Jan", 12)),
                children = null,
            )
        )
        val chart = ChartComponent(
            ChartProps(
                children = listOf(
                    chartLevelModifierOnMark,
                    TextComponent(TextComponentProps(markdown = null, rawValue = "not a mark", children = null)),
                )
            )
        )

        val diagnostics = ChartCollector.collect(chart).diagnostics

        assertTrue(diagnostics.any { it.severity == ChartDiagnostic.Severity.Error && it.message.contains("cannot be attached") })
        assertTrue(diagnostics.any { it.severity == ChartDiagnostic.Severity.Error && it.message.contains("Chart children must be chart marks") })
    }

    @Test
    fun decodesChartDirectivesThroughGson() {
        val json = """
            {
              "type": "Chart",
              "props": {
                "children": [
                  {
                    "type": "BarMark",
                    "props": {
                      "x": { "value": "Jan" },
                      "y": { "value": 12 },
                      "stacking": "unstacked",
                      "children": []
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val component = GsonProvider.get().fromJson(json, BaseComponent::class.java)
        val chart = assertIs<ChartComponent>(component)
        val model = ChartCollector.collect(chart)

        assertEquals(ChartStacking.Unstacked, model.marks.single().style.stacking)
        assertNotNull(model.marks.single().channels.x)
    }

    private fun bar(x: String, y: Int): BarMarkComponent =
        BarMarkComponent(
            ChartMarkProps(
                children = null,
                x = mapOf("value" to x),
                y = mapOf("value" to y),
            )
        )

    private fun point(x: String, y: Int): PointMarkComponent =
        PointMarkComponent(
            ChartMarkProps(
                children = null,
                x = mapOf("value" to x),
                y = mapOf("value" to y),
            )
        )

    private fun rectangle(x: String, y: String, x2: String? = null, y2: String? = null): RectangleMarkComponent =
        RectangleMarkComponent(
            ChartMarkProps(
                children = null,
                x = mapOf("value" to x),
                y = mapOf("value" to y),
                x2 = x2?.let { mapOf("value" to it) },
                y2 = y2?.let { mapOf("value" to it) },
            )
        )

    private fun rule(x: String? = null, y: Int? = null): RuleMarkComponent =
        RuleMarkComponent(
            ChartMarkProps(
                children = null,
                x = x?.let { mapOf("value" to it) },
                y = y?.let { mapOf("value" to it) },
            )
        )

    private fun pieSlice(id: String? = null, value: Double, label: String? = null): PieSliceMarkComponent =
        PieSliceMarkComponent(
            PieSliceMarkProps(
                children = null,
                explicitId = id,
                value = value,
                label = label,
            )
        )

    private fun modified(modifier: ComponentModifier<*>, content: BaseComponent<*>): ModifiedComponent =
        ModifiedComponent(
            ModifierProps(
                modifier = modifier,
                content = listOf(content),
                children = null,
            )
        )
}
