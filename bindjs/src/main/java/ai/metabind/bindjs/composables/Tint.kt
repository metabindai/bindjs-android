package ai.metabind.bindjs.composables

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The tint SwiftUI spells `Color.accentColor` — what `Color("accent")`,
 * `Color("accentColor")` and `Color("link")` resolve to, and the colour an unstyled
 * [ButtonView] label is drawn in.
 *
 * iOS system blue by default, matching `bindjs-apple`'s untinted `.accentColor`.
 * Embedders that want BindJS content to follow their own brand tint provide a
 * different value around their `BindJSView`:
 *
 * ```kotlin
 * CompositionLocalProvider(LocalAccentColor provides MaterialTheme.colorScheme.primary) {
 *     BindJSView(…)
 * }
 * ```
 */
val LocalAccentColor = compositionLocalOf { Color(0xFF007AFF) }

/**
 * The colour unstyled text and glyphs inherit, or null outside any tinting context.
 *
 * SwiftUI tints everything inside a `Button` label with the accent colour unless the
 * label overrides it, which is why an iOS BindJS button reads as blue-on-grey while
 * the same tree rendered here used to come out black. [ButtonView] provides it;
 * `TextView` falls back to black when it is absent, so nothing outside a button
 * changes colour.
 *
 * `Modifier`-supplied colours still win: a label carrying `.foregroundStyle(...)` is
 * drawn in that colour, tint or no tint.
 */
val LocalContentTint = compositionLocalOf<Color?> { null }
