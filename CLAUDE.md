# CLAUDE.md

Context for agents working in this repo. Read together with `README.md` (user-facing) — this file is the runtime/internals view.

## Project Overview

BindJS Android is a JavaScript-driven UI rendering engine for Android using Jetpack Compose. A JS isolate runs user code that emits a tree of AST directives; the Kotlin side deserializes that tree into `BaseComponent` instances and walks it through Compose composables, plus a system of ~73 modifiers, gradient/material backgrounds, video/3D/image support, and a host-callback bridge for talking back out to the embedding app (links, messages, model-context updates, MCP tool calls).

Published as `ai.metabind:bindjs-android` to GitHub Packages. The Apple counterpart lives in the `bindjs-apple` repo (mirror these semantics).

## Consumers (be aware before changing public API or runtime contracts)

```
bindjs-android   (this repo, publishes :bindjs as ai.metabind:bindjs-android)
  └── consumed by metabind-android (the :metabindai and :mcpappshost modules,
      pinned via `bindjs` in metabind-android/gradle/libs.versions.toml)
        └── exercised by metabind-android/samples/assistant-demo (the chat demo)
```

By default `metabind-android` consumes the published GitHub Packages artifact. To develop against a local checkout, uncomment the `includeBuild("../bindjs-android")` dependency-substitution block at the bottom of `metabind-android/settings.gradle.kts`.

When the Apple/Android pair diverges in semantics, treat `bindjs-apple` as the reference for what the JS runtime expects — both Android and Apple host the same bindjs-runtime-derived `script.js` so behavior must match.

## Module layout

**Two Gradle modules**: `:bindjs` (the library) and `:preview` (a standalone preview app: a drawer with Components and Charts sections, each listing fixtures — add a component fixture in `preview/.../ComponentFixtures.kt`). Every component fixture (not charts — Vico draws asynchronously) has a Roborazzi screenshot test that paints its committed component tree (`preview/src/test/resources/trees`, rendered by node from `script.js`) on Robolectric; `./gradlew :preview:verifyRoborazziDebug` checks, `recordRoborazziDebug` re-records, and `:preview:testDebugUnitTest -PupdateTrees` re-renders the trees. See README.md → "Preview app and screenshot tests". The legacy `metabind` GraphQL layer and `app` sample were split into separate repos.

```
bindjs/src/main/
  res/raw/
    script.js                      ← the JS-side runtime (~3.4k LOC): hooks
                                     (useState/useMCPHost/useEnvironment), the
                                     component AST schema, the __MCP__:: protocol, the
                                     `setMcpHost(true|false)` shim, `__resolveToolCall`,
                                     `runtime.needsRerender`. Read this when investigating
                                     anything that "should be there but isn't" — it
                                     usually is, just on the JS side.
                                     NOTE: this file is a bundled COPY of the BindJS
                                     runtime from the bindjs-runtime repo (npm
                                     @metabindai/bindjs-runtime). bindjs-runtime is the
                                     source of truth; the copy here is synced manually
                                     today. Fix runtime bugs upstream, then re-sync —
                                     don't let this copy drift.
  java/ai/metabind/bindjs/
    DesignerComponent.kt           ← root bundle: name + content (JS source) + dependencies
    JsRuntime.kt / JsRuntimeImpl.kt← isolate lifecycle, JS↔Kotlin bridge, callComponent
    GsonProvider.kt                ← polymorphic AST deserialization via RuntimeTypeAdapterFactory
                                     (every component & modifier must be registered here)
    RuntimeTypeAdapterFactory.java
    composables/
      BindJSView.kt                ← THE recursive renderer + ModifiedComponent dispatch
      ext/ModifierExt.kt           ← buildModifier, modifiersToShareWithChildren, accessors
      <Component>View.kt           ← one composable per AST component (BoxView, ColumnView, …)
    model/
      BaseModels.kt                ← BaseComponent, Component, Props, BrushComponent
      <Component>.kt               ← one per AST component
      modifier/<Modifier>.kt       ← one per modifier (~73)
```

## JS ↔ Kotlin bridge — read this before anything else

There is **no direct JS interop binding**. All JS→Kotlin communication is layered on top of `console.log` with the literal prefix `__MCP__::`. `JsRuntimeImpl.setConsoleCallback` parses these (anything else falls through as a plain `JSConsole` log).

