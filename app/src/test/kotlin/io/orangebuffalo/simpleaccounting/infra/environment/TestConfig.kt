package io.orangebuffalo.simpleaccounting.infra.environment

import com.fasterxml.jackson.module.kotlin.readValue
import io.orangebuffalo.simpleaccounting.infra.utils.logger
import io.orangebuffalo.simpleaccounting.infra.utils.yamlObjectMapper
import java.io.File

data class TestConfig(
    val screenshots: ScreenshotsConfig = ScreenshotsConfig(),
    val fullStackTestsConfig: FullStackTestsConfig = FullStackTestsConfig(),
    val hostIpInDockerContainer: String = "172.17.0.1",
) {
    companion object {
        fun load(): TestConfig = loadTestConfig()
    }
}

data class ScreenshotsConfig(
    val replaceCommittedFiles: Boolean = false,
    val useCompliedStorybook: Boolean = true,
)

data class FullStackTestsConfig(
    val useLocalBrowser: Boolean = false,
    val useViteDevServer: Boolean = false,
    val viteDevServerSpringContextPort: Int = 5174,
)

private fun loadTestConfig(): TestConfig {
    val testConfigFile = File("src/test/.test-config.yaml")
    val config: TestConfig = if (testConfigFile.exists())
        yamlObjectMapper().readValue(testConfigFile)
    else TestConfig()
    logger.info { "Loaded test config: $config" }
    return config
}
