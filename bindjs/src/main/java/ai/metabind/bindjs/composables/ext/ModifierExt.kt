package ai.metabind.bindjs.composables.ext

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.composables.UiEvent
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.Component
import ai.metabind.bindjs.model.ModifiedComponent
import ai.metabind.bindjs.model.ext.namedFontWeight
import ai.metabind.bindjs.model.ext.toAlignment
import ai.metabind.bindjs.model.ext.toFontFamily
import ai.metabind.bindjs.model.ext.toTextAlign
import ai.metabind.bindjs.model.ext.toTextStyle
import ai.metabind.bindjs.model.modifier.AccessibilityLabelModifier
import ai.metabind.bindjs.model.modifier.AllowsHitTestingModifier
import ai.metabind.bindjs.model.modifier.AutocorrectionDisabledModifier
import ai.metabind.bindjs.model.modifier.BackgroundModifier
import ai.metabind.bindjs.model.modifier.BoldModifier
import ai.metabind.bindjs.model.modifier.ButtonStyleModifier
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.DisabledModifier
import ai.metabind.bindjs.model.modifier.FixedSizeModifier
import ai.metabind.bindjs.model.modifier.FontDesignModifier
import ai.metabind.bindjs.model.modifier.FontModifier
import ai.metabind.bindjs.model.modifier.FontWeightModifier
import ai.metabind.bindjs.model.modifier.ForegroundStyleModifier
import ai.metabind.bindjs.model.modifier.FrameModifier
import ai.metabind.bindjs.model.modifier.ItalicModifier
import ai.metabind.bindjs.model.modifier.LayoutPriorityModifier
import ai.metabind.bindjs.model.modifier.LineLimitModifier
import ai.metabind.bindjs.model.modifier.LineSpacingModifier
import ai.metabind.bindjs.model.modifier.LocalModifier
import ai.metabind.bindjs.model.modifier.MonospacedModifier
import ai.metabind.bindjs.model.modifier.MultilineTextAlignmentModifier
import ai.metabind.bindjs.model.modifier.OnAppearModifier
import ai.metabind.bindjs.model.modifier.OnDisappearModifier
import ai.metabind.bindjs.model.modifier.OnTapModifier
import ai.metabind.bindjs.model.modifier.OpacityModifier
import ai.metabind.bindjs.model.modifier.PickerStyleModifier
import ai.metabind.bindjs.model.modifier.ScaledToFillModifier
import ai.metabind.bindjs.model.modifier.ScaledToFitModifier
import ai.metabind.bindjs.model.modifier.StrikethroughModifier
import ai.metabind.bindjs.model.modifier.TextCaseModifier
import ai.metabind.bindjs.model.modifier.TextSelectionModifier
import ai.metabind.bindjs.model.modifier.TrackingModifier
import ai.metabind.bindjs.model.modifier.UnderlineModifier
import ai.metabind.bindjs.model.modifier.asColorComponent
import kotlin.reflect.KClass

@Composable
fun List<ComponentModifier<*>>.modifiersToShareWithChildren(): List<ComponentModifier<*>> {
    return filter { modifier ->
        return@filter when (modifier) {
            is BoldModifier,
            is FontModifier,
            is FontWeightModifier,
            is LineLimitModifier,
            is LineSpacingModifier,
            is ForegroundStyleModifier,
            is AllowsHitTestingModifier,
            is MultilineTextAlignmentModifier,
                -> true

            else -> false
        }
    }
}

@Composable
fun List<ComponentModifier<*>>.modifiersWithFrameRemoved(): List<ComponentModifier<*>> {
    return filter { modifier ->
        return@filter when (modifier) {
            is FrameModifier,
                -> false

            else -> true
        }
    }
}

/**
 * Rules:
 * 1. If component has allowsHitTestingModifier with value false - remove onTapModifier
 */
fun List<ComponentModifier<*>>.process(): List<ComponentModifier<*>> {
    val allowsHitTestingModifier =
        firstOrNull { it is AllowsHitTestingModifier } as? AllowsHitTestingModifier
    allowsHitTestingModifier?.let { allowsHitTestingModifier ->
        if (!allowsHitTestingModifier.props.rawValue) {
            return filter { it !is OnTapModifier }
        }
    }
    return this
}

