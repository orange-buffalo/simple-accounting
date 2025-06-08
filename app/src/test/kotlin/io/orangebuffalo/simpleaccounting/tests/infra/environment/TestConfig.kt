package io.orangebuffalo.simpleaccounting.tests.infra.environment

import com.fasterxml.jackson.module.kotlin.readValue
import io.orangebuffalo.simpleaccounting.tests.infra.utils.logger
import io.orangebuffalo.simpleaccounting.tests.infra.utils.yamlObjectMapper
import java.io.File

data class TestConfig(
    val screenshots: ScreenshotsConfig = ScreenshotsConfig(),
    val fullStackTestsConfig: FullStackTestsConfig = FullStackTestsConfig(),
    val hostIpInDockerContainer: String = "172.17.0.1",
) {
    companion object {
        val instance: TestConfig by lazy { loadTestConfig() }
    }
}

data class ScreenshotsConfig(
    val useCompliedStorybook: Boolean = true,
)

data class FullStackTestsConfig(
    val useHeadlessBrowser: Boolean = false,
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
