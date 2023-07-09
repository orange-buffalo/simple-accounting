package io.orangebuffalo.simpleaccounting.infra.environment

import com.fasterxml.jackson.module.kotlin.readValue
import io.orangebuffalo.simpleaccounting.infra.utils.logger
import io.orangebuffalo.simpleaccounting.infra.utils.yamlObjectMapper
import java.io.File

data class TestConfig(
    val screenshots: ScreenshotsConfig = ScreenshotsConfig(),
) {
    companion object {
        fun load(): TestConfig = loadTestConfig()
    }
}

data class ScreenshotsConfig(
    val replaceCommittedFiles: Boolean = false,
    val useCompliedStorybook: Boolean = true,
)

private fun loadTestConfig(): TestConfig {
    val testConfigFile = File("src/test/.test-config.yaml")
    val config: TestConfig = if (testConfigFile.exists())
        yamlObjectMapper().readValue(testConfigFile)
    else TestConfig()
    logger.info { "Loaded test config: $config" }
    return config
}
