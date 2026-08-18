package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.getPickerStyle
import ai.metabind.bindjs.composables.ext.modifiersToShareWithChildren
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.ModifiedComponent
import ai.metabind.bindjs.model.PickerComponent
import ai.metabind.bindjs.model.TextComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.LineLimitModifier
import ai.metabind.bindjs.model.modifier.LineLimitProps
import ai.metabind.bindjs.model.modifier.LocalModifier
import ai.metabind.bindjs.model.modifier.TagModifier

/**
 * One `Text("All").tag("all")` item of a Picker.
 *
 * [label] is the item as authored, kept whole so it can be rendered through the
 * renderer (and keep its own `.font(...)` / `.foregroundStyle(...)`); [text] is the
 * flattened string, which the drop-down's read-only TextField needs because it can
 * only take a `String`.
 */
private class PickerOption(
    val tag: String,
    val label: BaseComponent<*>,
    val text: String,
    val hasLineLimit: Boolean,
)

/**
 * Read a Picker item out of its modifier chain.
 *
 * `.tag(...)` can sit anywhere in that chain and the label may carry modifiers of its
 * own — `Text("All").font("caption").tag("all")` nests as Tag(Font(Text)). Matching
 * only Tag-directly-wrapping-Text dropped every styled item, and since the item list
 * drives the row, the whole segment disappeared with it. So walk the chain: the tag is
 * whichever `TagModifier` it carries, the text is the string at the leaf.
 */
private fun BaseComponent<*>.asPickerOption(): PickerOption? {
    var node: BaseComponent<*>? = this
    var tag: String? = null
    var hasLineLimit = false
    while (node is ModifiedComponent) {
        val modifier = node.props.modifier
        if (modifier is TagModifier) tag = modifier.props.rawValue
        if (modifier is LineLimitModifier) hasLineLimit = true
        node = node.props.content?.firstOrNull()
    }
    val resolvedTag = tag ?: return null
    val textProps = (node as? TextComponent)?.props
    return PickerOption(
        tag = resolvedTag,
        label = this,
        text = textProps?.rawValue ?: textProps?.markdown ?: "",
        hasLineLimit = hasLineLimit,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerView(
    jsRuntime: JsRuntime,
    component: PickerComponent,
    version: Int,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit
) {
    val options = component.props.children
        ?.mapNotNull { child -> child?.asPickerOption() }
        ?: emptyList()

    val label = component.props.label
    val selection = component.props.selection.first()

    val selectedItem = options.firstOrNull { it.tag == selection }?.text ?: label

    val pickerStyle = modifiers.getPickerStyle()
    if (pickerStyle == "segmented") {
        var selectedIndex by remember {
            mutableIntStateOf(0)
        }

        selectedIndex = options.indexOfFirst { it.tag == selection }

        // SwiftUI's `.segmented` picker is greedy horizontally: it takes the width
        // its container offers and splits it equally between the segments. M3's
        // SingleChoiceSegmentedButtonRow appends `.width(IntrinsicSize.Min)` to
        // whatever modifier it's handed, so with wrapContentWidth the row sized
        // itself off each label's *longest word* — "New & changed" came out narrow
        // and wrapped onto two lines. Handing it fillMaxWidth() gives that
        // intrinsic modifier a fixed incoming width to clamp to, matching iOS
        // (in a horizontally-unbounded parent fillMaxWidth is a no-op, so the row
        // still falls back to hugging its content). A picker that's a
        // non-expanding child of an HStack keeps hugging, so it can't starve its
        // siblings — same rule as ColumnView.
        val inRowNoWeight = modifiers.any { it is LocalModifier.InRow } &&
                modifiers.none { it is LocalModifier.Weight }

        SingleChoiceSegmentedButtonRow(
            modifier = if (inRowNoWeight) Modifier.wrapContentWidth() else Modifier.fillMaxWidth()
        ) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    icon = {},
                    selected = index == selectedIndex,
                    onClick = {
                        selectedIndex = index

                        component.props.setterId?.let {
                            onUiEvent(
                                UiEvent.OnPickerTap(
                                    environmentId = component.props.environmentId,
                                    setterId = it,
                                    tag = option.tag
                                )
                            )
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size)
                ) {
                    PickerOptionLabel(
                        jsRuntime = jsRuntime,
                        option = option,
                        version = version,
                        pickerModifiers = modifiers,
                        onUiEvent = onUiEvent
                    )
                }
            }
        }
    } else {
        // drop down
        var expanded by remember {
            mutableStateOf(false)
        }

        ExposedDropdownMenuBox(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .wrapContentWidth(),
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }
        ) {
            Row(
                modifier = Modifier.wrapContentWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                    value = selectedItem,
                    onValueChange = { },
                    readOnly = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent
                    ),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                )
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PickerOptionLabel(
                                    jsRuntime = jsRuntime,
                                    option = option,
                                    version = version,
                                    pickerModifiers = modifiers,
                                    onUiEvent = onUiEvent
                                )
                            }
                        },
                        onClick = {
                            expanded = false

                            component.props.setterId?.let {
                                onUiEvent(
                                    UiEvent.OnPickerTap(
                                        environmentId = component.props.environmentId,
                                        setterId = it,
                                        tag = option.tag
                                    )
                                )
                            }
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

/**
 * Render one option's label through the renderer, so a `.font(...)` /
 * `.foregroundStyle(...)` / `.textCase(...)` on the item — or on the Picker, which
 * SwiftUI passes down through the environment — actually lands. A plain `Text(...)`
 * here threw all of it away and drew the raw string in Material's default style.
 *
 * Two things travel with it, mirroring how [ButtonView] renders its own label slot:
 * the text-formatting modifiers of the Picker via [modifiersToShareWithChildren], and
 * M3's selected/unselected content colour via [LocalContentTint], which is what
 * [TextView] colours otherwise-unstyled text with. Without the tint the label would
 * come out black on top of a selected (filled) segment.
 */
@Composable
private fun PickerOptionLabel(
    jsRuntime: JsRuntime,
    option: PickerOption,
    version: Int,
    pickerModifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    val shared = pickerModifiers.modifiersToShareWithChildren()
    // iOS truncates a segment label rather than wrapping it, and a wrapped label
    // would also grow the row's height. An explicit `.lineLimit(...)` anywhere in
    // the chain still wins.
    val labelModifiers = if (option.hasLineLimit || shared.any { it is LineLimitModifier }) {
        shared
    } else {
        shared + LineLimitModifier(LineLimitProps(rawValue = 1, children = emptyList()))
    }

    CompositionLocalProvider(LocalContentTint provides LocalContentColor.current) {
        BindJSView(
            jsRuntime = jsRuntime,
            component = option.label,
            version = version,
            onUiEvent = onUiEvent,
            modifiers = labelModifiers
        )
    }
}
