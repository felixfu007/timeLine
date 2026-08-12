// Top-level build file: declares plugin versions used by sub-modules but does not apply them here.
// See android/gradle/libs.versions.toml for the actual version numbers (version catalog).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
}
