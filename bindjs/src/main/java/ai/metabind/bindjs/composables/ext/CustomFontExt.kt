package ai.metabind.bindjs.composables.ext

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.FontModifier

/**
 * The platform typeface for the nearest `.font(CustomFont({ family: ... }))` in the
 * chain, or null when there is none or the family cannot be resolved.
 *
 * SwiftUI's `Font.custom(_:size:)` looks a family up among the fonts registered with
 * the system — ones the app bundles, plus any it has registered itself. The two Android
 * equivalents are a font resource the host app ships in `res/font`, and a family the
 * device already has installed, so both are tried in that order.
 *
 * A `url` names a face to download and register before use; [RemoteFontCache] does that
 * once per URL for the whole process. Until it lands the text renders in the system
 * face, and `family` is deliberately not consulted as a stand-in — bindjs-apple resolves
 * a remote font the same way, and preferring a local face of the same name here would
 * make the two platforms disagree and would swap the typeface under the reader when the
 * download completed.
 */
@Composable
internal fun List<ComponentModifier<*>>.getCustomTypeface(): Typeface? {
    val custom = customFontProps() ?: return null
    val context = LocalContext.current
    val url = custom.url

    if (url.isNullOrBlank()) {
        val family = custom.family
        return remember(family, context) { context.resolveFontFamily(family) }
    }

    // Seeded from the cache so a font already fetched earlier in the session paints on
    // the first frame instead of flashing the system face.
    val remote by produceState(RemoteFontCache.cached(url), url, context) {
        value = RemoteFontCache.typeface(context, url)
    }
    return remote
}

/**
 * The nearest custom font's declared properties. Modifiers accumulate outermost-first,
 * so the innermost `.font(...)` is the last one and wins, matching SwiftUI.
 */
internal fun List<ComponentModifier<*>>.customFontProps() =
    asReversed().firstNotNullOfOrNull { (it as? FontModifier)?.props?.custom }

private fun Context.resolveFontFamily(family: String?): Typeface? {
    if (family.isNullOrBlank()) return null

    // A font the host app bundles. Android font resource names are lowercase with
    // underscores, so "Inter Display" is shipped as `res/font/inter_display.ttf`.
    val resourceName = family.lowercase().replace(NON_RESOURCE_CHARS, "_")
    val resourceId = resources.getIdentifier(resourceName, "font", packageName)
    if (resourceId != 0) {
        runCatching { ResourcesCompat.getFont(this, resourceId) }
            .getOrNull()
            ?.let { return it }
    }

    // A family the device has installed. `Typeface.create` never fails — it hands back
    // the default face for a name it does not know — so an unknown family is only
    // detectable by that identity, and has to come back as null rather than as a
    // "resolved" default that would then override an ancestor's `.fontDesign(...)`.
    val installed = Typeface.create(family, Typeface.NORMAL)
    return installed.takeIf { it != Typeface.DEFAULT }
}

private val NON_RESOURCE_CHARS = Regex("[^a-z0-9_]")

/** The Compose [FontFamily] for a resolved platform typeface. */
internal fun Typeface.toComposeFontFamily(): FontFamily = FontFamily(this)
