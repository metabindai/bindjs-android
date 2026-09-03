package ai.metabind.bindjs.composables

import ai.metabind.bindjs.JsRuntime

/**
 * Routes a [UiEvent] from [BindJSView] back into the runtime that produced the tree.
 *
 * `BindJSView` reports what the user did and leaves the dispatch to its host, which
 * means every host has had to re-derive the same `when` — and the mapping is not
 * obvious in two places. [UiEvent.OnDrag] must *not* be followed by an explicit render
 * (bindjs coalesces drags internally and drives its own re-render through
 * [JsRuntime.setOnRerenderRequested]; rendering per event rebuilds the backlog that
 * coalescing exists to prevent), and the events carrying a value have to forward it —
 * a bare handler call means `setText(undefined)`, which wipes the bound value on the
 * first keystroke.
 *
 * [onRendered] runs after the discrete events, so the tree is re-fetched. Hosts that
 * lean on the rerender listener alone can leave it at the default; doing both is
 * harmless, since a redundant render is one memoised pass.
 *
 * One event is deliberately mechanical here: [UiEvent.OnNavigationTap] just calls its
 * handler. A host that implements navigation should intercept it before delegating and
 * push the result of [JsRuntime.callForResultComponent] as a new screen.
 */
suspend fun JsRuntime.routeUiEvent(event: UiEvent, onRendered: suspend () -> Unit = {}) {
    when (event) {
        is UiEvent.OnTap -> callEventHandler(event.handlerId)
        is UiEvent.OnAppear -> callEventHandler(event.handlerId)
        is UiEvent.OnDisappear -> callEventHandler(event.handlerId)
        is UiEvent.OnLongPress -> callEventHandler(event.handlerId)
        is UiEvent.OnNavigationTap -> callEventHandler(event.handlerId)
        is UiEvent.OnSwitch -> callEventHandler(event.handlerId, arrayOf(event.checked))
        is UiEvent.OnTextChange -> callEventHandler(event.handlerId, arrayOf(event.text))
        is UiEvent.OnChartSelection -> callEventHandler(event.handlerId, arrayOf(event.value))
        is UiEvent.OnChange -> callEventHandler(
            event.handlerId,
            arrayOf(event.oldValue ?: "", event.newValue ?: ""),
        )

        is UiEvent.OnPickerTap -> callPickerSetter(event.setterId, event.tag)

        // Coalesced and serialized inside bindjs (latest-wins on the `changed` phase),
        // and it drives its own re-render through the rerender listener. No handler call
        // and no render per event.
        is UiEvent.OnDrag -> {
            dispatchDragEvent(event.handlerId, event.state)
            return
        }
    }

    onRendered()
}
