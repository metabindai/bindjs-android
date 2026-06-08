package ai.metabind.bindjs.model

import com.google.gson.JsonElement

/**
 * SwiftUI `ContentUnavailableView` — the empty/error placeholder tool UIs use
 * (e.g. the interior designer when image generation fails). `title`,
 * `description` and `label` may each arrive as either a nested component
 * directive or a raw string, so they're kept as raw [JsonElement] and resolved
 * at render time (see ContentUnavailableView composable) — typing them as
 * BaseComponent would crash the whole tree parse on the string form.
 * `children` carries the optional action components.
 */
class ContentUnavailableViewComponent(
    props: ContentUnavailableViewProps,
) : BaseComponent<ContentUnavailableViewProps>(props)

class ContentUnavailableViewProps(
    val title: JsonElement?,
    val description: JsonElement?,
    val label: JsonElement?,
    val systemImage: String?,
    children: List<BaseComponent<*>?>?,
) : Props(children = children)
