# BindJS Android

A JavaScript-driven UI rendering engine for Android using Jetpack Compose. BindJS deserializes JSON component trees into native Android views with 30+ composables, 73 modifiers, gradient support, and a JavaScript runtime for event handlers and dynamic logic.

It's used by [metabind-android](https://github.com/metabindai/metabind-android), the Metabind Android SDK, to render Interactive Tool results, but works standalone against any BindJS bundle.

> [!TIP]
> BindJS powers [Metabind](https://metabind.ai) — the hosted platform for MCP Apps. Turn your app's UI and APIs into a governed agent that runs in your own app and across Claude, ChatGPT, and every MCP host. **[🚀 Start free at metabind.ai](https://metabind.ai)** · **[📖 Read the docs](https://docs.metabind.ai)**

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

- **30+ composable views** — Box, Row, Column, Button, Text, Image, Video, Model3D, and more
- **73 modifiers** — layout, appearance, text styling, visual effects, interaction, transforms, accessibility
- **Gradient support** — linear, radial, and sweep gradients via `BrushComponent`
- **JavaScript runtime** — event handlers and dynamic logic via `androidx.javascriptengine`
- **Polymorphic deserialization** — GSON-based JSON parsing with `RuntimeTypeAdapterFactory`
- **MCP host bridge** — the native side of BindJS's `useMCPHost()` contract, so components can call tools and talk back to the embedding app

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

## Build

```bash
# Build the library
./gradlew :bindjs:build

# Clean build
./gradlew clean :bindjs:build
```

## Publishing

Published as `ai.metabind:bindjs-android` to GitHub Packages.

### Publish to GitHub Packages

Authentication requires a GitHub personal access token with `write:packages` scope.

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

Artifacts are published to `https://maven.pkg.github.com/metabindai/bindjs-android-binary`.

### Consuming the artifact

GitHub Packages requires authentication even for reading packages. You need a personal access token with `read:packages` scope.

Add the repository to your project's `settings.gradle.kts` (or root `build.gradle.kts`):

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/metabindai/bindjs-android-binary")
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
    implementation("ai.metabind:bindjs-android:0.0.20")
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
    implementation("ai.metabind:bindjs-android:0.0.20")
}
```

## License

Apache License 2.0. See [`LICENSE`](LICENSE).
