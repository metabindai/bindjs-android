package ai.metabind.bindjs.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
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
import ai.metabind.bindjs.composables.ext.getMaxLines
import ai.metabind.bindjs.composables.ext.getTextAlign
import ai.metabind.bindjs.composables.ext.getTextDecoration
import ai.metabind.bindjs.composables.ext.getTextStyle
import ai.metabind.bindjs.composables.ext.getTracking
import ai.metabind.bindjs.composables.ext.isTextSelectionEnabled
import ai.metabind.bindjs.model.BrushComponent
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.TextComponent
import ai.metabind.bindjs.model.modifier.BackgroundModifier
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.FixedSizeModifier
import ai.metabind.bindjs.model.modifier.OffsetModifier
import ai.metabind.bindjs.model.modifier.OpacityModifier
import ai.metabind.bindjs.model.modifier.ScaleEffectModifier
import ai.metabind.bindjs.model.modifier.RotationEffectModifier
import ai.metabind.bindjs.model.modifier.BlurModifier
import ai.metabind.bindjs.model.modifier.ShadowModifier
import ai.metabind.bindjs.model.modifier.BorderModifier
import ai.metabind.bindjs.model.modifier.LocalModifier
import ai.metabind.bindjs.model.modifier.PaddingModifier

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

/**
 * Modifiers the inner text node must not re-apply.
 *
 * Every branch below builds the modifier chain twice: once for the wrapping [Box] and
 * once for the text node inside it. A modifier that *paints* therefore lands twice, and
 * `.background(...)` with a colour resolves to `Modifier.background(...)` — so a badge
 * written as `Text("+130%").padding(...).background(color)` drew its fill twice: once at
 * the Box's padded size and once hugging the glyphs, because the inner chain drops
 * PaddingModifier. With a translucent colour (which is how these pills are built) the
 * overlap reads as a second, darker pill inside the first, where iOS draws one. The Box
 * already paints the background at the padded size, so the inner node skips it.
 */
/**
 * Modifiers the wrapping Box already applies, left off the inner text node. The chain is
 * built for both, and each of these compounds when applied twice: a background painted
 * a second, smaller pill inside the first; `.offset(y: 32)` moved the label 64 (the
 * Explore card's hotspot labels hung twice as far below their dots as on iOS); opacity
 * multiplied with itself, and scale, rotation, blur, shadow and border stacked.
 */
private val BACKGROUND_AND_PADDING =
    listOf(
        BackgroundModifier::class,
        PaddingModifier::class,
        LocalModifier::class,
        OffsetModifier::class,
        OpacityModifier::class,
        ScaleEffectModifier::class,
        RotationEffectModifier::class,
        BlurModifier::class,
        ShadowModifier::class,
        BorderModifier::class,
    )

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

    // Markdown and verbatim text draw through the same `Text`, so every modifier below
    // applies to both — see [inlineMarkdown].
    val markdownSource = component.props.markdownSource
    val linkColor = LocalAccentColor.current
    val text = remember(markdownSource, component.props.verbatim, linkColor, textDecoration, modifiers) {
        if (markdownSource != null) {
            inlineMarkdown(markdownSource, linkColor, textDecoration, modifiers::applyTextCase)
        } else {
            AnnotatedString(modifiers.applyTextCase(component.props.verbatim ?: ""))
        }
    }

    // The fill is the one thing that differs by foreground style: a colour, a brush, or —
    // unstyled — black everywhere except inside a Button label, which SwiftUI tints with
    // the accent colour (see LocalContentTint).
    val fill = when (foregroundStyleComponent) {
        is ColorComponent -> TextStyle(color = foregroundStyleComponent.getForegroundColor())
        is BrushComponent -> TextStyle(brush = foregroundStyleComponent.createBrush())
        else -> TextStyle(color = LocalContentTint.current ?: Color.Black)
    }

    Box(
        modifier = modifiers
            .buildModifier(onUiEvent)
            .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(modifiers.textClipModifier()),
        contentAlignment = modifiers.getAlignment()
    ) {
        // BasicText, not Material3's Text: that one always merges a colour into the style
        // (LocalContentColor when none is given), which replaces a brush, so every
        // gradient `.foregroundStyle(...)` came out as a solid colour.
        val style = TextStyle(
            platformStyle = PlatformTextStyle(
                includeFontPadding = true
            ),
            letterSpacing = tracking.sp
        ).merge(fill).merge(textStyle).merge(
            TextStyle(
                fontSize = fontSize?.toInt()?.sp ?: TextUnit.Unspecified,
                fontWeight = fontWeight,
                textAlign = textAlign,
                lineHeight = lineHeight?.sp ?: TextUnit.Unspecified,
                fontFamily = fontFamily,
                textDecoration = textDecoration,
                fontStyle = fontStyle,
            )
        )

        @Composable
        fun composeText() {
            BasicText(
                text = text,
                modifier = modifiers
                    .buildModifier(
                        onUiEvent,
                        exclude = BACKGROUND_AND_PADDING
                    )
                    .then(Modifier.wrapContentSize(modifiers.getAlignment())).then(modifiers.textClipModifier()),
                style = style,
                overflow = TextOverflow.Ellipsis,
                maxLines = maxLines,
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
