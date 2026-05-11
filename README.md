# BindJS Android

A JavaScript-driven UI rendering engine for Android using Jetpack Compose. BindJS deserializes JSON component trees into native Android views with 30+ composables, 73 modifiers, gradient support, and a JavaScript runtime for event handlers and dynamic logic.

## Features

- **30+ composable views** — Box, Row, Column, Button, Text, Image, Video, Model3D, and more
- **73 modifiers** — layout, appearance, text styling, visual effects, interaction, transforms, accessibility
- **Gradient support** — linear, radial, and sweep gradients via `BrushComponent`
- **JavaScript runtime** — event handlers and dynamic logic via `androidx.javascriptengine`
- **Polymorphic deserialization** — GSON-based JSON parsing with `RuntimeTypeAdapterFactory`

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
    implementation("ai.metabind:bindjs-android:0.0.3")
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
    implementation("ai.metabind:bindjs-android:0.0.3")
}
```
