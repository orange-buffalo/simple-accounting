apply<SaFrontendPlugin>()

tasks.register("clean") {
    dependsOn("npmClean")
}

tasks.register("check") {
    dependsOn("npmTest")
}
