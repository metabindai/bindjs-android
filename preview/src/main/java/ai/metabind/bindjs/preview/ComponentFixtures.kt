package ai.metabind.bindjs.preview

/**
 * Component fixtures, one per BindJS component worth eyeballing. Add a new component by
 * appending a [component] entry here; the Components list picks it up.
 */
private fun component(name: String, description: String, source: String) = PreviewFixture(
    name = name,
    description = description,
    section = PreviewSection.Components,
    source = source,
)

private const val PACIFICO_URL =
    "https://raw.githubusercontent.com/google/fonts/main/ofl/pacifico/Pacifico-Regular.ttf"

val componentFixtures = listOf(
    component(
        name = "Slider",
        description = "A stepped slider with value labels, a continuous 0..1 slider, and a read-only one.",
        source = """
        (() => {
          const [volume, setVolume] = useState(40);
          const [opacity, setOpacity] = useState(0.5);
          return VStack({ alignment: 'leading', spacing: 16 }, [
            Text('Volume: ' + volume).font('headline'),
            Slider({
              value: volume, setValue: setVolume, range: [0, 100], step: 5, label: 'Volume',
              minimumValueLabel: Text('0').font('caption'),
              maximumValueLabel: Text('100').font('caption'),
            }),
            Text('Opacity: ' + opacity.toFixed(2)).font('headline'),
            Slider({ value: opacity, setValue: setOpacity }),
            RoundedRectangle({ cornerRadius: 12 }).fill(Color('blue')).opacity(opacity).frame({ height: 60 }),
            Text('Read-only (no setValue)').font('caption'),
            Slider({ value: 0.3 }),
          ]);
        })()
        """.trimIndent()
    ),
    component(
        name = "Placeholder",
        description = "Placeholder slots: one resolved to a composable the app registers under the same name, which receives the props of the component the slot sits in, and one with no registered composable, drawn as a grey box.",
        source = """
        (() => {
          const Badge = defineComponent({
            body: (props) => HStack({ spacing: 8 }, [
              Text('Badge:').font('caption'),
              Placeholder({ name: 'PreviewBadge' }),
            ]),
          });
          return VStack({ alignment: 'leading', spacing: 16 }, [
            Text('Placeholder resolved by the host').font('headline'),
            Badge({ label: 'Native', count: 3 }),
            Text('Placeholder with no registered component').font('headline'),
            Placeholder({ name: 'NotRegistered' }).frame({ width: 200, height: 60 }),
          ]);
        })()
        """.trimIndent()
    ),
    component(
        name = "Path",
        description = "Every path element: lines closed into a filled star, a stroked cubic curve and arc, and rect, rounded rect and ellipse in one path.",
        source = """
        ScrollView([VStack({ alignment: 'leading', spacing: 20 }, [
          Text('Lines, closed and filled').font('headline'),
          Path(p => {
            p.move(60, 0); p.line(75, 40); p.line(120, 45); p.line(88, 75); p.line(97, 120);
            p.line(60, 98); p.line(23, 120); p.line(32, 75); p.line(0, 45); p.line(45, 40);
            p.close();
          }).fill(Color('orange')).frame({ width: 120, height: 120 }),
          Text('Cubic curve and arc, stroked').font('headline'),
          Path(p => {
            p.move(0, 80);
            p.curve(240, 80, 60, -40, 180, 200);
            p.arc({ centerX: 120, centerY: 80, radius: 40, startAngle: 0, endAngle: 270, clockwise: true });
          }).stroke({ style: Color('blue'), lineWidth: 4 }).frame({ width: 240, height: 160 }),
          Text('Rect, rounded rect and ellipse').font('headline'),
          Path(p => {
            p.addRect(0, 0, 60, 60);
            p.addRoundedRect({ x: 80, y: 0, width: 60, height: 60, cornerWidth: 16, cornerHeight: 16 });
            p.addEllipse(160, 0, 80, 60);
          }).fill(Color('green')).frame({ width: 240, height: 60 }),
          Text('Quad curve, stroked and filled').font('headline'),
          Path(p => {
            p.move(0, 60); p.quadCurve(200, 60, 100, -60); p.close();
          }).fill(Color('purple')).stroke({ style: Color('black'), lineWidth: 2 }).frame({ width: 200, height: 60 }),
        ])])
        """.trimIndent()
    ),
    component(
        name = "CustomFont",
        description = "Font families resolved three ways: an installed family, a font downloaded from a URL, and an unknown family falling back to the system face.",
        source = """
        VStack({ alignment: 'leading', spacing: 16 }, [
          Text('System font at size 20').font(20),
          Text('Installed family: serif, 24').font(CustomFont({ family: 'serif', size: 24 })),
          Text('Installed family: monospace, 18').font(CustomFont({ family: 'monospace', size: 18 })),
          Text('Remote: Pacifico from a URL, 28').font(CustomFont({ family: 'Pacifico', size: 28, url: '$PACIFICO_URL' })),
          Text({ markdown: 'Remote font on **markdown** text, 22' }).font(CustomFont({ family: 'Pacifico', size: 22, url: '$PACIFICO_URL' })),
          Text('Unknown family falls back to system, 20').font(CustomFont({ family: 'No Such Font', size: 20 })),
        ])
        """.trimIndent()
    ),
    component(
        name = "Markdown",
        description = "Text({ markdown }) rendering: headings, inline styles, lists, a quote, a code block, and markdown under font and colour modifiers.",
        source = """
        ScrollView([
          VStack({ alignment: 'leading', spacing: 16 }, [
            Text({ markdown: '# Heading 1\n## Heading 2\n### Heading 3' }),
            Text({ markdown: 'A paragraph with **bold**, _italic_, ~~strikethrough~~, `inline code` and a [link](https://metabind.ai).' }),
            Text({ markdown: '- First item\n- Second item\n  - Nested item\n\n1. Step one\n2. Step two' }),
            Text({ markdown: '> A block quote\n> spanning two lines.' }),
            Text({ markdown: '```\nconst answer = 42;\nconsole.log(answer);\n```' }),
            Text({ markdown: 'Markdown styled with `.font(title2)` and a blue `.foregroundStyle`' }).font('title2').foregroundStyle(Color('blue')),
            Text({ markdown: 'Centered **markdown** in a full-width frame' }).multilineTextAlignment('center').frame({ maxWidth: Infinity }),
          ])
        ])
        """.trimIndent()
    ),
)
