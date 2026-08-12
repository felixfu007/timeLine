import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

// Release signing config, loaded from a local, gitignored `keystore.properties` (see
// android/.gitignore + the "release" skill for how it's generated). Deliberately tolerant of
// this file being absent — e.g. a fresh clone on someone else's machine, or CI without the
// secret — so `./gradlew assembleDebug` and plain compilation still work everywhere; only
// `assembleRelease` actually needs it, and produces an *unsigned* APK without it (Gradle's own
// behavior when no signingConfig is attached), which is a safe, obvious-when-you-try-to-install-it
// fallback rather than a hard build failure.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}
val hasReleaseSigningConfig = keystorePropertiesFile.exists()

android {
    namespace = "com.timeline.onthisday"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.timeline.onthisday"
        // minSdk 26 chosen so java.time (LocalDate/Instant) can be used directly for
        // month/day/cache-timestamp handling without needing core library desugaring.
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        // Required (AGP 8+ defaults this to false) because NetworkModule reads
        // BuildConfig.DEBUG / BuildConfig.VERSION_NAME for the OkHttp logging interceptor
        // and the Wikimedia-compliant User-Agent header.
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX / Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Hilt (DI)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room (local DB / cache)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit + Moshi + OkHttp (Wikipedia On This Day API client)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.okhttp.core)
    // NOTE: implementation (not debugImplementation) — NetworkModule.kt references
    // HttpLoggingInterceptor from main source set code (gated at runtime by BuildConfig.DEBUG),
    // so the class must be on the compile classpath for the release variant too.
    implementation(libs.okhttp.logging.interceptor)

    // WorkManager (background scheduling — wired up in M4)
    implementation(libs.androidx.work.runtime.ktx)

    // Glance (Widget UI — wired up in M3)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // DataStore (settings persistence — wired up in M6)
    implementation(libs.androidx.datastore.preferences)

    // AppCompat (per-app language override, F-07 — wired up in M6)
    implementation(libs.androidx.appcompat)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