Wire format: `__MCP__::<method>::<JSON-array-of-args>`

| JS emits | Kotlin does |
|---|---|
| `__MCP__::__rerender__::[]` | Host-independent. Fires `onRerenderRequested` listener on the main thread. **Coalesced** via `rerenderPosted` so a burst of setState calls posts one rerender. |
| `__MCP__::log::[level, …]` | `host.log(level, payload)` (payload is joined, non-string args gson-encoded) |
| `__MCP__::openLink::[url]` | `host.openLink(url)` |
| `__MCP__::sendMessage::[text]` | `host.sendMessage(text)` |
| `__MCP__::updateModelContext::[obj]` | `host.updateModelContext(obj)` |
| `__MCP__::setTimeout/setInterval/clearTimeout::[…]` | Host-independent. `JsRuntimeImpl` schedules/cancels the timer (the isolate has no event loop) and calls back into JS on fire. |
| `__MCP__::toolCall::[id, name, args]` | `dispatchToolCall` → suspend `host.toolCall(name, args)` off-main on `toolCallScope` → `payload = gson.toJson(result)` → `evaluateJavaScriptAsync("__resolveToolCall($id, $ok, $payload);")` resolves the pending JS Promise. |

Returning the other way, Kotlin → JS is **always** `evaluateJavaScriptAsync(...).await()`. There is no two-way binding API.

### `McpHost` contract details

`McpHost` (in `JsRuntime.kt`) carries default implementations for every method so embedders only override what they need:

- `openLink`, `sendMessage`, `updateModelContext`, `log` default to no-op.
- **`suspend fun toolCall(name, args): Any?` defaults to throwing `NotImplementedError("tool '<name>' not implemented by host")`** — deliberate, so missing tools surface clearly in JS as a rejected Promise rather than hanging forever. Override this in embeddings that want to forward tool calls anywhere (e.g., an MCP server).

### Error propagation across the bridge

