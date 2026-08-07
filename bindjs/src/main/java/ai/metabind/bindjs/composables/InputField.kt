package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow

/**
 * The shared body of [TextFieldView] and [TextEditorView] — SwiftUI's text inputs, which
 * are undecorated: no filled container, no indicator line, no minimum height, and no
 * padding of their own. Material's `TextField` supplies all four, which on an A2UI input
 * (already wrapped by the component in `.padding(10).background(…).cornerRadius(8)`)
 * stacked a second, darker box inside the first and padded the row to Material's 56dp
 * minimum. [BasicTextField] has none of that, so the surrounding component's own
 * background is the only one drawn — as on iOS.
 *
 * The value is owned by JS: it arrives resolved in [value], and edits go back through
 * [UiEvent.OnTextChange]. That round-trip is asynchronous — handler, re-render, then the
 * new value arrives back as a prop — so while the field has focus its local state is
 * authoritative and incoming values are ignored. Reconciling on every prop change instead
 * loses characters: type faster than the round-trip and an echo of an earlier keystroke
 * arrives while later ones are already in the buffer, overwriting them ("jane@acme.com"
 * came back as "@acmeoma"). External writes are adopted when focus leaves, which is also
 * when a server-pushed correction is least disruptive.
 */
@Composable
internal fun PlainTextField(
    value: String,
    setTextId: String?,
    modifier: Modifier,
    onUiEvent: (UiEvent) -> Unit,
    singleLine: Boolean,
    enabled: Boolean = true,
    autoCorrectEnabled: Boolean = true,
    placeholder: String = "",
    obscured: Boolean = false,
) {
    var draft by remember { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }
    if (!focused && value != draft) {
        draft = value
    }

    val textStyle = LocalTextStyle.current.merge(color = LocalContentColor.current)

    BasicTextField(
        value = draft,
        onValueChange = { newText ->
            draft = newText
            setTextId?.let { onUiEvent(UiEvent.OnTextChange(it, newText)) }
        },
        // SwiftUI's text inputs take all the width they are offered — Material's
        // `TextField` faked that with a 280dp minimum, which `BasicTextField` does not
        // have, so without this a field shrinks to fit its current text. Applied inside
        // the caller's chain so an explicit `.frame(width:)` still wins.
        modifier = modifier
            .then(Modifier.fillMaxWidth())
            .onFocusChanged { focused = it.isFocused },
        enabled = enabled,
        singleLine = singleLine,
        textStyle = textStyle,
        cursorBrush = SolidColor(LocalContentColor.current),
        visualTransformation = if (obscured) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            // Suppress autocorrect/suggestions over a masked value regardless of the
            // `.autocorrectionDisabled()` modifier, as SwiftUI's SecureField does.
            autoCorrectEnabled = autoCorrectEnabled && !obscured,
            keyboardType = if (obscured) KeyboardType.Password else KeyboardType.Unspecified,
        ),
        decorationBox = { innerTextField ->
            Box {
                if (draft.isEmpty() && placeholder.isNotEmpty()) {
                    // `singleLine` governs the input text, not this composable — an
                    // unconstrained placeholder still wraps, which is how an empty
                    // DateTimeInput grew back to three lines the moment its value cleared.
                    Text(
                        text = placeholder,
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            }
        },
    )
}
