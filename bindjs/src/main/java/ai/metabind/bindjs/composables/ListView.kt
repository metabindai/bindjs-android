package ai.metabind.bindjs.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.getListStyle
import ai.metabind.bindjs.composables.ext.isScrollContentBackgroundHidden
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.BrushComponent
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.EmptyComponent
import ai.metabind.bindjs.model.ListComponent
import ai.metabind.bindjs.model.ModifiedComponent
import ai.metabind.bindjs.model.SectionComponent
import ai.metabind.bindjs.model.expandingForEach
import ai.metabind.bindjs.model.layoutChildren
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.FontModifier
import ai.metabind.bindjs.model.modifier.FontProps
import ai.metabind.bindjs.model.modifier.ListRowBackgroundModifier
import ai.metabind.bindjs.model.modifier.ListRowSeparatorModifier
import ai.metabind.bindjs.model.modifier.LocalModifier
import ai.metabind.bindjs.model.modifier.TagModifier
import ai.metabind.bindjs.model.modifier.TextCaseModifier
import ai.metabind.bindjs.model.modifier.TextCaseProps

/**
 * A `List(...)`: rows, grouped into sections, in a vertically scrolling column.
 *
 * The list owns everything between the rows' content and its own edges: the row
 * padding and minimum height, the separators, the section headers and footers, the
 * backdrop and the grouped cards. Its children only supply the content, so each row is
 * rendered with no inherited modifiers, the way [ScrollView] renders its children.
 *
 * Two looks, chosen by `.listStyle(...)` on the list:
 *
 * - **Grouped** (`automatic`, `insetGrouped`, `grouped`): a grey backdrop with each
 *   section in a white card. `grouped` runs the cards edge to edge; the other two inset
 *   them and round their corners. Headers are drawn in uppercase footnote type above
 *   the card, footers in footnote type below it. `.scrollContentBackground('hidden')`
 *   drops the grey so a `.background(...)` on the list shows through.
 * - **Plain** (`plain`, `inset`, `sidebar`): no backdrop and no cards, just rows and
 *   separators.
 *
 * Rows come from the children, with a `ForEach` flattened into its rows and each
 * `Section` contributing its header, rows and footer. Consecutive rows written directly
 * on the list form a section of their own with neither. Two modifiers are read off a
 * row's chain rather than applied to it: `.listRowSeparator('hidden')` leaves out the
 * separators on both sides of the row, and `.listRowBackground(view)` replaces what the
 * list would have painted behind it.
 *
 * Selection: when the list has a `setSelection` handler, every row carrying a `.tag(...)`
 * is clickable and a tap reports its tag through [UiEvent.OnListSelection]. The row whose
 * tag equals `selection` is highlighted. Rows without a tag are plain content.
 *
 * Like [ScrollView], the column is lazy unless the host already scrolls vertically, in
 * which case it is a plain [Column] so two scroll containers are never nested.
 */
@Composable
fun ListView(
    jsRuntime: JsRuntime,
    component: ListComponent,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    val look = ListLook.from(modifiers.getListStyle())
    val entries = component.entries()

    val backdrop = if (look.grouped && !modifiers.isScrollContentBackgroundHidden()) {
        Modifier.background(GroupedBackdrop)
    } else {
        Modifier
    }
    val listModifier = modifiers
        .buildModifier(onUiEvent)
        .fillMaxWidth()
        .then(backdrop)

    val entry: @Composable (ListEntry) -> Unit = { item ->
        ListEntryView(
            jsRuntime = jsRuntime,
            list = component,
            entry = item,
            look = look,
            version = version,
            onUiEvent = onUiEvent,
        )
    }

    // A row is offered unbounded height either way; see LocalInVerticalScroll.
    if (LocalHostScrollsVertically.current) {
        Column(modifier = listModifier) {
            CompositionLocalProvider(LocalInVerticalScroll provides true) {
                entries.forEach { entry(it) }
            }
        }
    } else {
        LazyColumn(modifier = listModifier) {
            entries.forEach {
                item { CompositionLocalProvider(LocalInVerticalScroll provides true) { entry(it) } }
            }
        }
    }
}

/** How a list style translates into chrome. */
private class ListLook(
    /** Backdrop plus a card per section; otherwise bare rows. */
    val grouped: Boolean,
    /** Cards inset from the list's edges with rounded corners; only meaningful when grouped. */
    val inset: Boolean,
) {
    /** Horizontal distance from the list's edge to a card's edge. */
    val cardInset: Dp get() = if (grouped && inset) 16.dp else 0.dp

    /** Horizontal padding of headers and footers, which sit outside the card. */
    val captionInset: Dp get() = cardInset + 16.dp

    val cardCorner: Dp get() = if (grouped && inset) 10.dp else 0.dp

    companion object {
        fun from(style: String): ListLook = when (style.lowercase()) {
            "plain", "inset", "sidebar" -> ListLook(grouped = false, inset = false)
            "grouped" -> ListLook(grouped = true, inset = false)
            // "automatic", "insetGrouped", "inset-grouped" and anything unknown.
            else -> ListLook(grouped = true, inset = true)
        }
    }
}

