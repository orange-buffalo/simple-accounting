package io.orangebuffalo.simpleaccounting.e2e

import com.microsoft.playwright.Browser
import io.orangebuffalo.simpleaccounting.tests.infra.environment.SaPlaywrightConfigurer
import io.orangebuffalo.simpleaccounting.tests.infra.environment.TestEnvironment
import mu.KotlinLogging
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

private val simpleAccountingLogger = KotlinLogging.logger("simple-accounting")
private class SimpleAccountingApp
    : GenericContainer<SimpleAccountingApp>("orangebuffalo/simple-accounting:latest")
private val simpleAccounting: SimpleAccountingApp = SimpleAccountingApp()
    .withNetwork(TestEnvironment.network)
    .withNetworkAliases("simple-accounting")
    .withEnv("spring_profiles_active", "ci-tests")
    .withLogConsumer { frame -> simpleAccountingLogger.info(frame.utf8String) }
    .waitingFor(Wait.forLogMessage(".*Started SimpleAccountingApplicationKt.*", 1))

class E2eTestsExtension : BeforeEachCallback, Extension {

    override fun beforeEach(context: ExtensionContext) {
        // lazy start the container for easier debugging in IDE
        // but keep singleton containers as they start slowly
        if (!simpleAccounting.isRunning) {
            simpleAccounting.start()
        }
    }
}

class E2eTestsPlaywrightConfigurer : SaPlaywrightConfigurer() {

    override fun createBrowserContextOptions(): Browser.NewContextOptions? {
        return Browser.NewContextOptions()
            .setBaseURL("http://simple-accounting:9393/")
            .setViewportSize(1920, 1080)
    }
}
