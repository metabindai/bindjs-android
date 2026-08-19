package ai.metabind.bindjs.model

import android.graphics.Color
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.composables.LocalAccentColor

class ColorComponent(
    props: ColorProps,
) : BaseComponent<ColorProps>(props) {

    val color: Int
        @Composable
        get() {
            props.rawValue?.let { color ->
                val colorVal = if (color.startsWith("#")) {
                    hexColor(color)
                } else {
                    colorByName(color)
                }

                return if (props.opacity != null) {
                    val alpha = props.opacity.times(255.0f).toInt()
                    ColorUtils.setAlphaComponent(colorVal, alpha)
                } else {
                    colorVal
                }
            }
            return Color.valueOf(
                (props.r?.div(255.0f)) ?: 0f,
                (props.g?.div(255.0f)) ?: 0f,
                (props.b?.div(255.0f)) ?: 0f,
                props.a ?: 1f
            ).toArgb()
        }

    companion object {
        private const val TAG = "ColorComponent"

        private val MATERIALS = listOf(
            "thin",
            "regular",
            "thick",
            "ultraThin",
            "bar",
            "chrome"
        )

        // Packed by hand rather than through `Color.argb`: the companion initialises
        // during JVM unit tests, where android.graphics is a throwing stub.
        private fun argb(r: Int, g: Int, b: Int, a: Float = 1f): Int =
            ((a * 255f).toInt() shl 24) or (r shl 16) or (g shl 8) or b

        // SwiftUI's named hues, light mode. `bindjs-apple` resolves these through
        // SwiftUI (`Color.red`, `Color.indigo`, …), which are *not* the same as
        // UIKit's `systemRed`/`systemIndigo` — SwiftUI's are authored in Display P3
        // and are visibly more vivid than the sRGB values Apple's HIG tables list.
        // The marked values were sampled from an iOS render and converted P3 → sRGB,
        // so they reproduce what the device actually draws; the rest fall back to the
        // HIG sRGB values (close, but not calibrated against a real render).
        //
        // The previous palette mixed CSS/Android defaults into this list, which is
        // what made `indigo` a near-black purple (CSS #4B0082) and `pink` a pastel
        // (CSS #FFC0CB) instead of the vivid iOS hues.
        private val SYSTEM_RED = argb(255, 57, 60)      // measured
        private val SYSTEM_ORANGE = argb(255, 141, 40)  // measured
        private val SYSTEM_YELLOW = argb(255, 204, 2)   // measured
        private val SYSTEM_GREEN = argb(53, 199, 89)    // measured
        private val SYSTEM_MINT = argb(0, 199, 190)     // HIG sRGB
        private val SYSTEM_TEAL = argb(3, 195, 209)     // measured (thin sample)
        private val SYSTEM_CYAN = argb(50, 173, 230)    // HIG sRGB
        private val SYSTEM_BLUE = argb(0, 136, 255)     // measured
        private val SYSTEM_INDIGO = argb(97, 85, 245)   // measured
        private val SYSTEM_PURPLE = argb(175, 82, 222)  // HIG sRGB
        private val SYSTEM_PINK = argb(255, 44, 85)     // measured
        private val SYSTEM_BROWN = argb(162, 132, 94)   // HIG sRGB
        private val SYSTEM_GRAY = argb(142, 142, 147)   // HIG sRGB

        // UIKit's semantic palette, light-mode values. bindjs-apple resolves these
        // through UIColor so they adapt to dark mode; this renderer draws unstyled
        // text as black regardless of theme, so adaptive fills here would put black
        // labels on near-black surfaces. Light values keep the pair readable — swap
        // both together if the renderer ever gains a colour scheme.
        private val LABEL = argb(0, 0, 0)
        private val SECONDARY_LABEL = argb(60, 60, 67, 0.60f)
        private val TERTIARY_LABEL = argb(60, 60, 67, 0.30f)
        private val QUATERNARY_LABEL = argb(60, 60, 67, 0.18f)

        private val SYSTEM_BACKGROUND = argb(255, 255, 255)
        private val SECONDARY_SYSTEM_BACKGROUND = argb(242, 242, 247)

        private val SYSTEM_GRAY_2 = argb(174, 174, 178)
        private val SYSTEM_GRAY_3 = argb(199, 199, 204)
        private val SYSTEM_GRAY_4 = argb(209, 209, 214)
        private val SYSTEM_GRAY_5 = argb(229, 229, 234)
        private val SYSTEM_GRAY_6 = argb(242, 242, 247)

        private val SYSTEM_FILL = argb(120, 120, 128, 0.20f)
        private val SECONDARY_SYSTEM_FILL = argb(120, 120, 128, 0.16f)
        private val TERTIARY_SYSTEM_FILL = argb(118, 118, 128, 0.12f)
        private val QUATERNARY_SYSTEM_FILL = argb(116, 116, 128, 0.08f)

        private val SEPARATOR = argb(60, 60, 67, 0.29f)
        private val OPAQUE_SEPARATOR = argb(198, 198, 200)
    }

    fun isMaterial(): Boolean {
        return props.rawValue in MATERIALS
    }

    /**
     * Mirrors `bindjs-apple`'s `Color.namedColors`. The names are SwiftUI/UIKit's, so
     * their meanings have to be too: `primary`/`secondary` are *label* colours in
     * SwiftUI, not a Material scheme's brand roles, and the unknown-name fallback is
     * the label colour rather than an accent. Resolving them against
     * `MaterialTheme.colorScheme` painted A2UI's grey button fills and secondary text
     * in the theme's purple.
     */
    @Composable
    private fun colorByName(name: String): Int {
        return when (name) {
            "clear" -> Color.TRANSPARENT
            "red" -> SYSTEM_RED
            "orange" -> SYSTEM_ORANGE
            "yellow" -> SYSTEM_YELLOW
            "green" -> SYSTEM_GREEN
            "mint" -> SYSTEM_MINT
            "teal" -> SYSTEM_TEAL
            "cyan" -> SYSTEM_CYAN
            "blue" -> SYSTEM_BLUE
            "indigo" -> SYSTEM_INDIGO
            "purple" -> SYSTEM_PURPLE
            "pink" -> SYSTEM_PINK
            "brown" -> SYSTEM_BROWN
            "black" -> Color.BLACK
            "white" -> Color.WHITE
            "gray", "systemGray" -> SYSTEM_GRAY

            "accent", "accentColor", "link" -> LocalAccentColor.current.toArgb()

            // Labels
            "primary", "label" -> LABEL
            "secondary", "secondaryLabel" -> SECONDARY_LABEL
            "tertiary", "tertiaryLabel", "placeholderText" -> TERTIARY_LABEL
            "quaternary", "quaternaryLabel" -> QUATERNARY_LABEL

            // Backgrounds. `background` is SwiftUI's `.systemBackground`, and the
            // grouped variants alternate the same two greys iOS does.
            "background", "systemBackground", "tertiarySystemBackground",
            "secondarySystemGroupedBackground" -> SYSTEM_BACKGROUND

            "secondarySystemBackground", "systemGroupedBackground",
            "tertiarySystemGroupedBackground" -> SECONDARY_SYSTEM_BACKGROUND

            "systemGray2" -> SYSTEM_GRAY_2
            "systemGray3" -> SYSTEM_GRAY_3
            "systemGray4" -> SYSTEM_GRAY_4
            "systemGray5" -> SYSTEM_GRAY_5
            "systemGray6" -> SYSTEM_GRAY_6

            // Fills — the translucent greys behind buttons, chips and fields.
            "systemFill" -> SYSTEM_FILL
            "secondarySystemFill" -> SECONDARY_SYSTEM_FILL
            "tertiarySystemFill" -> TERTIARY_SYSTEM_FILL
            "quaternarySystemFill" -> QUATERNARY_SYSTEM_FILL

            "separator" -> SEPARATOR
            "opaqueSeparator" -> OPAQUE_SEPARATOR

            else -> LABEL
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun hexColor(value: String): Int {
        try {
            val stringValue = value.trim().replace("#", "")
            val hexValue = stringValue.hexToLong()

            val r: Float
            val g: Float
            val b: Float
            val a: Float

            when (stringValue.length) {
                3 -> {
                    r = ((hexValue and 0xF00) shr 8) / 15.0f
                    g = ((hexValue and 0x0F0) shr 4) / 15.0f
                    b = (hexValue and 0x00F) / 15.0f
                    a = 1.0f
                }

                4 -> {
                    r = ((hexValue and 0xF000) shr 12) / 15.0f
                    g = ((hexValue and 0x0F00) shr 8) / 15.0f
                    b = ((hexValue and 0x00F0) shr 4) / 15.0f
                    a = (hexValue and 0x000F) / 15.0f
                }

                6 -> {
                    r = ((hexValue and 0xF00000) shr 16) / 255.0f
                    g = ((hexValue and 0x00FF00) shr 8) / 255.0f
                    b = (hexValue and 0x0000FF) / 255.0f
                    a = 1.0f
                }

                8 -> {
                    r = ((hexValue and 0xFF000000) shr 24) / 255.0f
                    g = ((hexValue and 0x00FF0000) shr 16) / 255.0f
                    b = ((hexValue and 0x0000FF00) shr 8) / 255.0f
                    a = (hexValue and 0x000000FF) / 255.0f
                }

                else -> {
                    r = 0.0f
                    g = 0.0f
                    b = 0.0f
                    a = 0.0f
                }
            }

            return Color.valueOf(r, g, b, a).toArgb()
        } catch (e: Exception) {
            Log.e(TAG, "Cannot convert hex value $value to Color", e)
            return Color.TRANSPARENT
        }
    }
}

class ColorProps(
    children: List<BaseComponent<*>>? = null,
    @SerializedName(value = "rawValue", alternate = ["value"])
    val rawValue: String?,
    val r: Float? = null,
    val g: Float? = null,
    val b: Float? = null,
    val a: Float? = null,
    val opacity: Float? = null,
) : Props(children = children) {
    override fun toString(): String {
        return "ColorProps(rawValue=$rawValue, r=$r, g=$g, b=$b, a=$a)"
    }
}
