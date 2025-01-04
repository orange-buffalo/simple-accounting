import org.gradle.api.Project
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult

fun Project.printTestDescriptionDuringBuild(testDescriptor: TestDescriptor) {
    logger.lifecycle("Running test: $testDescriptor")
}

fun Project.printTestResultDuringBuild(testDescriptor: TestDescriptor, testResult: TestResult) {
    logger.lifecycle("Test $testDescriptor finished with result: $testResult")
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
