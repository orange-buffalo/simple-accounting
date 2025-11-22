package io.orangebuffalo.simpleaccounting.tests.infra.environment

import com.fasterxml.jackson.module.kotlin.readValue
import io.orangebuffalo.simpleaccounting.tests.infra.utils.logger
import io.orangebuffalo.simpleaccounting.tests.infra.utils.yamlObjectMapper
import java.io.File

data class TestConfig(
    val fullStackTests: FullStackTestsConfig = FullStackTestsConfig(),
    val apiContracts: ApiContractsConfig = ApiContractsConfig(),
    val hostIpInDockerContainer: String = "172.17.0.1",
) {
    companion object {
        val instance: TestConfig by lazy { loadTestConfig() }
    }
}

data class FullStackTestsConfig(
    val useHeadlessBrowser: Boolean = true,
    val useViteDevServer: Boolean = false,
    val viteDevServerSpringContextPort: Int = 5174,
    val slowMoMs: Int = 0,
    val usePersistentContext: Boolean = false,
)

data class ApiContractsConfig(
    val overrideCommittedSchema: Boolean = false,
)

private fun loadTestConfig(): TestConfig {
    val testConfigFile = File("src/test/.test-config.yaml")
    val config: TestConfig = if (testConfigFile.exists())
        yamlObjectMapper().readValue(testConfigFile)
    else TestConfig()
    logger.info { "Loaded test config: $config" }
    return config
}
