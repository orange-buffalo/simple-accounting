package io.orangebuffalo.simpleaccounting.infra.environment

import com.microsoft.playwright.BrowserContext
import io.orangebuffalo.testcontainers.playwright.PlaywrightContainer
import io.orangebuffalo.testcontainers.playwright.junit.PlaywrightConfigurer
import mu.KotlinLogging
import org.testcontainers.containers.Network

private val testsNetwork: Network = Network.newNetwork()

private val browserContainerLogger = KotlinLogging.logger("browser")

private val testBrowser: PlaywrightContainer = PlaywrightContainer()
    .withLogConsumer { frame -> browserContainerLogger.info(frame.utf8String) }
    .withNetwork(testsNetwork)
    .withExtraHost("host.docker.internal", "host-gateway")

class TestsEnvironment {

    companion object {
        val network = testsNetwork
        val browser = testBrowser
        val config = TestConfig.load()
    }
}

abstract class SaPlaywrightConfigurer : PlaywrightConfigurer {

    override fun createContainer(): PlaywrightContainer = testBrowser

    override fun setupBrowserContext(context: BrowserContext) {
        context.setDefaultTimeout(10_000.0)
    }
}
