package ai.metabind.bindjs.composables

sealed class UiEvent {
    data class OnAppear(
        val handlerId: String,
    ) : UiEvent()

    data class OnDisappear(
        val handlerId: String,
    ) : UiEvent()

    data class OnTap(
        val handlerId: String,
    ) : UiEvent()

    data class OnSwitch(
        val handlerId: String,
        val checked: Boolean
    ) : UiEvent()

    /**
     * A text input's new contents, for the JS `setText` closure.
     *
     * Text entry used to dispatch a bare [OnTap], which calls the handler with no
     * arguments — so `setText(undefined)` wiped the bound value on the first keystroke
     * and the field fell back to its placeholder. The text has to travel with the
     * event, the way [OnSwitch] carries its boolean.
     */
    data class OnTextChange(
        val handlerId: String,
        val text: String,
    ) : UiEvent()

    data class OnLongPress(
        val handlerId: String,
    ) : UiEvent()

    data class OnDrag(
        val handlerId: String,
        // SwiftUI-style gesture state forwarded to the JS handler as its first
        // argument: { phase, locationInView:{x,y}, translation:{x,y}, velocity:{x,y} }.
        // Coordinates are in dp so they round-trip 1:1 through .offset().
        val state: Map<String, Any>,
    ) : UiEvent()

    data class OnNavigationTap(
        val handlerId: String,
    ) : UiEvent()

    data class OnPickerTap(
        val environmentId: String,
        val setterId: String,
        val tag: String
    ) : UiEvent()

    data class OnChartSelection(
        val handlerId: String,
        val value: Any,
    ) : UiEvent()

    data class OnChange(
        val handlerId: String,
        val oldValue: String?,
        val newValue: String?,
    ) : UiEvent()
}
