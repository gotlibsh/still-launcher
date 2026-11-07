plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release signing comes from environment variables (set by the GitHub release workflow).
// The key itself never lives in this repo.
val releaseKeystore: String? = System.getenv("STILL_KEYSTORE_FILE")
val buildNumber: Int = System.getenv("STILL_VERSION_CODE")?.toIntOrNull() ?: 1

android {
    namespace = "com.gotlibsh.still"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gotlibsh.still"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
        manifestPlaceholders["appLabel"] = "Still"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("STILL_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("STILL_KEY_ALIAS")
                keyPassword = System.getenv("STILL_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Local builds install as a separate "Still dev" app, so they never clash
            // with the signed release that Obtainium keeps up to date.
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            manifestPlaceholders["appLabel"] = "Still dev"
        }
        release {
            isMinifyEnabled = false
            signingConfig = if (releaseKeystore != null) {
                signingConfigs.getByName("release")
            } else {
                // Without the release key (local builds), fall back to the debug key.
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.home)
    implementation(libs.play.services.home.types)

    testImplementation(libs.junit)
    // Real org.json for JVM unit tests (Android's built-in copy is a stub off-device).
    testImplementation(libs.org.json)
}
