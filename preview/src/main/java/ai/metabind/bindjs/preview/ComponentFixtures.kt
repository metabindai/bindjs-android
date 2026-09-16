package ai.metabind.bindjs.preview

/**
 * Component fixtures, one per BindJS component worth eyeballing. Add a new component by
 * appending a [component] entry here; the drawer picks it up.
 */
private fun component(name: String, description: String, source: String) = PreviewFixture(
    name = name,
    description = description,
    section = PreviewSection.Components,
    source = source,
)

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
)