@Composable
fun List<ComponentModifier<*>>.buildModifier(
    onUiEvent: (UiEvent) -> Unit,
    exclude: List<KClass<*>> = emptyList(),
): Modifier {
    var modifier: Modifier = Modifier

    forEach { componentModifier ->
        if (exclude.contains(componentModifier::class)) {
            return@forEach
        }

//        Timber.d("ComponentView: Applying modifier: $componentModifier")

        modifier = modifier.then(componentModifier.buildModifier(onUiEvent))
    }
    return modifier
}

@Composable
fun List<ComponentModifier<*>>.buildModifierFromSubset(
    onUiEvent: (UiEvent) -> Unit,
    include: List<KClass<*>>,
): Modifier {
    var modifier: Modifier = Modifier
    forEach { componentModifier ->
        if (include.contains(componentModifier::class)) {
            modifier = modifier.then(componentModifier.buildModifier(onUiEvent))
        }
    }
    return modifier
}

fun List<ComponentModifier<*>>.getFontSize(): Number? {
    return (firstOrNull { it is FontModifier } as? FontModifier)?.props?.rawValue as? Number
}

/**
 * Point size of SwiftUI's named text styles, used to size font-relative content
 * (e.g. an SF-Symbol glyph declared `1em`) the way iOS does. Mirrors the default
 * Dynamic Type sizes at the standard content-size category.
 */
fun String.namedFontPointSize(): Float? = when (this) {
    "largeTitle" -> 34f
    "title" -> 28f
    "title2" -> 22f
    "title3" -> 20f
    "headline" -> 17f
    "body" -> 17f
    "callout" -> 16f
    "subheadline" -> 15f
    "footnote" -> 13f
    "caption" -> 12f
    "caption2" -> 11f
    else -> null
}

/**
 * Resolves the effective font point size from the *nearest* `.font(...)` in the
 * chain (innermost wins, matching SwiftUI), looking through numeric sizes, named
 * text styles, and custom fonts. Modifiers accumulate outermost-first, so the
 * innermost `.font` is the last one — scan in reverse. Used to size SF-Symbol
 * SVG glyphs, which the JS bridge emits as `1em`-sized images.
 */
fun List<ComponentModifier<*>>.getNearestFontPointSize(): Float? {
    for (modifier in asReversed()) {
        if (modifier is FontModifier) {
            when (val raw = modifier.props.rawValue) {
                is Number -> return raw.toFloat()
                is String -> raw.namedFontPointSize()?.let { return it }
                is Map<*, *> -> {
                    // Custom font: { type: "CustomFont", props: { size: N, ... } }
                    val size = (raw["props"] as? Map<*, *>)?.get("size") as? Number
                    if (size != null) return size.toFloat()
                }
            }
        }
    }
    return null
}

/**
 * Weight implied by the nearest named `.font(...)` — semibold for `headline`, regular
 * otherwise. Companion to [getNearestFontPointSize]; an explicit `.fontWeight(...)`
 * (see [getFontWeight]) takes precedence over this.
 */
fun List<ComponentModifier<*>>.getNearestNamedFontWeight(): FontWeight? {
    for (modifier in asReversed()) {
        if (modifier is FontModifier) {
            (modifier.props.rawValue as? String)?.let { return it.namedFontWeight() }
        }
    }
    return null
}

@Composable
fun List<ComponentModifier<*>>.getTextStyle(): TextStyle? {
    firstOrNull { it is FontModifier }?.let { modifier ->
        val fontModifier = (modifier as FontModifier)
        if (fontModifier.props.rawValue is String) {
            return fontModifier.props.rawValue.toTextStyle()
        }
    }
    return null
}

fun List<ComponentModifier<*>>.buttonStyleModifier(): ButtonStyleModifier? {
    return firstOrNull { it is ButtonStyleModifier } as? ButtonStyleModifier
}

fun List<ComponentModifier<*>>.getTextDecoration(): TextDecoration {
    return firstOrNull { it is UnderlineModifier }?.let { modifier ->
        TextDecoration.Underline
    } ?: firstOrNull { it is StrikethroughModifier }?.let { modifier ->
        TextDecoration.LineThrough
    } ?: TextDecoration.None
}

