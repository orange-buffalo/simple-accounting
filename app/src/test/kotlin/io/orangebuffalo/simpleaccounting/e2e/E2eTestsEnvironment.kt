package io.orangebuffalo.simpleaccounting.e2e

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import io.orangebuffalo.testcontainers.playwright.PlaywrightContainer
import io.orangebuffalo.testcontainers.playwright.junit.PlaywrightConfigurer
import mu.KotlinLogging
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait

private val browserContainerLogger = KotlinLogging.logger("browser")
private val simpleAccountingLogger = KotlinLogging.logger("simple-accounting")

private val network: Network = Network.newNetwork()

private val simpleAccounting: SimpleAccountingApp = SimpleAccountingApp()
    .withNetwork(network)
    .withNetworkAliases("simple-accounting")
    .withEnv("spring_profiles_active", "ci-tests")
    .withLogConsumer { frame -> simpleAccountingLogger.info(frame.utf8String) }
    .waitingFor(Wait.forLogMessage(".*Started SimpleAccountingApplicationKt.*", 1))

class E2eTestsEnvironment : BeforeEachCallback, Extension {

    override fun beforeEach(context: ExtensionContext) {
        // lazy start the container for easier debugging in IDE
        // but keep singleton containers as they start slowly
        if (!simpleAccounting.isRunning) {
            simpleAccounting.start()
        }
    }
}

private class SimpleAccountingApp
    : GenericContainer<SimpleAccountingApp>("orangebuffalo/simple-accounting:latest")

class SaPlaywrightConfigurer : PlaywrightConfigurer {

    override fun setupContainer(container: PlaywrightContainer) {
        container
            .withNetwork(network)
            .withLogConsumer { frame -> browserContainerLogger.info(frame.utf8String) }
    }

    override fun createBrowserContextOptions(): Browser.NewContextOptions? {
        return Browser.NewContextOptions()
            .setBaseURL("http://simple-accounting:9393/")
            .setViewportSize(1920, 1080)
    }

    override fun setupBrowserContext(context: BrowserContext) {
        context.setDefaultTimeout(10_000.0)
    }
}
