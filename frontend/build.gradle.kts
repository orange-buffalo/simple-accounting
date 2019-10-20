apply {
    plugin(FrontendPlugin::class.java)
}

tasks.register("build") {
    dependsOn("npmBuild")
}