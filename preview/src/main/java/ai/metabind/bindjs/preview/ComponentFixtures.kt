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
        name = "List",
        description = "A grouped list with a selection: a section whose tagged rows select on tap and report the choice in the footer, and a section showing a row background and a hidden separator.",
        source = """
        (() => {
          const [selected, setSelected] = useState('tokyo');
          const cities = [['paris', 'Paris'], ['tokyo', 'Tokyo'], ['lima', 'Lima']];
          return List({ selection: selected, setSelection: setSelected }, [
            Section({
              header: Text('Destinations'),
              footer: Text('Tap a row to select it. Selected: ' + selected),
            }, cities.map(([id, name]) => HStack([
              Text(name),
              Spacer(),
              Text(id === selected ? 'Selected' : '').font('caption').foregroundStyle('blue'),
            ]).tag(id))),
            Section({ header: Text('Row styling') }, [
              Text('Custom row background').listRowBackground(Color('yellow')),
              Text('No separator around this row').listRowSeparator('hidden'),
              Text('A plain row').font('headline'),
            ]),
          ]);
        })()
        """.trimIndent()
    ),
    component(
        name = "ListPlain",
        description = "The same list machinery in the plain style: no backdrop or cards, rows from a ForEach with separators, a section header, and a hidden scroll content background.",
        source = """
        (() => {
          const fruit = ['Apple', 'Banana', 'Cherry', 'Date'];
          return List([
            Section({ header: Text('Fruit') }, [
              ForEach(fruit, (name) => HStack({ spacing: 12 }, [
                Circle().fill(Color('green')).frame({ width: 10, height: 10 }),
                Text(name),
              ])),
            ]),
            Text('A row outside any section'),
            Text('And another'),
          ]).listStyle('plain').scrollContentBackground('hidden');
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
    component(
        name = "TextField",
        description = "Text inputs on a quaternary fill, as the A2UI catalog draws them: an empty field showing its placeholder, a filled one, a secure field, and a disabled one.",
        source = """
        (() => {
          const [name, setName] = useState('Jane Doe');
          const [when, setWhen] = useState('');
          const [secret, setSecret] = useState('');
          const field = (input) => input.padding(10).background(Color('quaternary')).cornerRadius(8);
          return VStack({ alignment: 'leading', spacing: 12 }, [
            Text('Event').font('caption').foregroundStyle(Color('secondary')),
            field(TextField({ placeholder: 'YYYY-MM-DDTHH:MM', text: when, setText: setWhen })),
            Text('Full name').font('caption').foregroundStyle(Color('secondary')),
            field(TextField({ placeholder: 'Jane Doe', text: name, setText: setName })),
            Text('Password').font('caption').foregroundStyle(Color('secondary')),
            field(SecureField({ placeholder: 'Required', text: secret, setText: setSecret })),
            Text('Disabled').font('caption').foregroundStyle(Color('secondary')),
            field(TextField({ placeholder: 'Not editable', text: '' }).disabled(true)),
          ]);
        })()
        """.trimIndent()
    ),
    component(
        name = "VerticalDivider",
        description = "Vertical rules inside rows: a 1pt Rectangle filling the row height next to one-line and two-line text, and the same row given an explicit height, where the rule fills the frame instead.",
        source = """
        (() => {
          const rule = () => Rectangle().foregroundStyle(Color('gray')).frame({ width: 1, maxHeight: Infinity });
          return VStack({ alignment: 'leading', spacing: 16 }, [
            Text('As tall as the tallest sibling').font('caption'),
            HStack({ spacing: 16 }, [Text('Left'), rule(), Text('Right')]),
            HStack({ spacing: 16 }, [
              Text('One line'),
              rule(),
              VStack({ alignment: 'leading' }, [Text('Two lines'), Text('on this side').font('caption')]),
            ]),
            Text('Inside a 64pt frame').font('caption'),
            HStack({ spacing: 16 }, [Text('Left'), rule(), Text('Right')]).frame({ height: 64 }),
          ]);
        })()
        """.trimIndent()
    ),
    component(
        name = "ForEach",
        description = "ForEach rows inside containers that read their children rather than lay them out: a navigation bar's toolbar items, segmented and menu pickers whose tagged options come from a ForEach, and a menu whose items do.",
        source = """
        (() => {
          const [size, setSize] = useState('M');
          const [fruit, setFruit] = useState('Cherry');
          return NavigationStack(
            VStack({ alignment: 'leading', spacing: 16 }, [
              Text('Segmented picker').font('headline'),
              Picker('Size', [size, setSize], [
                ForEach(['S', 'M', 'L', 'XL'], (s) => Text(s).tag(s)),
              ]).pickerStyle('segmented'),
              Text('Menu picker').font('headline'),
              Picker('Fruit', [fruit, setFruit], [
                ForEach(['Apple', 'Banana', 'Cherry'], (f) => Text(f).tag(f)),
              ]),
              Text('Menu').font('headline'),
              Menu({ label: Text('Actions') }, [
                ForEach(['Copy', 'Rename', 'Delete'], (a) => Button(a, () => {})),
              ]),
            ])
              .padding(16)
              .navigationTitle('ForEach')
              .navigationBarTitleDisplayMode('inline')
              .toolbar([
                ForEach(['Share', 'Edit'], (a) =>
                  ToolbarItem({ placement: 'primaryAction' }, [Button(a, () => {})])),
              ])
          );
        })()
        """.trimIndent()
    ),
    component(
        name = "Menu",
        description = "Menus titled three ways: a string title with a string-titled submenu, a label component, and no label at all, which reads 'Menu' as on iOS.",
        source = """
        VStack({ alignment: 'leading', spacing: 16 }, [
          Menu('Actions', [
            Button('Copy', () => {}),
            Menu('More', [Button('Archive', () => {})]),
          ]),
          Menu({ label: Text('Styled label').foregroundStyle(Color('blue')) }, [
            Button('Share', () => {}),
          ]),
          Menu([Button('Untitled item', () => {})]),
        ]).padding(16)
        """.trimIndent()
    ),
    component(
        name = "Picker",
        description = "Pickers styled the SwiftUI way: a segmented style written on the enclosing stack and one behind a frame, a picker's own menu style beating the stack's, an untagged option that shows but cannot be chosen, a disabled picker, and a tag with an apostrophe.",
        source = """
        (() => {
          const [size, setSize] = useState('m');
          const [speed, setSpeed] = useState('fast');
          const [dept, setDept] = useState("Men's");
          const [tagged, setTagged] = useState('tagged');
          const [locked, setLocked] = useState('on');
          const sizes = () => [Text('Small').tag('s'), Text('Medium').tag('m'), Text('Large').tag('l')];
          const caption = (text) => Text(text).font('caption').foregroundStyle(Color('gray'));
          return VStack({ alignment: 'leading', spacing: 12 }, [
            caption('Segmented, set on the stack'),
            Picker('Size', [size, setSize], sizes()),
            caption('Segmented, behind a frame'),
            Picker('Speed', [speed, setSpeed], [Text('Slow').tag('slow'), Text('Fast').tag('fast')])
              .frame({ maxWidth: Infinity })
              .pickerStyle('segmented'),
            caption('Untagged option'),
            Picker('Tagging', [tagged, setTagged], [Text('Tagged').tag('tagged'), Text('Untagged')]).pickerStyle('segmented'),
            caption('Disabled'),
            Picker('Locked', [locked, setLocked], [Text('Off').tag('off'), Text('On').tag('on')])
              .pickerStyle('segmented')
              .disabled(true),
            caption("Own menu style, tag with an apostrophe"),
            Picker('Department', [dept, setDept], [Text("Men's").tag("Men's"), Text("Women's").tag("Women's")])
              .pickerStyle('menu'),
          ]).pickerStyle('segmented').padding(16);
        })()
        """.trimIndent()
    ),
    component(
        name = "Disabled",
        description = "`.disabled(true)` the SwiftUI way: on a stack it disables every control inside, through a ScrollView, a List and a frame, and a `.disabled(false)` further in cannot undo it. The last button is live.",
        source = """
        (() => {
          const [isOn, setIsOn] = useState(true);
          const [text, setText] = useState('');
          const [choice, setChoice] = useState('a');
          const caption = (t) => Text(t).font('caption').foregroundStyle(Color('gray'));
          return VStack({ alignment: 'leading', spacing: 10 }, [
            caption('Disabled stack'),
            VStack({ alignment: 'leading', spacing: 8 }, [
              Button('Stack button', () => {}),
              Toggle({ label: 'Stack toggle', isOn, setIsOn }),
              TextField({ placeholder: 'Stack field', text, setText }),
              Picker('Choice', [choice, setChoice], [Text('Seg A').tag('a'), Text('Seg B').tag('b')])
                .pickerStyle('segmented'),
              NavigationLink('Stack link', () => Text('Destination')),
              Menu('Stack menu', [Button('Menu item', () => {})]),
              Button('Nested enable', () => {}).disabled(false),
            ]).disabled(true),
            caption('Inside a ScrollView and a List'),
            ScrollView([Button('Scroll button', () => {})]).frame({ height: 44 }).disabled(true),
            List([Button('List button', () => {})]).frame({ height: 60 }).disabled(true),
            caption('Behind a frame, and re-enabled further out'),
            Button('Framed button', () => {}).frame({ maxWidth: Infinity, alignment: 'leading' }).disabled(true),
            Button('Inner disabled', () => {}).disabled(true).padding(4).disabled(false),
            caption('Enabled'),
            Button('Live button', () => {}).disabled(false),
          ]).padding(16);
        })()
        """.trimIndent()
    ),
    component(
        name = "Spacer",
        description = "Spacers the SwiftUI way: minLength as a floor rather than a size, a Spacer under padding, a flexible frame or a Group still taking the slack, one in a fixed-size card and nested a level down, and the fixed gaps and scroll views where a Spacer must stay its size.",
        source = """
        (() => {
          const caption = (t) => Text(t).font('caption').foregroundStyle(Color('gray'));
          const bar = (left, spacer, right) =>
            HStack([Text(left), spacer, Text(right)]).frame({ width: 300 }).background(Color('systemGray6'));
          const card = (top, content) =>
            content.frame({ width: 140, height: 90 }).background(Color('systemGray6'));
          return VStack({ alignment: 'leading', spacing: 6 }, [
            caption('Flexible: pushed to the edges'),
            bar('A1', Spacer(), 'B1'),
            bar('A2', Spacer({ minLength: 40 }), 'B2'),
            bar('A3', Spacer().padding(4), 'B3'),
            bar('A4', Spacer().frame({ minWidth: 10 }), 'B4'),
            bar('A5', Group([Spacer()]), 'B5'),
            caption('Fixed: a 24pt gap'),
            bar('A6', Spacer().frame({ width: 24 }), 'B6'),
            caption('In a fixed-size card, and nested a level down'),
            HStack([
              card('C1', VStack([Text('Top C1'), Spacer(), Text('Bottom C1')])),
              card('C2', VStack([VStack([Text('Top C2'), Spacer({ minLength: 8 }), Text('Bottom C2')])])),
            ]),
            caption('In a vertical ScrollView: its minimum, 24pt'),
            ScrollView([VStack([Text('Top S'), Spacer({ minLength: 24 }), Text('Bottom S')])])
              .frame({ height: 90 }).background(Color('systemGray6')),
            caption('In a horizontal ScrollView: its minimum, 30pt'),
            ScrollView({ axis: 'horizontal' }, [HStack([Text('Left H'), Spacer({ minLength: 30 }), Text('Right H')])])
              .frame({ width: 300 }).background(Color('systemGray6')),
          ]).padding(16);
        })()
        """.trimIndent()
    ),
    component(
        name = "Hotspot",
        description = "Hotspots like the Explore card's: a Circle in a frame much taller than it is wide draws as the dot that fits, and a label offset below it moves by the offset once, as do an offset, faded and scaled Text.",
        source = """
        (() => {
          const hotspot = (title) => ZStack([
            Circle().fill(Color('white')).frame({ width: 16, height: 400 }),
            Circle().fill(Color('white')).frame({ width: 34, height: 34 }).opacity(0.25),
            Text(title).multilineTextAlignment('center').fixedSize({ horizontal: true })
              .offset({ y: 32 }).font('footnote').fontWeight('semibold').foregroundStyle(Color('gray')),
          ]).frame({ width: 44, height: 44 });
          return VStack({ spacing: 24 }, [
            ZStack([
              hotspot('Hotspot A').offset({ x: -80, y: -40 }),
              hotspot('Hotspot B').offset({ x: 60, y: 30 }),
            ]).frame({ width: 300, height: 220 }).background(Color('black')).cornerRadius(24),
            HStack({ spacing: 24 }, [
              Text('Offset').offset({ y: 12 }),
              Text('Faded').opacity(0.5),
              Text('Scaled').scaleEffect(1.5),
            ]),
          ]).padding(16);
        })()
        """.trimIndent()
    ),
    component(
        name = "LazyVStack",
        description = "A lazy vertical stack in a ScrollView: sections with pinned headers, leading alignment with explicit spacing, and a trailing-aligned stack on the default spacing.",
        source = """
        ScrollView([
          LazyVStack({ alignment: 'leading', spacing: 12, pinnedViews: 'sectionHeaders' }, [
            Section({ header: Text('Section A').font('headline') }, [
              Text('Leading aligned, 12pt spacing'),
              Text('Row A2'),
              Text('Row A3'),
            ]),
            Section({ header: Text('Section B').font('headline') }, [
              Text('Row B1'),
              Text('Row B2'),
            ]),
          ]),
          LazyVStack({ alignment: 'trailing' }, [
            Text('Trailing aligned').font('headline'),
            Text('On the default spacing'),
            Text('Which matches SwiftUI'),
          ]),
        ])
        """.trimIndent()
    ),
    component(
        name = "LazyHStack",
        description = "A lazy horizontal stack at every vertical alignment — top, center, bottom and the two text baselines — plus the default spacing and one inside a horizontal ScrollView.",
        source = """
        (() => {
          const mixed = () => [
            Text('Big').font(34),
            Text('small').font('caption'),
            Color('blue').frame({ width: 24, height: 44 }),
            Text('Medium').font('title3'),
          ];
          const row = (alignment) => VStack({ alignment: 'leading', spacing: 4 }, [
            Text(alignment).font('caption').foregroundStyle(Color('gray')),
            LazyHStack({ alignment, spacing: 8 }, mixed()),
          ]);
          return VStack({ alignment: 'leading', spacing: 16 }, [
            row('top'),
            row('center'),
            row('bottom'),
            row('firstTextBaseline'),
            row('lastTextBaseline'),
            Text('default spacing').font('caption').foregroundStyle(Color('gray')),
            LazyHStack([Text('one'), Text('two'), Text('three')]),
            Text('in a horizontal ScrollView').font('caption').foregroundStyle(Color('gray')),
            ScrollView({ axis: 'horizontal' }, [
              LazyHStack({ spacing: 8 }, ['Alpha', 'Beta', 'Gamma', 'Delta', 'Epsilon'].map(
                (name) => Text(name).padding(8).background(Color('yellow')).cornerRadius(8)
              )),
            ]),
          ]);
        })()
        """.trimIndent()
    ),
)
