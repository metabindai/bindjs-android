package ai.metabind.bindjs.model.ext

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign

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

@Composable
fun String?.toTextStyle(): TextStyle? {
    return when (this) {
        "largeTitle" -> MaterialTheme.typography.headlineLarge
        "title" -> MaterialTheme.typography.headlineMedium
        "title2" -> MaterialTheme.typography.headlineSmall
        "title3" -> MaterialTheme.typography.titleLarge
        "headline" -> MaterialTheme.typography.titleMedium
        "body" -> MaterialTheme.typography.bodyLarge
        "callout" -> MaterialTheme.typography.bodyMedium
        "subheadline" -> MaterialTheme.typography.bodySmall
        "footnote" -> MaterialTheme.typography.labelLarge
        "caption" -> MaterialTheme.typography.labelMedium
        "caption2" -> MaterialTheme.typography.labelSmall
        else -> null
    }
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
