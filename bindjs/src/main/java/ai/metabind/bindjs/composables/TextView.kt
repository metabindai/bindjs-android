package ai.metabind.bindjs.composables

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
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
import ai.metabind.bindjs.composables.ext.getNearestFontPointSize
import ai.metabind.bindjs.composables.ext.getNearestNamedFontWeight
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
import ai.metabind.bindjs.model.modifier.FixedSizeModifier
import ai.metabind.bindjs.model.modifier.LocalModifier
import ai.metabind.bindjs.model.modifier.PaddingModifier
import io.noties.markwon.Markwon
import android.widget.TextView as AndroidTextView

/**
 * Clip text to its layout bounds — except when `.fixedSize()` is present.
 *
 * `TextView` wraps every text in a `wrapContentSize` + `clipToBounds` Box, which
 * is correct for framed/truncating text. But SwiftUI's `.fixedSize()` means
 * "use the ideal size and ignore the parent's constraints," so such a label is
 * allowed to overflow its container (e.g. a hotspot annotation offset out of a
 * tiny ZStack). Clipping it to the (parent-coerced) bounds erased it entirely.
 * When a FixedSizeModifier is present we skip the clip so the label can render.
 */
private fun List<ComponentModifier<*>>.textClipModifier(): Modifier =
    if (any { it is FixedSizeModifier }) Modifier else Modifier.clipToBounds()

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
        // getFontSize() only sees a numeric `.font(20)`; a named `.font("title3")` has
        // to come from the point-size ladder, or `.lineSpacing(...)` on a named style
        // would compute its leading from the 16f fallback instead of the real size.
        val baseFontSize = fontSize?.toFloat()
            ?: modifiers.getNearestFontPointSize()
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
                        .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(modifiers.textClipModifier()),
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
                                .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(modifiers.textClipModifier()),
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
                        .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(modifiers.textClipModifier()),
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
                                .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(modifiers.textClipModifier()),
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
                        .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(modifiers.textClipModifier()),
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
                                .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(modifiers.textClipModifier()),
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
                                // Unstyled text is black everywhere except inside a
                                // Button label, which SwiftUI tints with the accent
                                // colour — see LocalContentTint.
                                color = LocalContentTint.current ?: Color.Black,
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

    // Most BindJS text arrives as `Text({ markdown: ... })` — it is the only spelling
    // that renders on both the Compose and SwiftUI backends — so this branch, not the
    // Compose one, is where named font styles have to be honoured. `fontSize` here comes
    // from getFontSize(), which only sees a numeric `.font(20)`; without the ladder
    // fallback every `.font("title2")` collapsed to the AndroidView default size.
    val effectiveSizeSp = fontSize?.toFloat() ?: modifiers.getNearestFontPointSize()

    // An explicit `.fontWeight(...)` wins, else the named style's own weight. The old
    // `>= 700` test dropped semibold (600), which is exactly what A2UIText applies to
    // h1–h4, so every heading rendered regular.
    val effectiveWeight = (fontWeight ?: modifiers.getNearestNamedFontWeight())?.weight

    val color = when (foregroundStyleComponent) {
        is ColorComponent -> foregroundStyleComponent.getForegroundColor()
        else -> LocalContentTint.current ?: Color.Black
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
            .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(modifiers.textClipModifier()),
        contentAlignment = modifiers.getAlignment()
    ) {
        AndroidView(
            modifier = modifiers
                .buildModifier(
                    onUiEvent,
                    exclude = listOf(LocalModifier::class)
                )
                .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(modifiers.textClipModifier()),
            factory = { ctx ->
                MarkdownTextView(ctx).apply {
                    setGravity(gravity)
                    if (maxLines > 0) {
                        this.maxLines = maxLines
                        ellipsize = TextUtils.TruncateAt.END
                    }
                }
            },
            // Typography is applied here rather than in `factory` so a re-render that
            // changes the style actually lands — AndroidView only runs `factory` once.
            update = { textView ->
                markwon.setMarkdown(textView, text)
                textView.setTextColor(color.toArgb())
                if (effectiveSizeSp != null) {
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, effectiveSizeSp)
                }
                textView.applyWeight(effectiveWeight)
            }
        )
    }
}

/**
 * A markdown TextView that lets touches through unless it actually rendered a link.
 *
 * `Markwon.setMarkdown` installs a `LinkMovementMethod`, and `setMovementMethod` makes
 * the view clickable and long-clickable — so `View.onTouchEvent` returned true for every
 * touch, the `AndroidView` interop consumed the pointer, and the enclosing Compose
 * gesture was cancelled. Since practically all BindJS text is `Text({ markdown: … })`,
 * that killed taps anywhere over a label: the A2UI FlightCard's `Select` button fired
 * only when tapped on the padding *around* its text.
 *
 * Declining the touch here rather than clearing `movementMethod`/`isClickable` in the
 * `update` block: those setters call `checkForRelayout()`, and a `requestLayout()` raised
 * from inside `update` left the interop view measured at zero until the *next* re-render,
 * so every re-rendered markdown row went blank.
 */
private class MarkdownTextView(context: Context) : AndroidTextView(context) {
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Text with a link keeps the platform behaviour — that tap belongs to the link.
        val spanned = text as? Spanned
        val hasLink = spanned != null &&
                spanned.getSpans(0, spanned.length, ClickableSpan::class.java).isNotEmpty()
        return if (hasLink) super.onTouchEvent(event) else false
    }
}

/**
 * Apply a numeric font weight to a platform TextView.
 *
 * API 28+ can set an arbitrary weight, so semibold (600) renders as semibold rather than
 * being rounded to bold. Below that, Typeface only offers normal/bold, so anything at or
 * above semibold becomes bold — closer than dropping it, which is what the previous
 * `>= 700` check did to every A2UI heading.
 */
private fun AndroidTextView.applyWeight(weight: Int?) {
    val base = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    if (weight == null) {
        typeface = base
        return
    }
    typeface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Typeface.create(base, weight, false)
    } else {
        Typeface.create(base, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
    }
}