fun List<ComponentModifier<*>>.getFontStyle(): FontStyle {
    return firstOrNull { it is ItalicModifier }?.let { modifier ->
        val italicModifier = (modifier as ItalicModifier)
        if (italicModifier.props.rawValue == null || italicModifier.props.rawValue) FontStyle.Italic else FontStyle.Normal
    } ?: FontStyle.Normal
}

fun List<ComponentModifier<*>>.getFontFamily(): FontFamily? {
    return firstOrNull { it is FontDesignModifier }?.let { modifier ->
        (modifier as FontDesignModifier).props.rawValue.toFontFamily()
    } ?: firstOrNull { it is MonospacedModifier }?.let { modifier ->
        FontFamily.Monospace
    }
}

fun List<ComponentModifier<*>>.isTextSelectionEnabled(): Boolean {
    return firstOrNull { it is TextSelectionModifier }?.let { modifier ->
        (modifier as TextSelectionModifier).props.rawValue == "enabled"
    } ?: false
}

fun List<ComponentModifier<*>>.getWeight(): Float {
    firstOrNull { it is LayoutPriorityModifier }?.let { modifier ->
        return (modifier as LayoutPriorityModifier).props.rawValue.toFloat()
    }
    return 1f
}

fun List<ComponentModifier<*>>.getMaxLines(): Int {
    return firstOrNull { it is LineLimitModifier }?.let { modifier ->
        (modifier as LineLimitModifier).props.rawValue
    } ?: Int.MAX_VALUE
}

fun List<ComponentModifier<*>>.getLineSpacing(): Float? {
    return firstOrNull { it is LineSpacingModifier }?.let { modifier ->
        (modifier as LineSpacingModifier).props.rawValue
    }
}

fun List<ComponentModifier<*>>.getTracking(): Float {
    return firstOrNull { it is TrackingModifier }?.let { modifier ->
        (modifier as TrackingModifier).props.rawValue
    } ?: 0f
}

fun List<ComponentModifier<*>>.applyTextCase(text: String): String {
    return firstOrNull { it is TextCaseModifier }?.let { modifier ->
        val textCase = (modifier as TextCaseModifier).props.rawValue
        when (textCase) {
            "uppercase" -> {
                text.uppercase()
            }

            "lowercase" -> {
                text.lowercase()
            }

            else -> {
                text
            }
        }
    } ?: text
}

fun List<ComponentModifier<*>>.getTextAlign(): TextAlign {
    return firstOrNull { it is MultilineTextAlignmentModifier }?.let { modifier ->
        (modifier as MultilineTextAlignmentModifier).props.rawValue.toTextAlign()
    } ?: TextAlign.Start
}

fun List<ComponentModifier<*>>.getContentScale(): ContentScale {
    return firstOrNull { it is ScaledToFitModifier }?.let { modifier ->
        ContentScale.Fit
    } ?: firstOrNull { it is ScaledToFillModifier }?.let { modifier ->
        ContentScale.FillBounds
    } ?: ContentScale.Fit
}

fun List<ComponentModifier<*>>.getAlignment(): Alignment {
    return firstOrNull { it is FrameModifier }?.let { modifier ->
        (modifier as FrameModifier).props.alignment.toAlignment()
    } ?: Alignment.TopStart
}

fun List<ComponentModifier<*>>.getContentDescription(): String {
    return firstOrNull { it is AccessibilityLabelModifier }?.let { modifier ->
        (modifier as AccessibilityLabelModifier).props.rawValue ?: ""
    } ?: ""
}

fun List<ComponentModifier<*>>.hasFrame(): Boolean {
    return firstOrNull { it is FrameModifier }?.let { modifier ->
        (modifier as FrameModifier)
    } != null
}

fun List<ComponentModifier<*>>.hasFixedWidth(): Boolean {
    return firstOrNull {
        it is FixedSizeModifier && it.props.horizontal == true
    } != null
}

inline fun <reified T> List<ComponentModifier<*>>.has(): Boolean {
    return firstOrNull { it is T }?.let { modifier ->
        (modifier as T)
    } != null
}

