object Versions {
    const val jooq = "3.16.12"
    const val kotlinSerialization = "1.4.1"
}

repositories {
    mavenCentral()
}

plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "1.7.22"
}

dependencies {
    implementation("org.jooq:jooq-meta-extensions:${Versions.jooq}") {
        // causes conflicts with spring-boot plugin
        exclude(group = "org.springframework")
    }
    implementation("org.jooq:jooq-codegen:${Versions.jooq}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinSerialization}")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}
