plugins {
    id("com.gradle.build-scan") version Versions.buildScanPlugin
    id("io.wusa.semver-git-plugin") version Versions.semverGitPlugin
}

val semVersion = semver.info.version
allprojects {
    version = semVersion
    group = "io.orangebuffalo.accounting"
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}