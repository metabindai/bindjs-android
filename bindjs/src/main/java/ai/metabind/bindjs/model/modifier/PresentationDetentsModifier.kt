package ai.metabind.bindjs.model.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.metabind.bindjs.composables.UiEvent
import com.google.gson.annotations.SerializedName
import ai.metabind.bindjs.model.BaseComponent
import java.io.Serializable

/**
 * `.presentationDetents([Detent.medium, Detent.large])` — the heights a sheet is
 * allowed to rest at. JS sends the `Detent` helper's plain objects through
 * `GenericModifier`, so they land under `rawValue`.
 *
 * Layout-neutral: [SheetModifier]'s presenter reads it off the modifier chain of
 * the content it just materialized, since the detents are written on the sheet's
 * root view rather than on the view presenting it.
 */
class PresentationDetentsModifier(
    props: PresentationDetentsModifierProps,
) : ComponentModifier<PresentationDetentsModifierProps>(props) {
    @Composable
    override fun buildModifier(
        onUiEvent: (UiEvent) -> Unit
    ): Modifier {
        return Modifier
    }
}

class PresentationDetentsModifierProps(
    // Nullable elements on purpose. `Detent.medium` resolves to `undefined` in a
    // component today — bindjs-runtime registers `Detent` as a helper *component*,
    // which shadows the value object it is meant to expose — so the array arrives as
    // `[null, null]`. bindjs-apple hits the same thing and falls back to `.large`,
    // which is why an iOS sheet written with two detents still opens full height.
    // Mirror that fallback rather than crashing on the nulls.
    @SerializedName("rawValue")
    private val _rawValue: List<Detent?>?,
    children: List<BaseComponent<*>>?,
) : ComponentModifierProps(children) {
    val detents: List<Detent>
        get() = _rawValue?.filterNotNull().orEmpty()
}

/** One entry of `.presentationDetents(...)`: `medium`, `large`, `fraction` or `height`. */
class Detent(
    val detentType: String?,
    val value: Float?,
) : Serializable {
    /**
     * Whether this detent rests short of full height. Compose has a single
     * partially-expanded state rather than an arbitrary set of stops, so a sheet
     * offering any short detent gets that state and the rest map onto it.
     */
    val isPartial: Boolean
        get() = detentType == "medium" || detentType == "fraction" || detentType == "height"

    override fun toString(): String = "Detent($detentType${value?.let { ", $it" } ?: ""})"
}
