plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.yapstudios.bindjs.preview"
    compileSdk {
        version = release(libs.versions.android.compile.sdk.get().toInt()) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.yapstudios.bindjs.preview"
        minSdk = libs.versions.android.min.sdk.get().toInt()
        targetSdk = libs.versions.android.target.sdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Robolectric needs the merged resources to inflate the fixtures' AndroidViews.
        unitTests.isIncludeAndroidResources = true
    }
}

// `./gradlew :preview:testDebugUnitTest -PupdateTrees` rewrites the committed fixture
// trees instead of checking them — see FixtureTreesTest.
tasks.withType<Test>().configureEach {
    systemProperty("bindjs.updateTrees", (project.findProperty("updateTrees") != null).toString())
}

dependencies {
    implementation(project(":bindjs"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.android.compose.material)
    implementation(libs.android.compose.material.icons)
    implementation(libs.android.compose.ui)

    // Screenshot tests: Robolectric paints the fixtures on the JVM, Roborazzi records and
    // compares them. See README.md, "Preview app and screenshot tests".
    testImplementation(libs.junit)
    testImplementation(libs.gson)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