fun List<ComponentModifier<*>>.addFillWidthIfNoFrame(): List<ComponentModifier<*>> {
    return if (hasFrame()) {
        this
    } else {
        this + LocalModifier.FillMaxWidth(Modifier.fillMaxWidth())
    }
}

@Composable
fun List<ComponentModifier<*>>.getForegroundColor(): Color {
    @Composable
    fun forColorComponent(colorComponent: ColorComponent): Color {
        return if (colorComponent.isMaterial()) {
            Color.White.copy(alpha = 0.2f)
        } else {
            Color(colorComponent.color)
        }
    }

    firstOrNull { it is ForegroundStyleModifier }?.let { modifier ->
        val rawValue = (modifier as ForegroundStyleModifier).props.rawValue
        val colorComponent = rawValue.asColorComponent()
        if (colorComponent != null) {
            return forColorComponent(colorComponent)
        } else if (rawValue is Component) {
            val colorComponent = rawValue.props.children?.firstOrNull {
                it is ColorComponent
            } as? ColorComponent
            colorComponent?.let {
                return forColorComponent(it)
            }
        }
    }
    return Color.Transparent
}

@Composable
fun ColorComponent.getForegroundColor(): Color {
    return if (isMaterial()) {
        Color.White.copy(alpha = 0.2f)
    } else {
        Color(color)
    }
}

fun List<ComponentModifier<*>>.getAlpha(): Float {
    return firstOrNull { it is OpacityModifier }?.let { modifier ->
        (modifier as OpacityModifier).props.rawValue
    } ?: 1f
}

@Composable
fun List<ComponentModifier<*>>.getForegroundStyleModifierComponent(): BaseComponent<*>? {
    return lastOrNull { it is ForegroundStyleModifier }?.let { modifier ->
        val rawValue = (modifier as ForegroundStyleModifier).props.rawValue
        val colorComponent = rawValue.asColorComponent()
        if (colorComponent != null) {
            return colorComponent
        }
        if (rawValue is Component) {
            rawValue.props.children?.firstOrNull()
        } else {
            rawValue as? BaseComponent<*>
        }
    }
}

@Composable
fun List<ComponentModifier<*>>.getBackgroundComponent(): BaseComponent<*>? {
    firstOrNull { it is BackgroundModifier }?.let { modifier ->
        val content = (modifier as BackgroundModifier).props.content
        return if (content is Component) {
            content.props.children?.firstOrNull()
        } else {
            content
        }
    }
    return null
}

@Composable
fun List<ComponentModifier<*>>.getBackgroundComponents(): List<BaseComponent<*>> {
    return filter { it is BackgroundModifier }.mapNotNull {
        val content = (it as BackgroundModifier).props.content
        if (content is Component) {
            content.props.children?.firstOrNull()
        } else {
            content
        }
    }
}

fun List<ComponentModifier<*>>.getFontWeight(): FontWeight? {
    return firstOrNull { it is FontWeightModifier }?.let { modifier ->
        when ((modifier as FontWeightModifier).props.rawValue) {
            "ultraLight" -> FontWeight.ExtraLight
            "thin" -> FontWeight.Thin
            "light" -> FontWeight.Light
            "regular" -> FontWeight.Normal
            "medium" -> FontWeight.Medium
            "semibold" -> FontWeight.SemiBold
            "bold" -> FontWeight.Bold
            "heavy" -> FontWeight.ExtraBold
            "black" -> FontWeight.Black
            else -> null
        }
    } ?: firstOrNull { it is BoldModifier }?.let { modifier ->
        FontWeight.Bold
    }
}

fun List<ComponentModifier<*>>.getPickerStyle(): String {
    return firstOrNull { it is PickerStyleModifier }?.let { modifier ->
        (modifier as PickerStyleModifier).props.rawValue
    } ?: "automatic"
}

/**
 * SwiftUI's `.disabled(_:)` takes a `Bool`, so `.disabled(false)` leaves the control
 * live — reading the modifier's mere presence as "disabled" killed every conditionally
 * disabled control (A2UI's FlightCard `Select` button, which is `.disabled(selected)`,
 * never fired).
 */
