# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BindJS Android is a JavaScript-driven UI rendering engine for Android using Jetpack Compose. It deserializes JSON component trees into native Android views with 30+ composables, 73 modifiers, gradient support, and a JavaScript runtime for event handlers and dynamic logic. Published as `ai.metabind:bindjs-android` to GitHub Packages.

## Build Commands

```bash
# Build the library
./gradlew :bindjs:build

# Clean build
./gradlew clean :bindjs:build

# Publish to GitHub Packages
./gradlew :bindjs:publish
```

## Architecture

**Single module**: `bindjs` — the core rendering library (the `metabind` GraphQL layer and `app` sample were split out into separate repos).

### Core Pipeline

1. **JSON → Component Tree**: GSON deserializes JSON into a polymorphic tree of `BaseComponent` subclasses using `RuntimeTypeAdapterFactory` (`GsonProvider.kt`)
2. **Recursive Rendering**: `BindJSView` walks the component tree and renders each node as a Jetpack Compose composable
3. **Modifier Application**: `ModifierExt.kt` builds Compose `Modifier` chains from the component's `ComponentModifier<T>` list
4. **JS Event Handling**: `JsRuntimeImpl` (singleton) uses `androidx.javascriptengine` (Android JS Sandbox) to execute event handler scripts

### Key Source Paths (`bindjs/src/main/java/ai/metabind/bindjs/`)

- `DesignerComponent.kt` — Root component model
- `GsonProvider.kt` — GSON config with runtime type adapter for polymorphic deserialization
- `JsRuntime.kt` / `JsRuntimeImpl.kt` — JavaScript runtime interface and implementation
- `composables/BindJSView.kt` — Main recursive renderer
- `composables/ext/ModifierExt.kt` — Modifier building/application utilities
- `model/BaseModels.kt` — `BaseComponent`, `Component`, `Props`, `BrushComponent` base classes
- `model/modifier/` — 73 modifier definitions (layout, appearance, text, visual effects, interaction, transforms, accessibility)

### Adding New Components

Each component needs: a model class in `model/` extending `Component`, a composable renderer in `composables/`, and registration in the GSON type adapter factory in `GsonProvider.kt`.

### Adding New Modifiers

Each modifier needs: a model class in `model/modifier/` extending `ComponentModifier<T>`, application logic in `ModifierExt.kt`, and registration in the GSON type adapter factory.

## Tech Stack

- **Kotlin** with Jetpack Compose, targeting Android SDK 26–36
- **Gradle 9.0.1** with Kotlin DSL and version catalog (`gradle/libs.versions.toml`)
- **Java 21** compatibility
- **Key dependencies**: Coil (image loading), SceneView (3D models), Media3/ExoPlayer (video), Markwon (markdown), GSON (JSON)
