val prodConfigs = arrayOf(
    ".eslintignore", ".eslintrc.cjs", "package.json",
    "bun.lock", "build-config/vite-plugins.ts", "vite.config.ts",
)

val installFrontendDependencies by tasks.register<SaFrontendTask>("installFrontendDependencies") {
    args.set("install --frozen-lockfile")
}

val generateGqlTypes by tasks.register<SaCacheableFrontendTask>("generateGqlTypes") {
    args.set("run graphql-codegen")
    inputFiles {
        include("codegen.ts")
        include("package.json")
        include("src/**/*.vue")
        include("src/**/*.ts")
        exclude("src/services/api/gql/**")
    }
    inputs.file(rootProject.file("app/src/test/resources/api-schema.graphqls"))
    outputDirectories.set(files("src/services/api/gql"))
    dependsOn(installFrontendDependencies)
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
    dependsOn(generateGqlTypes)
}

val testFrontend by tasks.register<SaCacheableFrontendTask>("testFrontend") {
    args.set("run test:unit")
    inputFiles {
        prodConfigs.forEach { include(it) }
        readTsConfig(file("tsconfig.vitest.json")).applyIncludesExcludes(this)
    }
    dependsOn(generateGqlTypes)
}

val lint by tasks.register<SaCacheableFrontendTask>("lint") {
    args.set("run lint")
    inputFiles {
        prodConfigs.forEach { include(it) }
        readTsConfig(file("tsconfig.vitest.json")).applyIncludesExcludes(this)
    }
    dependsOn(generateGqlTypes)
}

tasks.register("check") {
    dependsOn(testFrontend)
    dependsOn(lint)
}

val cleanFrontend by tasks.register("cleanFrontend") {
    group = "Frontend"
    doLast {
        delete(files("src/services/i18n/l10n"))
        delete(files("src/services/api/gql"))
        delete(files("dist"))
    }
}

tasks.register("clean") {
    dependsOn(cleanFrontend)
}
