package io.orangebuffalo.simpleaccounting.tests.infra.ui

import com.microsoft.playwright.Browser
import com.microsoft.playwright.junit.Options
import com.microsoft.playwright.junit.OptionsFactory
import io.orangebuffalo.simpleaccounting.tests.infra.environment.TestConfig
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.support.TestPropertySourceUtils
import org.springframework.test.util.TestSocketUtils
import java.nio.file.Path

private val springContextPort: Int by lazy {
    if (TestConfig.instance.fullStackTestsConfig.useViteDevServer)
        TestConfig.instance.fullStackTestsConfig.viteDevServerSpringContextPort
    else TestSocketUtils.findAvailableTcpPort()
}

private const val viteDevServerPort: Int = 5173

private val targetPort = if (TestConfig.instance.fullStackTestsConfig.useViteDevServer)
    viteDevServerPort
else springContextPort
private val browserUrl = "http://localhost:$targetPort"

class FullStackTestsSpringContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            applicationContext,
            "server.port=$springContextPort",
            "simpleaccounting.public-url=$browserUrl",
        )
    }
}

class FullStackTestsPlaywrightOptions : OptionsFactory {
    override fun getOptions(): Options = Options()
        .setContextOptions(
            Browser.NewContextOptions()
                .setBaseURL(browserUrl)
                .setViewportSize(1920, 1080)
        )
        .setHeadless(TestConfig.instance.fullStackTestsConfig.useHeadlessBrowser)
        .setTrace(Options.Trace.RETAIN_ON_FAILURE)
        .setOutputDir(Path.of("build", "playwright-traces"))
}
