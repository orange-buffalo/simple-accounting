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
import kotlin.io.path.absolute

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
            playwrightContext = PlaywrightContext(
                playwright,
                if (TestConfig.instance.fullStackTests.usePersistentContext) {
                    PersistentPageContextStrategy(playwright)
                } else {
                    IsolatedPageContextStrategy(playwright)
                },
            )
            threadLocalPlaywrightContext.set(playwrightContext)
            // setup assertions timeout
            AssertionsTimeout.setDefaultTimeout(UI_ASSERTIONS_TIMEOUT_MS.toDouble())
        }
        playwrightContext.pageContextStrategy.beforeTest()
        val browserContext: BrowserContext = playwrightContext.pageContextStrategy.getBrowserContext()
        browserContext.tracing().start(
            Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
        )
    }

    override fun afterEach(extensionContext: ExtensionContext) {
        val playwrightContext = threadLocalPlaywrightContext.get()
            ?: throw IllegalStateException("Playwright context is not initialized for the current thread")
        if (extensionContext.executionException.isPresent) {
            val browserContext = playwrightContext.pageContextStrategy.getBrowserContext()
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
        } else {
            // if the test has passed, do not keep the trace to save disk space
            playwrightContext.pageContextStrategy.getBrowserContext().tracing().stop()
        }
        playwrightContext.pageContextStrategy.afterTest()
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
                playwrightContext.pageContextStrategy.getPageForTheTest()
            }

            else -> throw IllegalArgumentException("Unsupported parameter type: ${parameterContext.parameter.type}")
        }
    }
}

private val threadLocalPlaywrightContext = ThreadLocal<PlaywrightContext?>()

private interface PageContextStrategy {
    fun getPageForTheTest(): Page
    fun getBrowserContext(): BrowserContext
    fun beforeTest()
    fun afterTest()
}

private data class PlaywrightContext(
    val playwright: Playwright,
    val pageContextStrategy: PageContextStrategy,
)

private class IsolatedPageContextStrategy(
    private val playwright: Playwright,
) : PageContextStrategy {
    private var browser: Browser? = null
    private var browserContext: BrowserContext? = null
    private var page: Page? = null

    override fun getPageForTheTest(): Page {
        if (page == null) {
            page = browserContext!!.newPage()
        }
        return page!!
    }

    override fun getBrowserContext(): BrowserContext {
        return browserContext ?: throw IllegalStateException("beforeTest was not called")
    }

    override fun beforeTest() {
        if (browser == null) {
            browser = playwright.chromium().launch(
                BrowserType.LaunchOptions()
                    .setHeadless(TestConfig.instance.fullStackTests.useHeadlessBrowser)
                    .setSlowMo(TestConfig.instance.fullStackTests.slowMoMs.toDouble())
            )
        }
        browserContext = browser!!.newContext(
            Browser.NewContextOptions()
                .setBaseURL(browserUrl)
                .setViewportSize(1920, 1080)
        )
        configureNewBrowserContext(browserContext!!)
    }

    override fun afterTest() {
        page?.close()
        page = null
        browserContext?.close()
        browserContext = null
    }
}

private class PersistentPageContextStrategy(
    private val playwright: Playwright,
) : PageContextStrategy {
    private var browserContext: BrowserContext? = null
    private var page: Page? = null

    override fun getPageForTheTest(): Page {
        return page ?: throw IllegalStateException("beforeTest was not called")
    }

    override fun getBrowserContext(): BrowserContext {
        return browserContext ?: throw IllegalStateException("beforeTest was not called")
    }

    override fun beforeTest() {
        if (page == null) {
            // for persistent context, we do not close the page for better developer experience
            val userDataDir = Path.of("..", "local-dev", "playwright-context")
            log.info { "Using persistent context at ${userDataDir.absolute()}" }
            browserContext = playwright.chromium().launchPersistentContext(
                userDataDir,
                BrowserType.LaunchPersistentContextOptions()
                    // makes no sense to use headless mode with persistent context
                    .setHeadless(false)
                    .setSlowMo(TestConfig.instance.fullStackTests.slowMoMs.toDouble())
                    .setBaseURL(browserUrl)
                    .setViewportSize(1920, 1080)
                    // auto open devtools for better developer experience
                    .setArgs(listOf("--auto-open-devtools-for-tabs"))
            )
            configureNewBrowserContext(browserContext!!)
            page = browserContext!!.newPage()
        }
    }

    override fun afterTest() {
        // do not close the page for better developer experience
        // clear cookies and local storage to have a clean state for the next test
        page!!.evaluate(
            """
                    () => {
                        localStorage.clear();
                        sessionStorage.clear();
                    }
                    """
        )
        browserContext!!.clearCookies()
    }
}

private fun configureNewBrowserContext(browserContext: BrowserContext) {
    browserContext.setDefaultTimeout(UI_ASSERTIONS_TIMEOUT_MS.toDouble())
    browserContext.addInitScript(
        """
            window.saRunningInTest = true;
            console.info("Playwright test environment initialized");
        """.trimIndent()
    )
}
