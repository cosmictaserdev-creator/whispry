import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

/**
 * Version comes from the git tag, not a hand-maintained number — the release workflow tags
 * `vX.Y.Z` and that tag IS the release, so versionName/versionCode deriving from anything else
 * would just be a second place they could drift out of sync (and the in-app updater compares
 * against exactly this tag format, see updater/SemVer.kt).
 *
 * - In CI (release.yml), GITHUB_REF_NAME is the exact tag that triggered the build.
 * - Locally, falls back to the nearest reachable tag (`git describe --tags --abbrev=0`) so debug
 *   builds still get a sane version instead of failing outright.
 * - No tags reachable at all (fresh clone before the first release) -> "0.1.0" / code 1.
 *
 * versionCode = major*10_000 + minor*100 + patch, so it's derived deterministically from the tag
 * and guaranteed to strictly increase release-over-release as long as the tags do.
 */
fun resolveVersion(): Pair<String, Int> {
    val tagPattern = Regex("^v(\\d+)\\.(\\d+)\\.(\\d+)$")

    val tag = System.getenv("GITHUB_REF_NAME")?.takeIf { tagPattern.matches(it) }
        ?: runCatching {
            providers.exec {
                commandLine("git", "describe", "--tags", "--abbrev=0")
                isIgnoreExitValue = true
            }.standardOutput.asText.get().trim()
        }.getOrNull()?.takeIf { tagPattern.matches(it) }

    val match = tag?.let { tagPattern.find(it) } ?: return "0.1.0" to 1
    val (major, minor, patch) = match.destructured
    return "$major.$minor.$patch" to (major.toInt() * 10_000 + minor.toInt() * 100 + patch.toInt())
}

val (resolvedVersionName, resolvedVersionCode) = resolveVersion()

android {
    namespace = "com.example.whispry"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.whispry"
        minSdk = 26
        targetSdk = 34
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProperties.getProperty("storeFile")
            if (storeFilePath != null) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = false
        buildConfig = true // ← this enables BuildConfig generation
        compose = true
    }

    lint {
        // CI's lintDebug is new (see ci.yml) and the codebase predates it -- baseline the
        // existing findings so CI fails on NEW issues a change introduces, not the backlog.
        baseline = file("lint-baseline.xml")
    }

}

// Diagnostics: emit per-composable stability + recomposition reports so we can see exactly which
// composables fail to skip (the real recomposition offenders) instead of guessing. Output lands in
// app/build/compose_reports/*-composables.txt (look for "restartable skippable" vs "restartable").
composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_reports")
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.compose.foundation.layout)
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.timber)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // UI & Animations
    implementation(libs.lottie.compose)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.core.splashscreen)

    // Data & Infrastructure
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    // Unit Tests
    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented Tests
    androidTestImplementation(libs.androidx.junit.ext)
    androidTestImplementation(libs.androidx.espresso)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.room.testing)
    kspAndroidTest(libs.hilt.compiler)

    implementation(libs.androidx.security.crypto)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.capsule)
    implementation(libs.backdrop)
}