Any throw inside `host.toolCall(...)` is caught by `dispatchToolCall`, which invokes `__resolveToolCall(id, false, errorMessage)`. The JS shim then rejects the Promise with `new Error(message)` (the exception's `message`, or stringified payload if non-string). So JS user code recovers via standard `try { await host.toolCall(...) } catch (e) { … }`.

`toolCallScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)` is intentional: one thrown tool call rejects only that one Promise without killing the channel for subsequent calls.

### Why no queueMicrotask

The androidx `JavaScriptIsolate` does **not** provide `queueMicrotask` — using it throws `ReferenceError`, which would propagate up through a `useState` setter, abort the calling async function, and leave `await host.toolCall(...)` promises hanging forever. So `runtime.needsRerender` emits its `__MCP__::__rerender__::[]` console message **synchronously**. Multiple setState calls per turn just emit multiple messages; the Kotlin side coalesces with `rerenderPosted`. Don't replace this pattern with microtasks.

### Pending tool-call lifecycle

`__pendingToolCalls` in `script.js` maps each in-flight `host.toolCall(...)` Promise to its `id`. `setMcpHost(false)` rejects all pending entries with `mcp host removed`. `setMcpHost(true)` does **not** clear them — a new host can resolve calls registered under the previous host, so be careful about lingering closures if the embedding app swaps hosts mid-flight.

## Rendering pipeline

1. **Bundle registration** — caller calls `setComponents(DesignerComponent)`. This is recursive: dependencies are registered first, then the root. JS code is stored verbatim in `runtime.components[name]`; **compilation is deferred** until the component is called. Each call site builds the component body via `new Function(`{ ${functionList} }`, body)(this.context)` — `functionList` is the destructured set of all registered runtime callbacks (`useState`, `useMCPHost`, `defineComponent`, `Color`, `VStack`, …), making them available as free variables inside user code.
2. **`setEnvironment(map)`** — replaces `runtime.environment` (used by `useEnvironment()`).
3. **`setMcpHost(host?)`** — Kotlin side stores the McpHost ref, then evaluates `setMcpHost(true)` or `setMcpHost(false)` in JS. The JS side rebuilds `runtime.mcpHost` with shims that emit `__MCP__::` console messages.
4. **`willRender()`** — **MANDATORY** before every `callComponent`. Resets `hookState.path` / `childIndex` to zero so that on the second render, the JS hook-path walker re-finds each component's hooks via the same path used the first time. Skipping this means `useState` reads from stale paths and freshly-set state looks like it never landed. This is also why the rerender-listener path in embeddings always looks like `willRender() → callComponent(...) → swap into Compose state` — not just `callComponent`.
5. **`callComponent(name, args)`** — runs the registered body, returns the AST JSON. Kotlin deserializes it via Gson (polymorphic via `RuntimeTypeAdapterFactory`) into a `BaseComponent<*>` tree.
6. **`BindJSView`** Composable receives the tree and walks it. State change → `runtime.needsRerender()` → console bridge → `onRerenderRequested.invoke()` in Kotlin → host re-runs `willRender() + callComponent(...)` and bumps a `version` int to force re-composition.

## Modifier system — the routing landmine

The AST represents `.foo().bar().baz()` chains as nested `ModifiedComponent` wrappers around the inner `BaseComponent`. Each `ModifiedComponent` carries **exactly one** modifier. Outermost wrapper = outermost JS call.

`BindJSView.ModifiedComponent` runs a `when (modifier)` dispatch:

- **Most modifiers** fall through to the `else` branch: append the modifier to `updateModifiers`, recurse into `modifierProps.content` via `InnerComponents`. The accumulated modifier list is eventually picked up at the leaf by `NonModifiedComponent`, which calls `buildModifier` to produce the Compose `Modifier` chain.
- **A handful of modifiers are special-cased** because they need their own Compose composable (Overlay, Frame, Mask, ContextMenu) or because they need to fire an effect at this specific layer (OnAppear, OnDisappear). These run their composable, then recurse via `InnerComponents`.

### `modifiersToShareWithChildren()` strips non-text modifiers

Several of those special-cased composables descend via:

```kotlin
InnerComponents(modifiers = modifiers.modifiersToShareWithChildren(), …)
```

That extension only keeps text-formatting modifiers (Font, FontWeight, ForegroundStyle, LineLimit, LineSpacing, MultilineTextAlignment, Bold, AllowsHitTesting). **Every other modifier is silently dropped before reaching children.** That means any modifier whose *handling* lives only at the leaf gets silently lost behind a `FrameModifier` / `MaskModifier` / `ContextMenuModifier` / `MenuView` label slot.

This was the OnAppear bug: `.onAppear(() => hydrate())` was being applied to a view that ended in `.frame(...).frame(...)`. The OnAppear modifier was stripped by the inner `FrameModifier`'s `modifiersToShareWithChildren()` call before the leaf could fire `LaunchedEffect`. Fix moved OnAppear/OnDisappear handling up to `ModifiedComponent`'s `when (modifier)`:

```kotlin
is OnAppearModifier -> {
    LaunchedEffect(Unit) { onUiEvent(UiEvent.OnAppear(modifier.props.handlerId)) }
    InnerComponents(..., modifiers = modifiers, ...)   // NOT updateModifiers — no leaf re-fire
}
is OnDisappearModifier -> {
    DisposableEffect(Unit) {
        onDispose { onUiEvent(UiEvent.OnDisappear(modifier.props.handlerId)) }
    }
    InnerComponents(..., modifiers = modifiers, ...)
}
```

**Rule of thumb for new effect-style modifiers**: handle them in `ModifiedComponent`'s `when` (not at the leaf in `NonModifiedComponent`), and pass un-augmented `modifiers` (not `updateModifiers`) into the recursive `InnerComponents` so the same modifier doesn't double-fire deeper in the tree.

## Adding a new component

1. **Model class** in `model/<Name>Component.kt` extending `Component` (or `BaseComponent<Props>`). Define a `<Name>Props` data class.
2. **Composable** in `composables/<Name>View.kt` with the standard signature `(jsRuntime, component, version, modifiers, onUiEvent, hasFrame)`. Use `modifiers.buildModifier(onUiEvent)` to materialize the Compose `Modifier`.
3. **Dispatch entry** in `BindJSView.ComponentInnerView`'s `when (component)` block.
4. **GSON registration** in `GsonProvider.kt` — add `.registerSubtype(<Name>Component::class.java, "<jsName>")` to the runtime type adapter factory.
5. **JS-side**: the component name must be registered as an in-built directive in `script.js` (search for `#registerBuiltInComponent`) or be supplied by user code via `defineComponent`.

## Adding a new modifier

1. **Model class** in `model/modifier/<Name>Modifier.kt` extending `ComponentModifier<T>` with a `<Name>ModifierProps` data class.
2. **Application logic** in `composables/ext/ModifierExt.kt` — add a branch in `buildModifier`'s `when` (or `process()` if it needs pre-processing) and, if relevant, an accessor (`fun List<ComponentModifier<*>>.<name>Modifier()`).
3. **GSON registration** in `GsonProvider.kt`.
4. **Decide propagation**: if the modifier needs to fire something at the layer it's attached to (rather than just contributing to the leaf's Compose `Modifier`), add a case in `ModifiedComponent`'s `when (modifier)`. Otherwise it'll be stripped behind any special-cased ancestor — see the OnAppear note above.
5. **JS-side**: list it in the `OnHandler` / `Padding` / etc registration in `script.js`, or in `#registerBuiltInModifier` for new categories.