fun List<ComponentModifier<*>>.isEnabled(): Boolean {
    return firstOrNull { it is DisabledModifier }?.let { modifier ->
        (modifier as DisabledModifier).props.rawValue == false
    } ?: true
}

fun List<ComponentModifier<*>>.allowsHitTesting(): Boolean {
    return firstOrNull { it is AllowsHitTestingModifier }?.let { modifier ->
        (modifier as AllowsHitTestingModifier).props.rawValue
    } ?: true
}

fun List<ComponentModifier<*>>.isAutoCorrectionDisabled(): Boolean {
    return firstOrNull { it is AutocorrectionDisabledModifier }?.let { modifier ->
        (modifier as AutocorrectionDisabledModifier).props.rawValue
    } ?: false
}

fun List<ComponentModifier<*>>.onAppearModifier(): OnAppearModifier? {
    return firstOrNull { it is OnAppearModifier }?.let { modifier ->
        modifier as OnAppearModifier
    }
}

fun List<ComponentModifier<*>>.onDisappearModifier(): OnDisappearModifier? {
    return firstOrNull { it is OnDisappearModifier }?.let { modifier ->
        modifier as OnDisappearModifier
    }
}

@Composable
fun Modifier.shadow(
    shape: Shape,
    color: Color = Color.LightGray,
    blurRadius: Dp = 3.dp,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp,
    spread: Dp = 0.dp,
) = this.drawBehind {

    val shadowSize = Size(size.width + spread.toPx(), size.height + spread.toPx())
    val outline = shape.createOutline(shadowSize, layoutDirection, this)

    val paint = Paint()
    paint.color = color

    if (blurRadius.toPx() > 0) {
        paint.asFrameworkPaint().apply {
            maskFilter = BlurMaskFilter(blurRadius.toPx(), BlurMaskFilter.Blur.NORMAL)
        }
    }
    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.translate(offsetX.toPx(), offsetY.toPx())
        canvas.drawOutline(outline, paint)
        canvas.restore()
    }
}

fun Modifier.materialBlur(
    blurRadius: Dp = 20.dp,
    overlayColor: Color = Color.White.copy(alpha = 0.2f),
): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this
            .blur(blurRadius)
            .background(overlayColor)
    } else {
        this.background(overlayColor)
    }
}

fun Modifier.invertColors() = this
    .graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }
    .drawWithContent {
        drawContent()
        drawRect(
            color = Color.White,
            blendMode = BlendMode.Difference
        )
    }

fun BaseComponent<*>.hasFixedSizeModifier(): Boolean {
    return (this as? ModifiedComponent)?.props?.modifier is FixedSizeModifier
}

/**
 * Walks the background modifier content to find an explicit `height` from a
 * simple shape background (e.g. `.background { RoundedRectangle().frame(height: 40) }`).
 *
 * Returns the height only when the *first* [FrameModifier] encountered in the
 * background's ModifiedComponent chain has an explicit `height` **without**
 * min/max constraints (which would indicate a complex layout background like
 * gradients).  Non-frame modifiers (opacity, etc.) are skipped while walking
 * the chain.
 */
fun List<ComponentModifier<*>>.getBackgroundFrameHeight(): Float? {
    val bgModifier = firstOrNull { it is BackgroundModifier } as? BackgroundModifier ?: return null
    var current: BaseComponent<*>? = bgModifier.props.content
    // Unwrap Component wrapper if needed
    if (current is Component) {
        current = current.props.children?.firstOrNull()
    }
    // Walk the ModifiedComponent chain looking for the first FrameModifier
    while (current is ModifiedComponent) {
        val modifier = current.props.modifier
        if (modifier is FrameModifier) {
            // Only return height for simple shape frames:
            // - has explicit height
            // - no maxWidth/maxHeight/minWidth/minHeight (those indicate complex layouts)
            val props = modifier.props
            return if (props.height != null &&
                props.maxWidth == null && props.maxHeight == null &&
                props.minWidth == null && props.minHeight == null
            ) {
                props.height
            } else {
                // First frame has constraints — this is a complex background, bail out
                null
            }
        }
        // Skip non-frame modifiers (opacity, etc.) and continue walking
        current = current.props.content?.firstOrNull()
    }
    return null
}
