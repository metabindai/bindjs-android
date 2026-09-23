package ai.metabind.bindjs

import android.content.res.Configuration
import android.view.View
import java.util.Locale
import java.util.TimeZone

/**
 * The environment every BindJS component can rely on, whatever the host passes to
 * [JsRuntime.setEnvironment]: the screen size, display scale, color scheme, locale, time
 * zone and layout direction, read from [configuration].
 *
 * bindjs-apple's `BindJSView` builds its environment from SwiftUI's the same way, so a
 * component written against it can read `env.colorScheme` or `env.screen.width` without
 * the host supplying them. On Android nothing did unless the host remembered to — and
 * `metabind-content`'s view never calls `setEnvironment` at all — so a component sizing
 * itself off `env.screen?.width` did its arithmetic on `undefined`. The Explore card's
 * hotspot offsets came out `NaN`, and every hotspot vanished.
 *
 * The keys and values follow bindjs-apple: `screen` in dp (SwiftUI points), `displayScale`
 * as the density, `colorScheme` `light`/`dark`, `layoutDirection`
 * `leftToRight`/`rightToLeft`, `locale` as `en_US`. What the host passes wins over any of
 * them.
 */
internal fun systemEnvironment(configuration: Configuration, density: Float): Map<String, Any> =
    systemEnvironment(
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp,
        density = density,
        dark = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES,
        rightToLeft = configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL,
        locale = configuration.locales[0],
        timeZone = TimeZone.getDefault(),
    )

/** [systemEnvironment] from plain values, which is what the unit tests drive. */
internal fun systemEnvironment(
    screenWidthDp: Int,
    screenHeightDp: Int,
    density: Float,
    dark: Boolean,
    rightToLeft: Boolean,
    locale: Locale,
    timeZone: TimeZone,
): Map<String, Any> = mapOf(
    "screen" to mapOf("width" to screenWidthDp, "height" to screenHeightDp),
    "displayScale" to density,
    "colorScheme" to if (dark) "dark" else "light",
    "layoutDirection" to if (rightToLeft) "rightToLeft" else "leftToRight",
    "locale" to locale.toString(),
    "timeZone" to timeZone.id,
)