## Native component slots (`ComponentRegistry`, `Placeholder`)

`composables/ComponentRegistry.kt` holds `ComponentRepresentable` (a composable keyed by
component name), `ComponentRegistry`, the `LocalComponentRegistry` composition local and
the public `WithComponent(name, component) { … }` wrapper. Two render paths consult it:

- `ComponentInnerView`'s `is Component` branch (a `ComponentCall`): a registered name
  replaces the body with the native composable; otherwise the body renders under
  `LocalComponentCall provides component`.
- `PlaceholderView` (`Placeholder({ name })`): needs both an enclosing `LocalComponentCall`
  and a registered `name`; it then renders the native composable with the *enclosing
  call's* props and children, not its own. Anything else draws a grey rounded rectangle;
  children written on the placeholder are deliberately ignored, matching bindjs-apple.
  `LocalResolvingPlaceholders` holds the names being resolved: the call's children *are*
  the body containing the placeholder, so a native composable that renders
  `context.Content()` would otherwise re-enter the same placeholder forever.

`ComponentCallProps.props` keeps the call's props as raw `JsonElement` (a non-object
would otherwise break the whole tree parse) and exposes `propsMap` for the composable.
The preview app registers `PreviewBadge` (`preview/.../PreviewComponents.kt`) and the
`Placeholder` fixture exercises both the resolved and the fallback paths.

## Sheets and navigation chrome

`.sheet(...)` presents its body in a Compose `ModalBottomSheet` (`SheetModifier` in
`BindJSView.kt`), mirroring `Sheet.swift` in bindjs-apple. Three things about it are
not like the other content-carrying modifiers:

- **The body is materialized, not deserialized.** JS sends `contentHandlerId`, not a
  subtree; the renderer calls `callForResultComponent` while the sheet is up and
  again after every render. Handler ids belong to the render that produced them, so
  a subtree held across a render has dead buttons.
- **JS opens the sheet; Compose closes it.** A user-driven dismissal (swipe, scrim,
  back) drops the sheet locally *first* and reports itself to JS afterwards. It has
  to: the sheet is a transparent full-screen dialog, and one left composed after
  being swiped away swallows every touch in the app.
- **`LocalHostScrollsVertically` is reset to false inside the sheet.** The sheet is
  its own window, so an embedder that scrolls its own content says nothing about
  this one. Left set, every `ScrollView` in the sheet degrades to a Column and a
  full-page sheet overflows instead of scrolling.

`NavigationStack` renders its content plus a bar it builds itself, reading
`navigationTitle` / `navigationBarTitleDisplayMode` / `toolbar` off the modifier
chain wrapping its child (`BaseComponent.unwrap()`) — SwiftUI hands those to the
container, not to the view they are written on. `ToolbarItem` placement maps
`cancellationAction` / `navigationBarLeading` / `topBarLeading` / `principal` to the
leading end and everything else to the trailing end.

**Two known bindjs-runtime bugs show up here** (both in `script.js`, both present in
bindjs-apple too — fix upstream in bindjs-runtime, then re-sync):

- `processProps` rewrites props named `set…`/`on…` into stored-handler ids
  (`setIsPresented` → `setIsPresentedId`) and deletes the original *before*
  `SheetModifier` reads it. The modifier therefore stores its own no-op fallback as
  `setIsPresentedHandlerId` and reports `dismissHandlerId: null`, so a dismissal
  never reaches the component and its `sheetMode` state goes stale. Fix:
  `SheetModifier` should prefer `props.setIsPresentedId` / `props.onDismissId`.
