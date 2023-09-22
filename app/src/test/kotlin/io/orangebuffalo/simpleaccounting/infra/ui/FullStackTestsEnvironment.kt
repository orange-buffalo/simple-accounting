package io.orangebuffalo.simpleaccounting.infra.ui

import com.microsoft.playwright.Browser
import io.orangebuffalo.simpleaccounting.infra.environment.SaPlaywrightConfigurer
import io.orangebuffalo.simpleaccounting.infra.environment.TestEnvironment
import io.orangebuffalo.simpleaccounting.infra.utils.logger
import io.orangebuffalo.testcontainers.playwright.junit.LocalPlaywrightApiProvider
import io.orangebuffalo.testcontainers.playwright.junit.PlaywrightApiProvider
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.support.TestPropertySourceUtils
import org.springframework.test.util.TestSocketUtils

private sealed interface BrowserStrategy {
    fun createPlaywrightApiProvider(): PlaywrightApiProvider?
    val appHostName: String
}

private object LocalBrowserStrategy : BrowserStrategy {
    override fun createPlaywrightApiProvider() = LocalPlaywrightApiProvider()

    override val appHostName: String
        get() = "localhost"
}

private object ContainerBrowserStrategy : BrowserStrategy {
    override fun createPlaywrightApiProvider() = null

    override val appHostName: String
        get() = "host.docker.internal"
}

private val browserStrategy: BrowserStrategy = if (TestEnvironment.config.fullStackTestsConfig.useLocalBrowser) {
    logger.info { "Using local browser for tests" }
    LocalBrowserStrategy
} else {
    logger.info { "Using container browser for tests" }
    ContainerBrowserStrategy
}

private val springContextPort: Int by lazy {
    if (TestEnvironment.config.fullStackTestsConfig.useViteDevServer)
        TestEnvironment.config.fullStackTestsConfig.viteDevServerSpringContextPort
    else TestSocketUtils.findAvailableTcpPort()
}

private const val viteDevServerPort: Int = 5173

class FullStackTestsSpringContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            applicationContext, "server.port=${springContextPort}"
        )
    }
}

class FullStackTestsPlaywrightConfigurer : SaPlaywrightConfigurer() {

    override fun createBrowserContextOptions(): Browser.NewContextOptions? {
        val targetPort = if (TestEnvironment.config.fullStackTestsConfig.useViteDevServer)
            viteDevServerPort
        else springContextPort

        @Suppress("HttpUrlsUsage")
        return Browser.NewContextOptions()
            .setBaseURL("http://${browserStrategy.appHostName}:$targetPort/")
            .setViewportSize(1920, 1080)
    }

    override fun createPlaywrightApiProvider(): PlaywrightApiProvider? = browserStrategy.createPlaywrightApiProvider()
}