/** The grey behind grouped cards. */
private val GroupedBackdrop = Color(0xFFF2F2F7)

/** A grouped card's own background; plain rows have none. */
private val CardBackground = Color.White

/** Painted behind the selected row in either look. */
private val SelectedRow = Color(0xFFD1D1D6)
private val Separator = Color(0xFFC6C6C8)
private val CaptionColor = Color(0xFF6D6D72)

/** One thing the list draws, top to bottom. */
private sealed class ListEntry {
    class Header(val content: BaseComponent<*>) : ListEntry()
    class Footer(val content: BaseComponent<*>) : ListEntry()

    /** The gap above a section that has no header to provide one. */
    object SectionGap : ListEntry()

    class Row(
        val content: BaseComponent<*>,
        /** The `.tag(...)` on the row, which makes it selectable. */
        val tag: String?,
        /** The `.listRowBackground(...)` on the row, if any. */
        val background: BaseComponent<*>?,
        /** Whether the row asked for its separators to be left out. */
        val separatorHidden: Boolean,
        val isFirstInSection: Boolean,
        val isLastInSection: Boolean,
        /** Whether a separator is drawn below this row. */
        val separatorBelow: Boolean,
    ) : ListEntry()
}

private class Section(
    val header: BaseComponent<*>?,
    val rows: List<BaseComponent<*>>,
    val footer: BaseComponent<*>?,
)

/** The sections of the list: each `Section` child, and runs of bare rows between them. */
private fun ListComponent.sections(): List<Section> {
    val sections = mutableListOf<Section>()
    val loose = mutableListOf<BaseComponent<*>>()
    fun flushLoose() {
        if (loose.isNotEmpty()) {
            sections.add(Section(header = null, rows = loose.toList(), footer = null))
            loose.clear()
        }
    }
    props.children.expandingForEach().layoutChildren()?.forEach { child ->
        when (child) {
            null -> {}
            is SectionComponent -> {
                flushLoose()
                sections.add(
                    Section(
                        header = child.props.header,
                        rows = child.props.children.expandingForEach().layoutChildren()
                            ?.filterNotNull() ?: emptyList(),
                        footer = child.props.footer,
                    )
                )
            }

            else -> loose.add(child)
        }
    }
    flushLoose()
    return sections
}

private fun ListComponent.entries(): List<ListEntry> {
    val entries = mutableListOf<ListEntry>()
    sections().forEach { section ->
        section.header?.takeUnless { it is EmptyComponent }
            ?.let { entries.add(ListEntry.Header(it)) }
            ?: entries.add(ListEntry.SectionGap)

        val rows = section.rows.map { it.asListRow() }
        rows.forEachIndexed { index, row ->
            val next = rows.getOrNull(index + 1)
            entries.add(
                ListEntry.Row(
                    content = row.content,
                    tag = row.tag,
                    background = row.background,
                    separatorHidden = row.separatorHidden,
                    isFirstInSection = index == 0,
                    isLastInSection = next == null,
                    // A hidden separator is hidden on both edges of its row, so the
                    // divider between two rows goes when either of them asks.
                    separatorBelow = next != null && !row.separatorHidden && !next.separatorHidden,
                )
            )
        }

        section.footer?.takeUnless { it is EmptyComponent }
            ?.let { entries.add(ListEntry.Footer(it)) }
    }
    return entries
}

private class RowSpec(
    val content: BaseComponent<*>,
    val tag: String?,
    val background: BaseComponent<*>?,
    val separatorHidden: Boolean,
)

/**
 * Read the row-level modifiers off a row's chain. They can sit anywhere in it —
 * `Text('A').tag('a').listRowBackground(...)` nests as
 * ListRowBackground(Tag(Text)) — so the whole chain is walked; the content stays as
 * authored so that its own `.font(...)` and the like still apply.
 */
private fun BaseComponent<*>.asListRow(): RowSpec {
    var node: BaseComponent<*>? = this
    var tag: String? = null
    var background: BaseComponent<*>? = null
    var separatorHidden = false
    while (node is ModifiedComponent) {
        when (val modifier = node.props.modifier) {
            is TagModifier -> if (tag == null) tag = modifier.props.rawValue
            is ListRowBackgroundModifier -> if (background == null) background = modifier.props.content
            is ListRowSeparatorModifier -> if (modifier.isHidden) separatorHidden = true
            else -> {}
        }
        node = node.props.content?.firstOrNull()
    }
    return RowSpec(content = this, tag = tag, background = background, separatorHidden = separatorHidden)
}

