package ai.metabind.bindjs.model.ext

import ai.metabind.bindjs.composables.ext.namedFontPointSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

fun String?.toAlignment(): Alignment {
    return when (this) {
        "leading" -> Alignment.TopStart
        "trailing" -> Alignment.TopEnd
        "center" -> Alignment.Center
        "top" -> Alignment.TopCenter
        "bottom" -> Alignment.BottomCenter
        "bottomLeading" -> Alignment.BottomStart
        "bottomTrailing" -> Alignment.BottomEnd
        "topLeading" -> Alignment.TopStart
        "topTrailing" -> Alignment.TopEnd
        else -> Alignment.Center
    }
}

fun String?.toTextAlign(): TextAlign {
    return when (this) {
        "leading" -> TextAlign.Start
        "trailing" -> TextAlign.End
        "center" -> TextAlign.Center
        else -> TextAlign.Start
    }
}

fun String?.toContentScale(): ContentScale? {
    return when (this) {
        "fit" -> ContentScale.Fit
        "fill" -> ContentScale.Crop
        else -> null
    }
}

fun String?.toFontFamily(): FontFamily {
    return when (this) {
        "monospaced" -> FontFamily.Monospace
        "rounded" -> FontFamily.SansSerif
        "serif" -> FontFamily.Serif
        else -> FontFamily.Default
    }
}

/**
 * Weight of SwiftUI's named text styles. Every style is Regular except `headline`,
 * which SwiftUI renders semibold.
 *
 * An explicit `.fontWeight(...)` still wins: `TextView` passes that as `Text`'s own
 * `fontWeight` parameter, which overrides whatever the style carries.
 */
internal fun String.namedFontWeight(): FontWeight =
    if (this == "headline") FontWeight.SemiBold else FontWeight.Normal

/**
 * SwiftUI's named text styles, built from the same point-size ladder that sizes
 * font-relative content — see [namedFontPointSize], which is the single source of truth
 * for both.
 *
 * This previously mapped each name onto a `MaterialTheme.typography` slot, which
 * diverged from iOS four ways:
 *
 * - **Sizes were off by up to 3pt, and not uniformly.** `title2`/`title3` came out 2sp
 *   *larger* while `largeTitle` came out 2sp smaller, compressing the heading ramp; and
 *   `subheadline` rendered 12sp against iOS's 15 — a 20% difference on the style
 *   `A2UIFlightCard` uses for its primary labels.
 * - **Weights differed.** M3's `label*` styles are Medium, so `footnote`, `caption` and
 *   `caption2` all rendered heavier than iOS's Regular.
 * - **The ramp moved with the host app's `Typography`,** so one surface looked different
 *   in two Android apps while being fixed across iOS apps. For a renderer that is
 *   backwards: `title3` names a step on a ramp, not "whatever this host calls
 *   titleLarge".
 * - **It disagreed with [namedFontPointSize],** which mirrors iOS correctly. So
 *   `Image(systemName:).font("title3")` sized its glyph to 20 while adjacent text drew
 *   at 22sp — a mismatch impossible on iOS, where both derive from the same `.title3`.
 *
 * `lineHeight` and `letterSpacing` are deliberately left unspecified: SwiftUI takes both
 * from the font's own metrics rather than a design-token table, so unspecified (which
 * falls back to those metrics) is the closer match. `TextView` still layers
 * `.lineSpacing(...)` and `.tracking(...)` on top when the JS asks for them.
 *
 * Stays `@Composable` even though it no longer reads composition — dropping the
 * annotation changes the JVM signature and would break anything already compiled
 * against this published SDK.
 */
@Composable
fun String?.toTextStyle(): TextStyle? {
    val name = this ?: return null
    val pointSize = name.namedFontPointSize() ?: return null
    return TextStyle(
        fontSize = pointSize.sp,
        fontWeight = name.namedFontWeight(),
    )
}

fun String?.toBlendMode(): BlendMode {
    return when (this) {
        "multiply" -> BlendMode.Multiply
        "screen" -> BlendMode.Screen
        "overlay" -> BlendMode.Overlay
        "darken" -> BlendMode.Darken
        "lighten" -> BlendMode.Lighten
        "colorDodge" -> BlendMode.ColorDodge
        "colorBurn" -> BlendMode.ColorBurn
        "softLight" -> BlendMode.Softlight
        "hardLight" -> BlendMode.Hardlight
        "difference" -> BlendMode.Difference
        "exclusion" -> BlendMode.Exclusion
        "hue" -> BlendMode.Hue
        "saturation" -> BlendMode.Saturation
        "color" -> BlendMode.Color
        "luminosity" -> BlendMode.Luminosity
        "sourceAtop" -> BlendMode.SrcAtop
        "destinationOver" -> BlendMode.DstOver
        "destinationOut" -> BlendMode.DstOut
        "plusDarker" -> BlendMode.Plus
        "plusLighter" -> BlendMode.Plus
        else -> BlendMode.SrcOver
    }
}

fun String?.offset(): Offset? {
    return when (this) {
        "leading" -> Offset(0f, 0.5f)
        "trailing" -> Offset(Float.POSITIVE_INFINITY, 0.5f)
        "center" -> Offset(0.5f, 0.5f)
        "top" -> Offset(0.5f, 0f)
        "bottom" -> Offset(0.5f, Float.POSITIVE_INFINITY)
        "bottomLeading" -> Offset(0f, Float.POSITIVE_INFINITY)
        "bottomTrailing" -> Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        "topLeading" -> Offset(0f, 0f)
        "topTrailing" -> Offset(Float.POSITIVE_INFINITY, 0f)
        else -> null
    }
}
