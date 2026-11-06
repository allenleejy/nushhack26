plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 compiles Kotlin itself; this only pins the Kotlin Gradle plugin version.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
}

// Builds the debug app and runs the JVM unit tests. Must pass before any pull request.
tasks.register("quickCheck") {
    group = "verification"
    description = "Builds the debug app and runs the JVM unit tests."
    dependsOn(":app:assembleDebug", ":app:testDebugUnitTest")
}
