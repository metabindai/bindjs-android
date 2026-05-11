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

    data class OnLongPress(
        val handlerId: String,
    ) : UiEvent()

    data class OnDrag(
        val handlerId: String,
    ) : UiEvent()

    data class OnNavigationTap(
        val handlerId: String,
    ) : UiEvent()

    data class OnPickerTap(
        val environmentId: String,
        val setterId: String,
        val tag: String
    ) : UiEvent()
}