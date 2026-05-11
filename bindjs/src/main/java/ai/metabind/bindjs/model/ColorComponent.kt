package ai.metabind.bindjs.model

import android.graphics.Color
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.google.gson.annotations.SerializedName

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
    }

    fun isMaterial(): Boolean {
        return props.rawValue in MATERIALS
    }

    @Composable
    private fun colorByName(name: String): Int {
        return when (name) {
            "clear" -> Color.TRANSPARENT
            "red" -> Color.valueOf(235 / 255.0f, 78 / 255.0f, 62 / 255.0f).toArgb()
            "orange" -> Color.valueOf(255 / 255.0f, 149 / 255.0f, 0 / 255.0f).toArgb()
            "yellow" -> Color.YELLOW
            "green" -> Color.valueOf(101 / 255.0f, 196 / 255.0f, 102 / 255.0f).toArgb()
            "mint" -> Color.valueOf(.2431f, .7059f, .5373f).toArgb()
            "teal" -> Color.valueOf(0f, .5f, .5f).toArgb()
            "cyan" -> Color.CYAN
            "blue" -> Color.valueOf(50 / 255.0f, 120 / 255.0f, 247 / 255.0f).toArgb()
            "indigo" -> Color.valueOf(.3f, 0f, .5f).toArgb()
            "purple" -> Color.valueOf(.62f, .12f, .95f).toArgb()
            "pink" -> Color.valueOf(1f, .71f, .75f).toArgb()
            "brown" -> Color.valueOf(.58f, .3f, 0f).toArgb()
            "black" -> Color.BLACK
            "white" -> Color.WHITE
            "gray" -> Color.GRAY
            "primary" -> MaterialTheme.colorScheme.primary.toArgb()
            "background" -> MaterialTheme.colorScheme.background.toArgb()
            "secondary" -> MaterialTheme.colorScheme.secondary.toArgb()
            "accentColor" -> hexColor("#007AFF")
            "accent" -> hexColor("#007AFF")
            "tertiary" -> MaterialTheme.colorScheme.tertiary.toArgb()
            "quaternary" -> MaterialTheme.colorScheme.onTertiary.toArgb()
            else -> MaterialTheme.colorScheme.primary.toArgb()
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
                    b = ((hexValue and 0x00FF0000) shr 8) / 255.0f
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