- `Detent` is registered with `#registerHelperComponent`, which binds the name to a
  *function*, so `Detent.medium` in component code evaluates to `undefined` and
  `.presentationDetents([Detent.medium, Detent.large])` arrives as `[null, null]`.
  Both renderers fall back to a full-height sheet. Fix: register it as a value.

## Lists

`List(...)` renders in `ListView` as a `LazyColumn` (a `Column` when
`LocalHostScrollsVertically` is set, like `ScrollView`). The list owns all the chrome:
row padding, separators, section headers/footers, the grouped backdrop and cards. Rows
are rendered with no inherited modifiers, only a fill-width hint.

- **Rows** are the children after `expandingForEach()`; a `Section` child contributes
  header, rows and footer, and runs of bare rows form a headerless section.
- **Selection** needs a `setSelection` handler on the list (`processProps` turns it into
  `setSelectionId`) and a `.tag(...)` on the row. A tap emits `UiEvent.OnListSelection`,
  which the router forwards as `callEventHandler(setSelectionId, [tag])`. The row whose
  tag equals `selection` is highlighted. Untagged rows are plain content.
- **Modifiers are read by the container, not applied.** `listStyle` and
  `scrollContentBackground` are read off the chain wrapping the list, and are in
  `modifiersToShareWithChildren()` so a `.frame(...)` between them and the list does not
  drop them. `listRowSeparator` and `listRowBackground` are read off each row's chain by
  walking its `ModifiedComponent` wrappers (like `PickerView` finds `.tag`). All four
  `buildModifier` to `Modifier`, so they are harmless wherever else they land.
- **Styles**: `automatic` / `insetGrouped` (default; grey backdrop, inset rounded white
  cards, uppercase footnote headers), `grouped` (edge-to-edge cards), and `plain` /
  `inset` / `sidebar` (rows and separators only).

The `List` and `ListPlain` preview fixtures cover both looks, selection, a custom row
background and a hidden separator.

## Stacks and lazy stacks

`VStack` / `HStack` are `ColumnView` / `RowView`; `LazyVStack` / `LazyHStack` are
`LazyColumnComponent` / `LazyRowComponent`, subclasses that lay out identically so every
`is ColumnComponent` dispatch picks them up. They exist as classes only because
`RuntimeTypeAdapterFactory` maps one label to one class and an unregistered label decodes
to `EmptyComponent`. **Neither is actually lazy**: a lazy stack inside a `ScrollView` is a
single `item` of that `LazyColumn`, so its rows all compose up front. iOS composes only
what is visible. Nothing renders wrongly; a long list just pays for itself up front.

- **Default spacing is 8dp** (`R.dimen.default_spacing`, shared with `GroupView`) —
  SwiftUI's default stack spacing, which is what a component that omits `spacing` is
  written against. It was 20dp, which stretched every unspaced stack past the iOS layout.
- **Baseline alignment.** `firstTextBaseline` / `lastTextBaseline` are not
  `Alignment.Vertical` values in Compose — a Row expresses them per child. `RowView` reads
  `baselineAlignment()`, sets the Row itself to `Alignment.Top` and hands each child a
  `LocalModifier.AlignByBaseline` carrying `alignBy(FirstBaseline / LastBaseline)`. A child
  with no baseline (a shape, an image) keeps Compose's fallback of Top, where SwiftUI would
  use its bottom edge.
- `verticalAlignment()` also accepts `leading` / `trailing`, which SwiftUI drops back to
  `center`. Kept because Android content predates the comparison; dropping them would
  re-lay-out that content without making any iOS render better.
- **`pinnedViews`** is honoured by `ScrollView.pinnedSectionHeaders`, which is the only
  place it can be: a header has to be an item of the scrolling list to be a
  `stickyHeader`, and a lazy stack is otherwise nested inside one item. A stack that sets
  it is flattened into the enclosing `LazyColumn` — each piece rendered by `ColumnView` on
  a one-child copy of the stack, so rows keep the stack's alignment, and the arrangement's
  spacing re-applied as top padding. Only a stack that asks for pinning takes that path.

Four things about `pinnedViews` are not iOS:

