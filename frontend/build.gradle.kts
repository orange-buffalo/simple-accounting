val prodConfigs = arrayOf(
    ".eslintignore", ".eslintrc.cjs", "package.json",
    "yarn.lock", "build-config/vite-plugins.ts", "vite.config.ts",
    ".yarnrc.yml", ".pnp.cjs", ".pnp.loader.mjs", ".yarn/releases/**",
)
val storybookConfigs = arrayOf(".babelrc", "build-config/storybook/**")

val installFrontendDependencies by tasks.register<SaFrontendTask>("installFrontendDependencies") {
    args.set("install --immutable")
}

val buildFrontend by tasks.register<SaCacheableFrontendTask>("buildFrontend") {
    args.set("build")
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
    args.set("test:unit")
    inputFiles {
        prodConfigs.forEach { include(it) }
        readTsConfig(file("tsconfig.vitest.json")).applyIncludesExcludes(this)
    }
    dependsOn(installFrontendDependencies)
}

val lint by tasks.register<SaCacheableFrontendTask>("lint") {
    args.set("lint")
    inputFiles {
        prodConfigs.forEach { include(it) }
        storybookConfigs.forEach { include(it) }
        readTsConfig(file("tsconfig.storybook-config.json")).applyIncludesExcludes(this)
    }
    dependsOn(installFrontendDependencies)
}

val buildStorybook by tasks.register<SaCacheableFrontendTask>("buildStorybook") {
    args.set("build-storybook")
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
        delete(project.files(".yarn/install-state.gz"))
        delete(project.files("src/services/i18n/l10n"))
        delete(project.files("dist"))
        delete(project.files("build/storybook"))
    }
}

tasks.register("clean") {
    dependsOn(cleanFrontend)
}
