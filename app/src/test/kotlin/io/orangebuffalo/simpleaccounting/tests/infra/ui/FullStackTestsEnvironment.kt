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
            
            if (TestConfig.instance.fullStackTests.usePersistentContext) {
                // Use persistent context for local development
                val userDataDir = Path.of("local-dev", "playwright-context")
                log.info { "Using persistent context at $userDataDir" }
                val persistentContext = playwright.chromium().launchPersistentContext(
                    userDataDir,
                    BrowserType.LaunchPersistentContextOptions()
                        .setHeadless(TestConfig.instance.fullStackTests.useHeadlessBrowser)
                        .setSlowMo(TestConfig.instance.fullStackTests.slowMoMs.toDouble())
                        .setBaseURL(browserUrl)
                        .setViewportSize(1920, 1080)
                )
                persistentContext.setDefaultTimeout(UI_ASSERTIONS_TIMEOUT_MS.toDouble())
                persistentContext.addInitScript("""
                    window.saRunningInTest = true;
                    console.info("Playwright test environment initialized");
                """.trimIndent())
                playwrightContext = PlaywrightContext(playwright, null, persistentContext)
                threadLocalPlaywrightContext.set(playwrightContext)
                // setup assertions timeout
                AssertionsTimeout.setDefaultTimeout(UI_ASSERTIONS_TIMEOUT_MS.toDouble())
                
                playwrightContext.page = persistentContext.newPage()
            } else {
                // Use ephemeral browser for CI or when not using persistent context
                val browser = playwright.chromium().launch(
                    BrowserType.LaunchOptions()
                        .setHeadless(TestConfig.instance.fullStackTests.useHeadlessBrowser)
                        .setSlowMo(TestConfig.instance.fullStackTests.slowMoMs.toDouble())
                )
                playwrightContext = PlaywrightContext(playwright, browser)
                threadLocalPlaywrightContext.set(playwrightContext)
                // setup assertions timeout
                AssertionsTimeout.setDefaultTimeout(UI_ASSERTIONS_TIMEOUT_MS.toDouble())
                
                val browserContext = browser.newContext(
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
        } else {
            // For persistent context, just create a new page and clear state
            if (TestConfig.instance.fullStackTests.usePersistentContext) {
                val persistentContext = playwrightContext.browserContext
                    ?: throw IllegalStateException("Persistent context is not initialized")
                
                // Clear storage to ensure test isolation
                persistentContext.clearCookies()
                // Close all existing pages to ensure clean state
                persistentContext.pages().forEach { it.close() }
                
                val newPage = persistentContext.newPage()
                // Navigate to the base URL and clear storage
                newPage.navigate("$browserUrl/")
                newPage.evaluate("() => { localStorage.clear(); sessionStorage.clear(); }")
                playwrightContext.page = newPage
            } else {
                // For non-persistent mode, create a new context and page as before
                val browserContext = playwrightContext.browser!!.newContext(
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
        }
    }

    override fun afterEach(extensionContext: ExtensionContext) {
        val playwrightContext = threadLocalPlaywrightContext.get()
            ?: throw IllegalStateException("Playwright context is not initialized for the current thread")
        
        if (!TestConfig.instance.fullStackTests.usePersistentContext) {
            // Only save traces for non-persistent context
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
        } else {
            // For persistent context, just close the page
            playwrightContext.page?.close()
            playwrightContext.page = null
        }
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
    val browser: Browser?,
    var browserContext: BrowserContext? = null,
    var page: Page? = null,
)
