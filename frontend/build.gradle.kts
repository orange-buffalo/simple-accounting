apply<SaFrontendPlugin>()

tasks.register("assemble") {
    dependsOn("npmBuild")
}
