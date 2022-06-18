package io.orangebuffalo.simpleaccounting

import com.codeborne.selenide.Configuration
import com.codeborne.selenide.WebDriverRunner
import mu.KotlinLogging
import org.junit.jupiter.api.extension.*
import org.openqa.selenium.chrome.ChromeOptions
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait

private val chromeLogger = KotlinLogging.logger("chrome")
private val simpleAccountingLogger = KotlinLogging.logger("simple-accounting")

private val network: Network = Network.newNetwork()

private val chrome: KBrowserWebDriverContainer = KBrowserWebDriverContainer()
    .withCapabilities(ChromeOptions())
    .withNetwork(network)
    .withLogConsumer { frame -> chromeLogger.info(frame.utf8String) }
    .withEnv("SCREEN_WIDTH", "1920")
    .withNetwork(network)

private val simpleAccounting: SimpleAccountingApp = SimpleAccountingApp()
    .withNetwork(network)
    .withNetworkAliases("simple-accounting")
    .withEnv("spring_profiles_active", "ci-tests")
    .withLogConsumer { frame -> simpleAccountingLogger.info(frame.utf8String) }
    .waitingFor(Wait.forHealthcheck())

class E2eTestsEnvironment : BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback, Extension {
    override fun beforeAll(context: ExtensionContext) {
        Configuration.baseUrl = "http://simple-accounting:9393"
        Configuration.timeout = 10_000
    }

    override fun beforeEach(context: ExtensionContext) {
        // lazy start the container for easier debugging in IDE
        // but keep singleton containers as they start slowly
        if (!simpleAccounting.isRunning) {
            simpleAccounting.start()

            chrome.start()

            Configuration.remote = chrome.seleniumAddress.toString()
        }
    }

    override fun afterEach(context: ExtensionContext) {
        WebDriverRunner.closeWindow()
    }

    override fun afterAll(context: ExtensionContext) {
        WebDriverRunner.closeWebDriver()
    }
}

private class SimpleAccountingApp
    : GenericContainer<SimpleAccountingApp>("orangebuffalo/simple-accounting:latest")

private class KBrowserWebDriverContainer : BrowserWebDriverContainer<KBrowserWebDriverContainer>()
