apply<SaFrontendPlugin>()

tasks.register("assemble") {
    dependsOn("npmBuild")
}

tasks.register("clean") {
    dependsOn("npmClean")
}

tasks.register("check") {
    dependsOn("npmTest")
}