@Composable
private fun ListEntryView(
    jsRuntime: JsRuntime,
    list: ListComponent,
    entry: ListEntry,
    look: ListLook,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
) {
    when (entry) {
        is ListEntry.SectionGap -> Spacer(modifier = Modifier.height(if (look.grouped) 20.dp else 12.dp))

        is ListEntry.Header -> Caption(
            jsRuntime = jsRuntime,
            content = entry.content,
            version = version,
            onUiEvent = onUiEvent,
            padding = PaddingValues(
                start = look.captionInset,
                end = look.captionInset,
                top = if (look.grouped) 20.dp else 12.dp,
                bottom = 8.dp,
            ),
            uppercase = look.grouped,
        )

        is ListEntry.Footer -> Caption(
            jsRuntime = jsRuntime,
            content = entry.content,
            version = version,
            onUiEvent = onUiEvent,
            padding = PaddingValues(
                start = look.captionInset,
                end = look.captionInset,
                top = 8.dp,
                bottom = 0.dp,
            ),
            uppercase = false,
        )

        is ListEntry.Row -> ListRowView(
            jsRuntime = jsRuntime,
            list = list,
            row = entry,
            look = look,
            version = version,
            onUiEvent = onUiEvent,
        )
    }
}

/** A section header or footer: footnote type in grey, outside the card. */
@Composable
private fun Caption(
    jsRuntime: JsRuntime,
    content: BaseComponent<*>,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
    padding: PaddingValues,
    uppercase: Boolean,
) {
    val modifiers = buildList<ComponentModifier<*>> {
        add(FontModifier(FontProps(rawValue = "footnote", children = emptyList())))
        if (uppercase) add(TextCaseModifier(TextCaseProps(rawValue = "uppercase", children = emptyList())))
    }
    Box(modifier = Modifier.fillMaxWidth().padding(padding)) {
        CompositionLocalProvider(LocalContentTint provides CaptionColor) {
            BindJSView(
                jsRuntime = jsRuntime,
                component = content,
                version = version,
                onUiEvent = onUiEvent,
                modifiers = modifiers,
            )
        }
    }
}

@Composable
private fun ListRowView(
    jsRuntime: JsRuntime,
    list: ListComponent,
    row: ListEntry.Row,
    look: ListLook,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
) {
    val setSelectionId = list.props.setSelectionId
    val selectable = setSelectionId != null && row.tag != null
    val selected = row.tag != null && row.tag == list.props.selection

    // A card is one shape but many items, so each row clips its own corners: the
    // first row the top ones, the last the bottom ones.
    val shape = RoundedCornerShape(
        topStart = if (row.isFirstInSection) look.cardCorner else 0.dp,
        topEnd = if (row.isFirstInSection) look.cardCorner else 0.dp,
        bottomStart = if (row.isLastInSection) look.cardCorner else 0.dp,
        bottomEnd = if (row.isLastInSection) look.cardCorner else 0.dp,
    )

    // What the list paints behind the row. A `.listRowBackground(...)` replaces it;
    // a colour or gradient goes straight into the modifier, anything else is rendered
    // as a view behind the content below. Selection wins over both.
    val customBackground = row.background?.takeUnless { it is EmptyComponent }
    val paint: Modifier = when {
        selected -> Modifier.background(SelectedRow)
        customBackground is ColorComponent -> Modifier.background(Color(customBackground.color))
        customBackground is BrushComponent -> Modifier.background(customBackground.createBrush())
        customBackground != null -> Modifier
        look.grouped -> Modifier.background(CardBackground)
        else -> Modifier
    }
    val backgroundView = customBackground?.takeUnless {
        selected || it is ColorComponent || it is BrushComponent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = look.cardInset)
            .clip(shape)
            .then(paint)
            .then(
                if (selectable) {
                    Modifier.clickable {
                        onUiEvent(
                            UiEvent.OnListSelection(
                                handlerId = setSelectionId!!,
                                selection = row.tag!!,
                                environmentId = list.props.environmentId,
                            )
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        backgroundView?.let { background ->
            BindJSView(
                jsRuntime = jsRuntime,
                component = background,
                version = version,
                onUiEvent = onUiEvent,
                modifiers = listOf(LocalModifier.MatchParentSize(Modifier.matchParentSize())),
                isBackground = true,
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BindJSView(
                    jsRuntime = jsRuntime,
                    component = row.content,
                    version = version,
                    onUiEvent = onUiEvent,
                    modifiers = listOf(LocalModifier.FillMaxWidth(Modifier.fillMaxWidth())),
                )
            }
            if (row.separatorBelow) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    thickness = 0.5.dp,
                    color = Separator,
                )
            }
        }
    }
}
