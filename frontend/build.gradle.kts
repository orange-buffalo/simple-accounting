val prodConfigs = arrayOf(
    ".eslintignore", ".eslintrc.cjs", "package.json",
    "package-lock.json", "build-config/vite-plugins.ts", "vite.config.ts"
)
val storybookConfigs = arrayOf(".babelrc", "build-config/storybook/**")

val npmInstall by tasks.register<SaCacheableNpmTask>("npmInstall") {
    args.set("ci --no-bin-links")
    inputFiles {
        include("package-lock.json")
        include("build-config/npm-post-install/**")
    }
    outputDirectories.set(files("node_modules", "src/services/i18n/l10n"))
}

val npmBuild by tasks.register<SaCacheableNpmTask>("npmBuild") {
    args.set("run-script build")
    inputFiles {
        prodConfigs.forEach { include(it) }
        readTsConfig(file("tsconfig.app.json")).applyIncludesExcludes(this)
        include("public/**")
        include("index.html")
    }
    outputDirectories.set(files("dist"))
    dependsOn(npmInstall)
}

val npmTest by tasks.register<SaCacheableNpmTask>("npmTest") {
    args.set("run-script test:unit")
    inputFiles {
        prodConfigs.forEach { include(it) }
        readTsConfig(file("tsconfig.vitest.json")).applyIncludesExcludes(this)
    }
    dependsOn(npmInstall)
}

val npmLint by tasks.register<SaCacheableNpmTask>("npmLint") {
    args.set("run-script lint")
    inputFiles {
        prodConfigs.forEach { include(it) }
        storybookConfigs.forEach { include(it) }
        readTsConfig(file("tsconfig.storybook-config.json")).applyIncludesExcludes(this)
    }
    dependsOn(npmInstall)
}

val npmBuildStorybook by tasks.register<SaCacheableNpmTask>("npmBuildStorybook") {
    args.set("run-script build-storybook")
    inputFiles {
        prodConfigs.forEach { include(it) }
        storybookConfigs.forEach { include(it) }
        readTsConfig(file("tsconfig.storybook-config.json")).applyIncludesExcludes(this)
    }
    outputDirectories.set(files("build/storybook"))
    dependsOn(npmInstall)
}


tasks.register("check") {
    dependsOn(npmTest)
    dependsOn(npmLint)
}

val npmClean by tasks.register("npmClean") {
    group = "Frontend"
    doLast {
        delete(project.files("node_modules"))
        delete(project.files("src/services/i18n/l10n"))
        delete(project.files("dist"))
        delete(project.files("build/storybook"))
    }
}

tasks.register("clean") {
    dependsOn(npmClean)
}

val frontendBuild by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add("frontendBuild", file("dist")) {
        builtBy(npmBuild)
    }
}

