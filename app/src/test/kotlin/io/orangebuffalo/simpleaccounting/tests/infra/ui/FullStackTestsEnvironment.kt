package io.orangebuffalo.simpleaccounting.tests.infra.ui

import com.microsoft.playwright.*
import com.microsoft.playwright.impl.AssertionsTimeout
import io.orangebuffalo.simpleaccounting.tests.infra.environment.TestConfig
import io.orangebuffalo.simpleaccounting.tests.infra.utils.UI_ASSERTIONS_TIMEOUT_MS
import org.junit.jupiter.api.extension.*
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.support.TestPropertySourceUtils
import org.springframework.test.util.TestSocketUtils
import java.nio.file.Path

private val log = mu.KotlinLogging.logger {}

private val springContextPort: Int by lazy {
    if (TestConfig.instance.fullStackTests.useViteDevServer)
        TestConfig.instance.fullStackTests.viteDevServerSpringContextPort
    else TestSocketUtils.findAvailableTcpPort()
}

private const val viteDevServerPort: Int = 5173

private val targetPort = if (TestConfig.instance.fullStackTests.useViteDevServer)
    viteDevServerPort
else springContextPort
private val browserUrl = "http://localhost:$targetPort"

fun getBrowserUrl(): String = browserUrl

class FullStackTestsSpringContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            applicationContext,
            "server.port=$springContextPort",
            "simpleaccounting.public-url=$browserUrl",
        )
    }
}

class SaPlaywrightExtension : Extension, BeforeEachCallback, AfterEachCallback, ParameterResolver {
    override fun beforeEach(extensionContext: ExtensionContext) {
        var playwrightContext = threadLocalPlaywrightContext.get()
        if (playwrightContext == null) {
            val playwright = Playwright.create()
            log.info { "Playwright instance created" }
            Runtime.getRuntime().addShutdownHook(Thread {
                playwright.close()
                log.info { "Playwright instance closed" }
            })
            val browser = playwright.chromium().launch(
                BrowserType.LaunchOptions()
                    .setHeadless(TestConfig.instance.fullStackTests.useHeadlessBrowser)
                    .setSlowMo(TestConfig.instance.fullStackTests.slowMoMs.toDouble())
            )
            playwrightContext = PlaywrightContext(playwright, browser)
            threadLocalPlaywrightContext.set(playwrightContext)
            // setup assertions timeout
            AssertionsTimeout.setDefaultTimeout(UI_ASSERTIONS_TIMEOUT_MS.toDouble())
        }

        val browserContext = playwrightContext.browser.newContext(
             Browser.NewContextOptions()
                .setBaseURL(browserUrl)
                .setViewportSize(1920, 1080)
        )
        browserContext.tracing().start(
            Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
        )
        browserContext.setDefaultTimeout(UI_ASSERTIONS_TIMEOUT_MS.toDouble())
        browserContext.addInitScript("""
            window.saRunningInTest = true;
            console.info("Playwright test environment initialized");
        """.trimIndent())
        playwrightContext.browserContext = browserContext

        playwrightContext.page = browserContext.newPage()
    }

    override fun afterEach(extensionContext: ExtensionContext) {
        val playwrightContext = threadLocalPlaywrightContext.get()
            ?: throw IllegalStateException("Playwright context is not initialized for the current thread")
        if (extensionContext.executionException.isPresent) {
            val browserContext = playwrightContext.browserContext
                ?: throw IllegalStateException("Browser context is not initialized in the Playwright context")
            browserContext.tracing()
                .stop(
                    Tracing.StopOptions()
                        .setPath(
                            Path.of(
                                "build", "playwright-traces",
                                "${extensionContext.requiredTestClass.simpleName}-${extensionContext.requiredTestMethod.name}.zip"
                            )
                        )
                )
        }
        playwrightContext.page?.close()
        playwrightContext.page = null
        playwrightContext.browserContext?.close()
        playwrightContext.browserContext = null
    }

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Boolean = parameterContext.parameter.type == Page::class.java

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any {
        val playwrightContext = threadLocalPlaywrightContext.get()
            ?: throw IllegalStateException("Playwright context is not initialized for the current thread")

        return when (parameterContext.parameter.type) {
            Page::class.java -> {
                playwrightContext.page
                    ?: throw IllegalStateException("Page is not initialized in the Playwright context")
            }

            else -> throw IllegalArgumentException("Unsupported parameter type: ${parameterContext.parameter.type}")
        }
    }
}

private val threadLocalPlaywrightContext = ThreadLocal<PlaywrightContext?>()

private data class PlaywrightContext(
    val playwright: Playwright,
    val browser: Browser,
    var browserContext: BrowserContext? = null,
    var page: Page? = null,
)
