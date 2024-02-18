repositories {
    mavenCentral()
}

plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.jooq.metaExtensions) {
        // causes conflicts with spring-boot plugin
        exclude(group = "org.springframework")
    }
    implementation(libs.jooq.codegen)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.snakeYaml)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}
