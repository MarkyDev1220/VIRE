pluginManagement {
    // Ensure plugin resolution repositories are present (you already have these)
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    // Explicitly register plugin versions so kotlin("parcelize") can be resolved
    plugins {
        // Add the parcelize plugin mapping with a Kotlin version that matches your project
        // Replace "1.9.10" with your project's Kotlin version if different.
        id("org.jetbrains.kotlin.plugin.parcelize") version "1.9.10"
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VIRE"
include(":VireApp")
include(":shared")