buildscript {
    dependencies {
        // Keep in step with `kotlin` in gradle/libs.versions.toml — see the hold note there.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.10")
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
}