- `sectionFooters` parses but does not pin. Compose's lazy lists have no sticky footer.
- Only the vertical stack pins. A `LazyHStack` in a horizontal `ScrollView` is rendered by
  `RowView` and ignores the prop; `LazyRow` has the same `stickyHeader` gap.
- Compose keeps a sticky header up until the *next* sticky header displaces it, so a
  pinned header followed by non-section content stays at the top where SwiftUI would let
  it scroll away at the end of its section.
- Only the string forms (`'sectionHeaders'`, `'sectionFooters'`, `'all'`) count. The
  SwiftUI-shaped `pinnedViews: ['sectionHeaders']` is inert **on both platforms**:
  bindjs-apple reads the prop through `PinnedScrollableViews(rawValue: String(describing:
  value))`, so an array stringifies to `["sectionHeaders"]` and matches nothing. Honouring
  the array here would pin on Android and not on iOS, so `LayoutProps` reads the string
  form only — and keeps the raw prop as a `JsonElement`, since an array arriving at a
  `String` field fails the whole tree parse. Fix upstream, then relax both sides.

The `LazyVStack` and `LazyHStack` preview fixtures cover the alignments, both spacings and
the pinned headers; `PinnedSectionHeaderTest` scrolls a pinned stack, which a screenshot at
rest cannot show.

## `JsRuntimeImpl` lifecycle notes

- **Singleton via `getInstance(context)`** — there is exactly one isolate per process. Embeddings that show multiple BindJS views in the same Compose tree share state. Hook storage is keyed by `(rendererId, path)`, so distinct trees use distinct rendererIds — but the singleton's `mcpHost`, `onRerenderRequested`, and `environment` are global. The last `setMcpHost(...)` / `setOnRerenderRequested(...)` wins.
- **`initDeferred`** — the isolate is created and the main `script.js` is evaluated inside a `CoroutineScope(Dispatchers.IO).async`. Every entry point calls `awaitReady()` first.
- **`toolCallScope`** — a long-lived `SupervisorJob() + Dispatchers.IO` scope so one failing tool call doesn't kill the channel.
- **Console callback** is set on the isolate during init only if `JavaScriptSandbox.isFeatureSupported(JS_FEATURE_CONSOLE_MESSAGING)`. Without it the entire `__MCP__::` bridge is dead — including rerender signals. The crash path at startup throws if `JavaScriptSandbox.isSupported()` returns false at all.

## Build / publish

```sh
./gradlew :bindjs:build              # build the library
./gradlew clean :bindjs:build        # clean build
./gradlew :bindjs:publish            # publish to GitHub Packages (requires gpr.user/gpr.key)
```

Publishing target: `https://maven.pkg.github.com/metabindai/bindjs-android-binary`. Auth via `gpr.user`/`gpr.key` in `~/.gradle/gradle.properties` or `GITHUB_ACTOR`/`GITHUB_TOKEN` env vars with `write:packages` scope.

For coordinated releases (`bindjs-android` → `metabind-android`), publish here first, then bump the `bindjs` pin in `metabind-android/gradle/libs.versions.toml` — the samples (including `samples/assistant-demo`) build from the same Gradle project, so there is no separate demo-app re-pin. Don't publish for local verification — `metabind-android` can substitute a local checkout via the commented-out `includeBuild("../bindjs-android")` block in its `settings.gradle.kts`.


## Tech stack

- **Kotlin 2.4.10**, **AGP 9.3.1**, Compose, `compileSdk` 36, `minSdk` 26, Java 21
- `androidx.javascriptengine` (Android JS Sandbox) — isolate, console messaging, `evaluateJavaScriptAsync`
- **Gson** + custom `RuntimeTypeAdapterFactory` (vendored from Google) for polymorphic AST deserialization
- **Coil** (image loading), **SceneView** (3D models), **Media3/ExoPlayer** (video), **Markwon** (markdown), **Vico** (charts)

## Logcat tags

| Tag | What it tells you |
|---|---|
| `JsRuntimeImpl` | `setEnvironment`/`willRender`/`callComponent` lifecycle, **component-tree dumps after each render**, JS isolate errors, `Unknown MCP method` warnings |
| `JSConsole` | Any JS `console.log` that does **not** start with `__MCP__::` |
| `BindJSView` | Renderer-side failures |

Quick filter when investigating render/runtime issues:
```sh
adb logcat JsRuntimeImpl:V JSConsole:V BindJSView:V *:S
```
