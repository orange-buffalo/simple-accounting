import org.gradle.api.Project
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult
import java.time.Instant

fun Project.printTestDescriptionDuringBuild(testDescriptor: TestDescriptor) {
    logger.lifecycle("${Instant.now()}  [Test started] [$testDescriptor]")
}

fun Project.printTestResultDuringBuild(testDescriptor: TestDescriptor, testResult: TestResult) {
    logger.lifecycle("${Instant.now()}  [Test finished] [$testDescriptor] " +
            "[time: ${testResult.endTime - testResult.startTime}ms] " +
            "[result: $testResult]")
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
