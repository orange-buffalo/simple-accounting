import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.testing.Test
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

fun Test.configureTestTask(mockitoAgent: Configuration, additionalJvmArgs: List<String> = emptyList()) {
    jvmArgs(
        "-javaagent:${mockitoAgent.asPath}",
        *additionalJvmArgs.toTypedArray(),
    )
    maxHeapSize = "1g"
    
    // Set timeout to prevent indefinite hangs in CI
    // Normal test execution takes ~3-4 minutes, so 20 minutes is a safe upper bound
    timeout.set(java.time.Duration.ofMinutes(20))
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
