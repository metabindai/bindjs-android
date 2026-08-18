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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.getPickerStyle
import ai.metabind.bindjs.model.ModifiedComponent
import ai.metabind.bindjs.model.PickerComponent
import ai.metabind.bindjs.model.TextComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.LocalModifier
import ai.metabind.bindjs.model.modifier.TagModifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerView(
    jsRuntime: JsRuntime,
    component: PickerComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit
) {
    val options = linkedMapOf<String, String>()
    component.props.children?.forEachIndexed { index, child ->
        if (child is ModifiedComponent &&
            child.props.content?.first() is TextComponent &&
            child.props.modifier is TagModifier
        ) {
            val textComponent = child.props.content.first() as TextComponent
            textComponent.props.rawValue?.let {
                val tag = child.props.modifier.props.rawValue
                options.put(tag, it)
            }
        }
    }

    val label = component.props.label
    val selection = component.props.selection.first()

    val selectedItem = options.getOrDefault(selection, label)

    val pickerStyle = modifiers.getPickerStyle()
    if (pickerStyle == "segmented") {
        var selectedIndex by remember {
            mutableIntStateOf(0)
        }

        selectedIndex = options.keys.indexOf(selection)

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
            component.props.children?.forEachIndexed { index, child ->
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
                                    tag = options.entries.elementAt(index).key
                                )
                            )
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size)
                ) {
                    // iOS truncates a segment label rather than wrapping it, and a
                    // wrapped label would also grow the whole row's height.
                    Text(
                        text = options.entries.elementAt(index).value,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                component.props.children?.forEachIndexed { index, child ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(options.entries.elementAt(index).value)
                            }
                        },
                        onClick = {
                            expanded = false

                            component.props.setterId?.let {
                                onUiEvent(
                                    UiEvent.OnPickerTap(
                                        environmentId = component.props.environmentId,
                                        setterId = it,
                                        tag = options.entries.elementAt(index).key
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
