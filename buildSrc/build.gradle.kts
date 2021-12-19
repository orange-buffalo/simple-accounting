object Versions {
    const val jooq = "3.15.5"
}

repositories {
    mavenCentral()
}

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.jooq:jooq-meta-extensions:${Versions.jooq}") {
        // causes conflicts with spring-boot plugin
        exclude(group = "org.springframework")
    }
    implementation("org.jooq:jooq-codegen:${Versions.jooq}")
}
