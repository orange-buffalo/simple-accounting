package io.orangebuffalo.simpleaccounting.tests.infra.environment

import com.microsoft.playwright.BrowserContext
import io.orangebuffalo.testcontainers.playwright.PlaywrightContainer
import io.orangebuffalo.testcontainers.playwright.junit.PlaywrightConfigurer
import mu.KotlinLogging
import org.testcontainers.containers.Network

private val testNetwork: Network = Network.newNetwork()

private val browserContainerLogger = KotlinLogging.logger("browser")

private val testConfig = TestConfig.load()

private val testBrowser: PlaywrightContainer = PlaywrightContainer()
    .withLogConsumer { frame -> browserContainerLogger.info(frame.utf8String) }
    .withNetwork(testNetwork)
    .withExtraHost("host.docker.internal", testConfig.hostIpInDockerContainer)

class TestEnvironment {

    companion object {
        val network = testNetwork
        val browser = testBrowser
        val config = testConfig
    }
}

abstract class SaPlaywrightConfigurer : PlaywrightConfigurer {

    override fun createContainer(): PlaywrightContainer = testBrowser

    override fun setupBrowserContext(context: BrowserContext) {
        context.setDefaultTimeout(10_000.0)
    }
}
