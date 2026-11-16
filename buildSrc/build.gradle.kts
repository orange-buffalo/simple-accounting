repositories {
    mavenCentral()
}

plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version embeddedKotlinVersion
}

gradlePlugin {
    plugins {
        create("saJooqCodeGen") {
            id = "sa.jooq-codegen"
            implementationClass = "SaJooqCodeGenPlugin"
        }
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
    implementation(libs.jooq.metaExtensions) {
        // causes conflicts with spring-boot plugin
        exclude(group = "org.springframework")
    }
    implementation(libs.jooq.codegen)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.snakeYaml)
    implementation(libs.dsgCodegen.gradle)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
}
