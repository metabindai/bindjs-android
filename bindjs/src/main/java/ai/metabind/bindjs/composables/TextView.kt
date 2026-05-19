package ai.metabind.bindjs.composables

import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.applyTextCase
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.getAlignment
import ai.metabind.bindjs.composables.ext.getFontFamily
import ai.metabind.bindjs.composables.ext.getFontSize
import ai.metabind.bindjs.composables.ext.getFontStyle
import ai.metabind.bindjs.composables.ext.getFontWeight
import ai.metabind.bindjs.composables.ext.getForegroundColor
import ai.metabind.bindjs.composables.ext.getForegroundStyleModifierComponent
import ai.metabind.bindjs.composables.ext.getLineSpacing
import ai.metabind.bindjs.composables.ext.getMaxLines
import ai.metabind.bindjs.composables.ext.getTextAlign
import ai.metabind.bindjs.composables.ext.getTextDecoration
import ai.metabind.bindjs.composables.ext.getTextStyle
import ai.metabind.bindjs.composables.ext.getTracking
import ai.metabind.bindjs.composables.ext.isTextSelectionEnabled
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.BrushComponent
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.TextComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.LocalModifier
import ai.metabind.bindjs.model.modifier.PaddingModifier
import io.noties.markwon.Markwon
import android.widget.TextView as AndroidTextView

@Composable
fun TextView(
    jsRuntime: JsRuntime,
    component: TextComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    val foregroundStyleComponent = modifiers.getForegroundStyleModifierComponent()

    val fontStyle = modifiers.getFontStyle()
    val fontWeight = modifiers.getFontWeight()
    val fontSize = modifiers.getFontSize()
    val textStyle = modifiers.getTextStyle()
    val textDecoration = modifiers.getTextDecoration()
    val maxLines = modifiers.getMaxLines()
    val lineSpacing = modifiers.getLineSpacing()
    val defaultLineHeight = if (!LocalTextStyle.current.lineHeight.value.isNaN()) {
        LocalTextStyle.current.lineHeight.value
    } else {
        val baseFontSize = fontSize?.toFloat()
            ?: LocalTextStyle.current.fontSize.value.takeIf { !it.isNaN() }
            ?: 16f
        baseFontSize * 1.2f
    }
    val lineHeight = lineSpacing?.plus(defaultLineHeight)
    val textAlign = modifiers.getTextAlign()
    val tracking = modifiers.getTracking()
    val fontFamily = modifiers.getFontFamily()
    val isTextSelectionEnabled = modifiers.isTextSelectionEnabled()

    val markdownContent = component.props.markdown
    val rawValue = component.props.rawValue

    if (markdownContent != null && rawValue == null) {
        Markdown(
            modifiers = modifiers,
            markdownContent = markdownContent,
            foregroundStyleComponent = foregroundStyleComponent,
            onUiEvent = onUiEvent,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign
        )
    } else {
        val baseText = rawValue ?: markdownContent ?: ""
        val text = modifiers.applyTextCase(baseText)

        when (foregroundStyleComponent) {
            is ColorComponent -> {
                val color = foregroundStyleComponent.getForegroundColor()

                Box(
                    modifier = modifiers
                        .buildModifier(onUiEvent)
                        .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(Modifier.clipToBounds()),
                    contentAlignment = modifiers.getAlignment()
                ) {
                    @Composable
                    fun composeText() {
                        Text(
                            modifier = modifiers
                                .buildModifier(
                                    onUiEvent,
                                    exclude = listOf(LocalModifier::class)
                                )
                                .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(Modifier.clipToBounds()),
                            fontStyle = fontStyle,
                            text = text,
                            fontSize = fontSize?.toInt()?.sp ?: TextUnit.Unspecified,
                            fontWeight = fontWeight,
                            fontFamily = fontFamily,
                            textDecoration = textDecoration,
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = lineHeight?.sp ?: TextUnit.Unspecified,
                            textAlign = textAlign,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = true
                                ),
                                color = color,
                                letterSpacing = tracking.sp
                            ).merge(textStyle)
                        )
                    }

                    if (isTextSelectionEnabled) {
                        SelectionContainer {
                            composeText()
                        }
                    } else {
                        composeText()
                    }
                }
            }

            is BrushComponent -> {
                val brush = foregroundStyleComponent.createBrush()

                Box(
                    modifier = modifiers
                        .buildModifier(onUiEvent)
                        .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(Modifier.clipToBounds()),
                    contentAlignment = modifiers.getAlignment()
                ) {
                    @Composable
                    fun composeText() {
                        Text(
                            modifier = modifiers
                                .buildModifier(
                                    onUiEvent,
                                    exclude = listOf(LocalModifier::class)
                                )
                                .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(Modifier.clipToBounds()),
                            fontStyle = fontStyle,
                            text = text,
                            fontSize = fontSize?.toInt()?.sp ?: TextUnit.Unspecified,
                            fontWeight = fontWeight,
                            fontFamily = fontFamily,
                            textDecoration = textDecoration,
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = lineHeight?.sp ?: TextUnit.Unspecified,
                            textAlign = textAlign,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = true
                                ),
                                brush = brush,
                                letterSpacing = tracking.sp
                            ).merge(textStyle)
                        )
                    }

                    if (isTextSelectionEnabled) {
                        SelectionContainer {
                            composeText()
                        }
                    } else {
                        composeText()
                    }
                }
            }

            else -> {
                Box(
                    modifier = modifiers
                        .buildModifier(onUiEvent)
                        .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(Modifier.clipToBounds()),
                    contentAlignment = modifiers.getAlignment()
                ) {
                    @Composable
                    fun composeText() {
                        Text(
                            modifier = modifiers
                                .buildModifier(
                                    onUiEvent,
                                    exclude = listOf(PaddingModifier::class, LocalModifier::class)
                                )
                                .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(Modifier.clipToBounds()),
                            fontStyle = fontStyle,
                            text = text,
                            fontSize = fontSize?.toInt()?.sp ?: TextUnit.Unspecified,
                            fontWeight = fontWeight,
                            fontFamily = fontFamily,
                            textDecoration = textDecoration,
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = lineHeight?.sp ?: TextUnit.Unspecified,
                            textAlign = textAlign,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = true
                                ),
                                color = Color.Black,
                                letterSpacing = tracking.sp
                            ).merge(textStyle)
                        )
                    }

                    if (isTextSelectionEnabled) {
                        SelectionContainer {
                            composeText()
                        }
                    } else {
                        composeText()
                    }
                }
            }
        }
    }
}

