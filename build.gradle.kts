plugins {
    id("com.gradle.build-scan") version Versions.buildScanPlugin
}

subprojects {
    group = "io.orangebuffalo.accounting"
    version = "0.0.1-SNAPSHOT"
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}