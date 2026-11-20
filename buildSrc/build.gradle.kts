repositories {
    mavenCentral()
}

plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version embeddedKotlinVersion
}

gradlePlugin {
    plugins {
        create("saHotReload") {
            id = "sa.hot-reload"
            implementationClass = "SaHotReloadPlugin"
        }
        create("saDgsCodegen") {
            id = "sa.dgs-codegen"
            implementationClass = "SaDgsCodegenPlugin"
        }
    }
}

dependencies {
    // The generator strategy is loaded from buildSrc and needs its jOOQ superclass on that classpath.
    implementation(libs.jooq.codegen)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.snakeYaml)
    implementation(libs.dsgCodegen.gradle)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
}
