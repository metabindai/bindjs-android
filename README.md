# BindJS Android

The Jetpack Compose rendering engine for BindJS. BindJS is the open component language for agent UI: write a component once, with its logic, and it renders as native SwiftUI, Jetpack Compose, and React. This engine turns the runtime's JSON view tree into native Android views, with 38 composables, 87 modifiers, gradient support, and a JavaScript runtime for event handlers and dynamic logic.

It's used by [metabind-android](https://github.com/metabindai/metabind-android), the Metabind Android SDK, to render Interactive Tool results, but works standalone against any BindJS bundle.

> [!TIP]
> BindJS powers [Metabind](https://metabind.ai) — the hosted platform for [MCP Apps](https://github.com/modelcontextprotocol/ext-apps). Turn your app's UI and APIs into a governed agent that runs in your own app and across Claude, ChatGPT, and every MCP host. **[Start free at metabind.ai](https://www.metabind.ai/signup)** · **[Read the docs](https://docs.metabind.ai)**

## Documentation

The full BindJS reference lives on [docs.metabind.ai](https://docs.metabind.ai/bindjs/introduction):

- [Introduction](https://docs.metabind.ai/bindjs/introduction) — the runtime, AST, renderers, and modifier pipeline
- [Authoring](https://docs.metabind.ai/bindjs/authoring/components) — how the components this engine renders are written
- [Component catalog](https://docs.metabind.ai/bindjs/components/layout-stacks) and [modifier catalog](https://docs.metabind.ai/bindjs/modifiers/layout-frame-and-padding) — every component and modifier, entry by entry

## The BindJS repositories

| Repo | What it is |
|---|---|
| [`bindjs-runtime`](https://github.com/metabindai/bindjs-runtime) | The core runtime and React renderer: `@metabindai/bindjs-runtime` + `@metabindai/bindjs-react` |
| [`bindjs-apple`](https://github.com/metabindai/bindjs-apple) | The SwiftUI rendering engine for iOS, macOS, visionOS, tvOS, and watchOS |
| `bindjs-android` — this repository | The Jetpack Compose rendering engine for Android |

One BindJS definition renders natively on all three surfaces. All three repos are Apache 2.0.

## Features

- **30+ composable views** — Box, Row, Column, List, Button, Text, Image, Video, Model3D, and more
- **73 modifiers** — layout, appearance, text styling, visual effects, interaction, transforms, accessibility
- **Gradient support** — linear, radial, and sweep gradients via `BrushComponent`
- **JavaScript runtime** — event handlers and dynamic logic via `androidx.javascriptengine`
- **Polymorphic deserialization** — GSON-based JSON parsing with `RuntimeTypeAdapterFactory`
- **MCP host bridge** — the native side of BindJS's `useMCPHost()` contract, so components can call tools and talk back to the embedding app
- **Native component slots** — register a composable under a component name and it renders wherever that component is called or a `Placeholder` names it

## Architecture

### Core Pipeline

1. **JSON to Component Tree** — GSON deserializes JSON into a polymorphic tree of `BaseComponent` subclasses (`GsonProvider.kt`)
2. **Recursive Rendering** — `BindJSView` walks the component tree and renders each node as a Jetpack Compose composable
3. **Modifier Application** — `ModifierExt.kt` builds Compose `Modifier` chains from each component's modifier list
4. **JS Event Handling** — `JsRuntimeImpl` executes event handler scripts via the Android JS Sandbox

### Key Dependencies

- **Coil** — image loading
- **SceneView** — 3D model rendering
- **Media3/ExoPlayer** — video playback
- **Markwon** — Markdown rendering
- **GSON** — JSON deserialization

## The MCP host bridge (useMCPHost)

The `useMCPHost()` hook is core BindJS, defined in the shared runtime ([`bindjs-runtime`](https://github.com/metabindai/bindjs-runtime)); this engine ships the native side of the contract, the `McpHost` interface. Implement it and attach via `JsRuntime.setMcpHost(...)` so components can call host capabilities:

```kotlin
val host = object : McpHost {
    override fun openLink(url: String) { /* open in browser */ }
    override fun sendMessage(message: String) { /* inject a chat turn */ }
    override fun updateModelContext(content: Map<String, Any?>) { /* buffer context */ }
    override suspend fun toolCall(name: String, args: Map<String, Any?>): Any? =
        myMcpServer.call(name, args)   // returned value resolves the JS promise
}
jsRuntime.setMcpHost(host)
```

Every method has a no-op default except `toolCall`, which throws `NotImplementedError` by default so a missing tool surfaces as a rejected JS promise instead of hanging. For the full Metabind integration (conversation loop, tool rendering, agent proxy), see [metabind-android](https://github.com/metabindai/metabind-android).

## Native components and `Placeholder`

An embedding app can supply Jetpack Compose views for parts of a component that are
better drawn natively: a map, a camera preview, a widget the app already owns. Register a
`ComponentRepresentable` under a name with `WithComponent`, wrapping the `BindJSView` that
should see it:

```kotlin
val MapView = ComponentRepresentable { context ->
    val latitude = context.props["latitude"] as? Double
    MyMap(latitude = latitude)          // any composable
    context.Content()                   // renders the children of the call, if wanted
}

WithComponent("MapView", MapView) {
    BindJSView(jsRuntime, component, version, onUiEvent)
}
```

`WithComponent` calls nest, each adding to the registry of the ones around it. A prepared
`ComponentRegistry` can also be provided directly through `LocalComponentRegistry`.

A registered name is resolved in two places in the component tree:

- **A component call.** When a component of that name is called, the composable renders
  in place of the component's body, with the call's props and children.
- **A `Placeholder` slot.** Inside a component's body, `Placeholder({ name })` renders the
  composable registered under `name`, again with the props and children of the component
  the placeholder sits in. This lets a component keep a body for the platforms and hosts
  that have no native view for it, and mark the spot where one should go:

  ```js
  const MapCard = defineComponent({
    body: (props, children) => VStack([
      Text(props.title).font('headline'),
      Placeholder({ name: 'MapView' }).frame({ height: 200 }),
    ]),
  });
  ```

A `Placeholder` whose name is not registered, or which is not inside a component call,
draws as a translucent grey rounded rectangle, sized by its modifiers like any other
shape. Children written on a placeholder are not rendered.

Props reach the composable as a `Map<String, Any?>` with numbers as `Double`. `children`
is the body the component rendered, and `context.Content()` draws it, which suits a
composable that frames a component's own content. A composable reached through a
`Placeholder` sees the same body, with the placeholder itself drawn as the grey fallback
inside it, so rendering `Content()` there does not recurse.

## Build

```bash
# Build the library
./gradlew :bindjs:build

# Clean build
./gradlew clean :bindjs:build
```

## Preview app and screenshot tests

`:preview` is a small app for eyeballing components and charts on a device or emulator.
Its drawer has two sections, Components and Charts; each lists fixtures, and tapping one
renders it through the real runtime. Fixtures are plain BindJS source in
`preview/src/main/java/ai/metabind/bindjs/preview/ComponentFixtures.kt` and
`ChartFixtures.kt`.

```sh
./gradlew :preview:installDebug
adb shell am start -n com.yapstudios.bindjs.preview/ai.metabind.bindjs.preview.MainActivity
# or straight into one fixture:
adb shell am start -n com.yapstudios.bindjs.preview/ai.metabind.bindjs.preview.MainActivity --es fixture Slider
```

Every component fixture also has a screenshot test. The JS isolate only runs inside
Android's WebView sandbox, so the tests split the work in two: node renders each fixture
to its component tree once, and the tree is committed under
`preview/src/test/resources/trees`; Robolectric then paints that tree with the real
Compose renderer on the JVM and Roborazzi compares the result with the golden image in
`preview/src/test/screenshots`. No emulator is involved and a full run takes a few seconds.

Chart fixtures are not screenshot-tested. Vico builds a chart's model in a coroutine
after Compose has reported idle, so a chart may or may not have drawn when the image is
captured; they are for eyeballing in the app only.

### Running the screenshot tests

```sh
./gradlew :preview:verifyRoborazziDebug     # compare every component fixture against its golden
./gradlew :preview:recordRoborazziDebug     # (re)write the goldens after an intended change
./gradlew :preview:compareRoborazziDebug    # like verify, but report instead of fail
```

Gradle treats the test task as up to date when nothing it reads has changed, so to force
a re-run add `--rerun-tasks`, or run `./gradlew :preview:cleanTestDebugUnitTest` first.

A failed verify writes `<fixture>_compare.png` to `preview/build/outputs/roborazzi/` with
the golden, the new rendering and a diff side by side. Goldens are recorded on a fixed
device profile (Pixel 6a, SDK 35, set in `FixtureScreenshotTest`), so record them with
Gradle rather than pasting in emulator screenshots.

### Adding or changing a fixture

1. Add or edit the entry in `ComponentFixtures.kt` (or `ChartFixtures.kt` for a chart,
   which then only needs step 5). Plain names such as `Slider` are fine: the app prefixes
   the name it registers with the runtime so a fixture cannot shadow the component it
   shows.
2. Re-render its tree. This needs `node` on the PATH:

   ```sh
   ./gradlew :preview:testDebugUnitTest -PupdateTrees --tests '*FixtureTreesTest*'
   ```

   Without the flag the same test fails when a committed tree is stale, and is skipped
   when node is not installed. Re-run it after re-syncing `script.js` too, since a runtime
   change can alter the trees.
3. Record its golden and check the image: `./gradlew :preview:recordRoborazziDebug`.
4. Commit the source, the tree JSON and the PNG together.
5. Install the app and look at it on a device too; a chart fixture has only this step.

A fixture that fetches a font by URL needs that font seeded for the test, since the JVM
run has no network; see `RemoteFonts` in `FixtureScreenshotTest.kt`. The seed files live in
`preview/src/test/resources/remote-fonts`, which must keep that name: a test-resources
directory called `fonts` would shadow the `fonts/` resource Robolectric loads Android's
system fonts from.

## Publishing

Published as `ai.metabind:bindjs-android` to GitHub Packages.

### Publish to GitHub Packages

Authentication requires a GitHub personal access token (classic) with `write:packages` scope and write access to this repository.

**Option 1 — environment variables:**

```bash
export GITHUB_ACTOR=<your-github-username>
export GITHUB_TOKEN=<your-github-token>
./gradlew :bindjs:publish
```

**Option 2 — Gradle properties:**

```bash
./gradlew :bindjs:publish -Pgpr.user=<your-github-username> -Pgpr.key=<your-github-token>
```

You can also add these to your `~/.gradle/gradle.properties`:

```properties
gpr.user=<your-github-username>
gpr.key=<your-github-token>
```

Publishing goes to the legacy binary repository at
`https://maven.pkg.github.com/metabindai/bindjs-android-binary`, which is also
where consumers resolve from. Pointing it at the public source repository was
tried and reverted: GitHub rejected the upload because the existing Maven
package is associated with the legacy repository. See the
[proposed package cutover](docs/PACKAGE_MIGRATION.md) for what has to happen
before the target can move.

The Maven coordinates remain `ai.metabind:bindjs-android`. GitHub Packages
still requires authentication for public downloads.

### Consuming the artifact

GitHub Packages requires authentication even for reading public packages. Use a
personal access token (classic) with `read:packages` scope, or a GitHub Actions
`GITHUB_TOKEN` with package read access. A token that only has `repo` scope is
not sufficient. Keep tokens in environment variables or user-level Gradle
properties, never in this repository.

Add the repository to your project's `settings.gradle.kts` (or root `build.gradle.kts`):

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/metabindai/bindjs-android")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

Then add the dependency:

```kotlin
dependencies {
    implementation("ai.metabind:bindjs-android:0.0.31")
}
```

Set credentials via `~/.gradle/gradle.properties` or environment variables as described above.

### Publish to local Maven repository

```bash
./gradlew publishToMavenLocal
```

Then in consuming projects:

```kotlin
repositories {
    mavenLocal()
}

dependencies {
    implementation("ai.metabind:bindjs-android:0.0.31")
}
```

## License

Apache License 2.0. See [`LICENSE`](LICENSE).
