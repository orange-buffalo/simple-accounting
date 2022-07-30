package io.orangebuffalo.simpleaccounting

import com.codeborne.selenide.Configuration
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.file.shouldExist
import io.orangebuffalo.simpleaccounting.utils.KBrowserWebDriverContainer
import io.orangebuffalo.simpleaccounting.utils.KNginxContainer
import io.orangebuffalo.simpleaccounting.utils.ReusableNetwork
import io.orangebuffalo.simpleaccounting.utils.logger
import mu.KotlinLogging
import org.junit.jupiter.api.extension.*
import org.openqa.selenium.chrome.ChromeOptions
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import io.orangebuffalo.simpleaccounting.utils.yamlObjectMapper
import java.io.File

private val testConfig = loadTestConfig()

private val chromeLogger = KotlinLogging.logger("chrome")

private val network: Network = if (testConfig.reuseContainers)
    ReusableNetwork("simple-accounting-storybook-tests")
else Network.newNetwork()

private val storybookEnvironment = StorybookEnvironment()

private const val storybookDirectory = "../frontend/build/storybook"

private val nginx = KNginxContainer("nginx:1.22")
    .withCustomContent(storybookDirectory)
    .waitingFor(HttpWaitStrategy())
    .withNetwork(network)
    .withReuse(testConfig.reuseContainers)
    .withNetworkAliases("storybook")

private val chrome: KBrowserWebDriverContainer = KBrowserWebDriverContainer()
    .withCapabilities(ChromeOptions().apply {
        addArguments("start-maximized")
    })
    .withNetwork(network)
    .withLogConsumer { frame -> chromeLogger.info(frame.utf8String) }
    .withEnv("SCREEN_WIDTH", "1200")
    .withEnv("SCREEN_HEIGHT", "500")
    .withEnv("SE_NODE_OVERRIDE_MAX_SESSIONS", "true")
    .withEnv("SE_NODE_MAX_SESSIONS", Runtime.getRuntime().availableProcessors().toString())
    .withNetwork(network)
    .withExtraHost("host.docker.internal", "host-gateway")
    .withReuse(testConfig.reuseContainers)
    .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.SKIP, null)

class StorybookExtension : BeforeAllCallback, BeforeEachCallback, Extension, ParameterResolver {
    override fun beforeAll(context: ExtensionContext) {
        Configuration.baseUrl = if (testConfig.useCompliedStorybook)
            "http://storybook/"
        else "http://host.docker.internal:6006/"
        Configuration.timeout = 10_000
    }

    override fun beforeEach(context: ExtensionContext) {
        // lazy start the container for easier debugging in IDE
        // but keep singleton containers as they start slowly
        if (!chrome.isRunning) {
            chrome.start()
            if (testConfig.useCompliedStorybook) {
                nginx.start()
            }

            Configuration.remote = chrome.seleniumAddress.toString()
        }
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == StorybookEnvironment::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return storybookEnvironment
    }
}

class StorybookEnvironment {

    val stories: Collection<StorybookStory>
    val shouldUpdateCommittedScreenshots: Boolean
        get() = testConfig.replaceCommittedFiles

    init {
        val storiesFile = File(storybookDirectory, "stories.json")
        storiesFile.shouldExist()

        val data: StorybookStoriesFileData = jacksonObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(storiesFile)
        stories = data.stories.values
    }
}

data class StorybookStory(
    val title: String,
    val name: String,
    val id: String,
)

private data class StorybookStoriesFileData(
    val stories: Map<String, StorybookStory>
)

private data class TestConfig(
    val reuseContainers: Boolean = false,
    val replaceCommittedFiles: Boolean = false,
    val useCompliedStorybook: Boolean = true,
)

private fun loadTestConfig(): TestConfig {
    val testConfigFile = File("src/storybookTest/.test-config.yaml")
    val config: TestConfig = if (testConfigFile.exists())
        yamlObjectMapper().readValue(testConfigFile)
    else TestConfig()
    logger.info { "Loaded test config: $config" }
    return config
}
