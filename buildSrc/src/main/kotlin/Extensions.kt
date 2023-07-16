import org.gradle.api.Project
import org.gradle.api.tasks.testing.TestDescriptor

fun Project.printTestDescriptionDuringBuild(testDescriptor: TestDescriptor) {
    logger.lifecycle("Running test: $testDescriptor")
}

fun ifCi(action: () -> Unit) {
    if (System.getenv("CI") == "true") {
        action()
    }
}
