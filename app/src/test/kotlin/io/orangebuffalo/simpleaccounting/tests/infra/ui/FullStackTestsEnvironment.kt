package io.orangebuffalo.simpleaccounting.tests.infra.ui

import com.microsoft.playwright.*
import com.microsoft.playwright.impl.AssertionsTimeout
import io.orangebuffalo.simpleaccounting.tests.infra.environment.TestConfig
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Notifications
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
        
        // Capture network requests
        browserContext.onRequest { request ->
            val postData = try { request.postData() } catch (e: Exception) { null }
            log.debug { "Network request: ${request.method()} ${request.url()}" }
            if (postData != null) {
                log.debug { "Request body: $postData" }
            }
        }
        browserContext.onResponse { response ->
            log.debug { "Network response: ${response.status()} ${response.url()}" }
            try {
                val body = response.text()
                if (body.isNotEmpty() && body.length < 10000) { // Limit body size in logs
                    log.debug { "Response body: $body" }
                }
            } catch (e: Exception) {
                log.debug { "Could not read response body: ${e.message}" }
            }
        }
        
        // Capture browser console
        val page = playwrightContext.pageContextStrategy.getPageForTheTest()
        page.onConsoleMessage { msg ->
            log.debug { "Browser console [${msg.type()}]: ${msg.text()}" }
        }
        
        browserContext.tracing().start(
            Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
        )
    }

    override fun afterEach(extensionContext: ExtensionContext) {
        val playwrightContext = threadLocalPlaywrightContext.get()
            ?: throw IllegalStateException("Playwright context is not initialized for the current thread")
        
        // Verify no notifications are visible after each test
        if (extensionContext.executionException.isEmpty) {
            val page = playwrightContext.pageContextStrategy.getPageForTheTest()
            Notifications(page).shouldHaveNoNotifications()
        }
        
        if (extensionContext.executionException.isPresent) {
            val browserContext = playwrightContext.pageContextStrategy.getBrowserContext()
            val tracesDir = Path.of("build", "playwright-traces")
            tracesDir.toFile().mkdirs()  // Ensure directory exists
            
            val tracePath = tracesDir.resolve(
                "${extensionContext.requiredTestClass.simpleName}-${extensionContext.requiredTestMethod.name}.zip"
            )
            browserContext.tracing()
                .stop(
                    Tracing.StopOptions()
                        .setPath(tracePath)
                )
            log.info { "Playwright trace saved to: ${tracePath.toAbsolutePath()}" }
            
            // Take screenshot on test failure
            val page = playwrightContext.pageContextStrategy.getPageForTheTest()
            val screenshotPath = tracesDir.resolve(
                "${extensionContext.requiredTestClass.simpleName}-${extensionContext.requiredTestMethod.name}.png"
            )
            page.screenshot(Page.ScreenshotOptions().setPath(screenshotPath))
            log.info { "Screenshot saved to: ${screenshotPath.toAbsolutePath()}" }
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
            // Set fixed time for the browser to avoid date-dependent test failures
            page!!.clock().install(Clock.InstallOptions().setTime(TEST_FIXED_DATE_TIME.toEpochMilli()))
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
                    // ensure viewport is a per window setting
                    .setViewportSize(null)
                    // auto open devtools for better developer experience
                    .setArgs(listOf("--auto-open-devtools-for-tabs"))
            )
            configureNewBrowserContext(browserContext!!)
            page = browserContext!!.newPage()
            // Set fixed time for the browser to avoid date-dependent test failures
            page!!.clock().install(Clock.InstallOptions().setTime(TEST_FIXED_DATE_TIME.toEpochMilli()))
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

/**
 * The fixed date/time used in browser tests.
 * Ensures tests stability and reproducibility.
 */
val TEST_FIXED_DATE_TIME: java.time.Instant = java.time.Instant.parse("2019-08-15T10:00:00Z")

private fun configureNewBrowserContext(browserContext: BrowserContext) {
    browserContext.setDefaultTimeout(UI_ASSERTIONS_TIMEOUT_MS.toDouble())
    browserContext.addInitScript(
        """
            window.saRunningInTest = true;
            console.info("Playwright test environment initialized");
        """.trimIndent()
    )
}
