val prodConfigs = arrayOf(
    ".eslintignore", ".eslintrc.cjs", "package.json",
    "bun.lockb", "build-config/vite-plugins.ts", "vite.config.ts",
)
val storybookConfigs = arrayOf(".babelrc", "build-config/storybook/**")

val installFrontendDependencies by tasks.register<SaFrontendTask>("installFrontendDependencies") {
    args.set("install --frozen-lockfile")
}

val buildFrontend by tasks.register<SaCacheableFrontendTask>("buildFrontend") {
    args.set("run build")
    inputFiles {
        prodConfigs.forEach { include(it) }
        readTsConfig(file("tsconfig.app.json")).applyIncludesExcludes(this)
        include("public/**")
        include("index.html")
    }
    outputDirectories.set(files("dist"))
    dependsOn(installFrontendDependencies)
}

val testFrontend by tasks.register<SaCacheableFrontendTask>("testFrontend") {
    args.set("run test:unit")
    inputFiles {
        prodConfigs.forEach { include(it) }
        readTsConfig(file("tsconfig.vitest.json")).applyIncludesExcludes(this)
    }
    dependsOn(installFrontendDependencies)
}

val lint by tasks.register<SaCacheableFrontendTask>("lint") {
    args.set("run lint")
    inputFiles {
        prodConfigs.forEach { include(it) }
        storybookConfigs.forEach { include(it) }
        readTsConfig(file("tsconfig.storybook-config.json")).applyIncludesExcludes(this)
    }
    dependsOn(installFrontendDependencies)
}

val buildStorybook by tasks.register<SaCacheableFrontendTask>("buildStorybook") {
    args.set("run build-storybook")
    inputFiles {
        prodConfigs.forEach { include(it) }
        storybookConfigs.forEach { include(it) }
        readTsConfig(file("tsconfig.storybook-config.json")).applyIncludesExcludes(this)
    }
    outputDirectories.set(files("build/storybook"))
    dependsOn(installFrontendDependencies)
}

tasks.register("check") {
    dependsOn(testFrontend)
    dependsOn(lint)
}

val cleanFrontend by tasks.register("cleanFrontend") {
    group = "Frontend"
    doLast {
        delete(project.files("src/services/i18n/l10n"))
        delete(project.files("dist"))
        delete(project.files("build/storybook"))
    }
}

tasks.register("clean") {
    dependsOn(cleanFrontend)
}
