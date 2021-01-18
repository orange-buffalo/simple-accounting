plugins {
    id("io.wusa.semver-git-plugin") version Versions.semverGitPlugin
    id("com.github.ben-manes.versions") version Versions.versionsPlugin
}

semver {
    branches {
        branch {
            regex = ".+"
            incrementer = "MINOR_INCREMENTER"
            formatter = Transformer<Any, io.wusa.Info> { info ->
                "${info.version.major}.${info.version.minor}.${info.version.patch}"
            }
        }
    }
}

val semVersion = semver.info
allprojects {
    version = semVersion
    group = "io.orangebuffalo.simpleaccounting"
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}
