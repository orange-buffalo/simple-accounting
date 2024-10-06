import org.gradle.api.Project
import org.gradle.api.tasks.testing.TestDescriptor

fun Project.printTestDescriptionDuringBuild(testDescriptor: TestDescriptor) {
    logger.lifecycle("Running test: $testDescriptor")
}

fun ifCi(action: () -> Unit) {
    if (isCi()) {
        action()
    }
}

fun ifLocal(action: () -> Unit) {
    if (!isCi()) {
        action()
    }
}

private fun isCi() = System.getenv("CI") == "true"
