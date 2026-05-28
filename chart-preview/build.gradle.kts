plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.yapstudios.bindjs.chartpreview"
    compileSdk {
        version = release(libs.versions.android.compile.sdk.get().toInt()) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.yapstudios.bindjs.chartpreview"
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
}

dependencies {
    implementation(project(":bindjs"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.android.compose.material)
    implementation(libs.android.compose.ui)
}
