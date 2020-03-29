repositories {
    jcenter()
}

plugins {
    `kotlin-dsl`
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

val jooqVersion = "3.13.1"

dependencies {
    implementation("org.jooq:jooq-meta-extensions:$jooqVersion")
    implementation("org.jooq:jooq-codegen:$jooqVersion")
}
