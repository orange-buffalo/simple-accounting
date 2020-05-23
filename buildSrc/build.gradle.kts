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
    implementation("org.jooq:jooq-meta-extensions:$jooqVersion") {
        // causes conflicts with spring-boot plugin
        exclude(group = "org.springframework")
    }
    implementation("org.jooq:jooq-codegen:$jooqVersion")
}