@Composable
private fun Markdown(
    modifiers: List<ComponentModifier<*>>,
    markdownContent: String,
    foregroundStyleComponent: BaseComponent<*>?,
    onUiEvent: (UiEvent) -> Unit,
    fontSize: Number?,
    fontWeight: FontWeight?,
    textAlign: TextAlign
) {
    val text = modifiers.applyTextCase(markdownContent)
    val context = LocalContext.current
    val markwon = remember { Markwon.create(context) }

    val color = when (foregroundStyleComponent) {
        is ColorComponent -> foregroundStyleComponent.getForegroundColor()
        else -> Color.Black
    }

    val gravity = when (textAlign) {
        TextAlign.Start -> Gravity.START
        TextAlign.End -> Gravity.END
        TextAlign.Center -> Gravity.CENTER_HORIZONTAL
        else -> Gravity.START
    }

    Box(
        modifier = modifiers
            .buildModifier(onUiEvent)
            .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(Modifier.clipToBounds()),
        contentAlignment = modifiers.getAlignment()
    ) {
        AndroidView(
            modifier = modifiers
                .buildModifier(
                    onUiEvent,
                    exclude = listOf(LocalModifier::class)
                )
                .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(Modifier.clipToBounds()),
            factory = { ctx ->
                AndroidTextView(ctx).apply {
                    setTextColor(color.toArgb())
                    setGravity(gravity)
                    if (fontSize != null) {
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize.toFloat())
                    }
                    if (maxLines > 0) {
                        this.maxLines = maxLines
                        ellipsize = TextUtils.TruncateAt.END
                    }
                    if (fontWeight != null && fontWeight.weight >= 700) {
                        setTypeface(typeface, Typeface.BOLD)
                    }
                }
            },
            update = { textView ->
                markwon.setMarkdown(textView, text)
                textView.setTextColor(color.toArgb())
            }
        )
    }
}
