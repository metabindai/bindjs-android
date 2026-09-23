package ai.metabind.bindjs.model

import androidx.compose.runtime.Composable

/**
 * `Material('thin')`, SwiftUI's translucent material, arriving as
 * `{"type":"Material","props":{"rawValue":"thin"}}`.
 *
 * A material is a style, so it can go anywhere a colour goes: on its own (bindjs-apple
 * draws `Rectangle().fill(material)`), in `.background`, `.foregroundStyle`, `.fill`,
 * `.stroke`, `.border` and `.listRowBackground`. It subclasses [ColorComponent] so every
 * `is ColorComponent` branch in the renderer takes it without a case of its own; its
 * [color] is the material's tint.
 *
 * Compose cannot blur what is *behind* a view, so the material is drawn as that tint
 * alone: a light grey at an alpha that rises with thickness. Over plain surfaces it
 * reads the way iOS does; over busy content it is a frosted wash with no blur.
 * `Modifier.blur`, used here before, blurs the view's *own* content — the text a
 * background sits behind — which is the opposite of a material.
 *
 * The names are the ones bindjs-apple parses (`Material(rawValue:)`). Anything else —
 * `bar`, `chrome`, the `{ type, opacity, blurRadius }` object form the SDK typings
 * advertise — is dropped on iOS, so it draws nothing here either.
 */
class MaterialComponent(
    props: ColorProps,
) : ColorComponent(props) {

    override val color: Int
        @Composable
        get() = tintFor(props.rawValue)

    companion object {
        // Light-mode approximations: iOS mixes a grey tint into a blur whose weight
        // grows with thickness. Not calibrated against a device render.
        private fun tint(alpha: Float): Int =
            ((alpha * 255f).toInt() shl 24) or (242 shl 16) or (242 shl 8) or 247

        private val ULTRA_THIN = tint(0.45f)
        private val THIN = tint(0.60f)
        private val REGULAR = tint(0.72f)
        private val THICK = tint(0.84f)
        private val ULTRA_THICK = tint(0.93f)

        fun tintFor(name: String?): Int = when (name) {
            "ultraThin" -> ULTRA_THIN
            "thin" -> THIN
            "regular" -> REGULAR
            "thick" -> THICK
            "ultraThick" -> ULTRA_THICK
            else -> 0
        }
    }
}
