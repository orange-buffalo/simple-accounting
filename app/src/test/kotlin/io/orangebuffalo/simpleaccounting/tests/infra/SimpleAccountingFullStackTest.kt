package io.orangebuffalo.simpleaccounting.tests.infra

import io.orangebuffalo.simpleaccounting.tests.infra.database.DatabaseCleanupExtension
import io.orangebuffalo.simpleaccounting.tests.infra.database.PreconditionsFactoryExtension
import io.orangebuffalo.simpleaccounting.tests.infra.security.TestPasswordEncoderConfig
import io.orangebuffalo.simpleaccounting.tests.infra.security.TestPasswordEncoderListener
import io.orangebuffalo.simpleaccounting.tests.infra.ui.FullStackTestsPlaywrightConfigurer
import io.orangebuffalo.simpleaccounting.tests.infra.ui.FullStackTestsSpringContextInitializer
import io.orangebuffalo.simpleaccounting.tests.infra.utils.TestsMocksConfiguration
import io.orangebuffalo.simpleaccounting.tests.infra.utils.TestsUtilsConfiguration
import io.orangebuffalo.testcontainers.playwright.junit.PlaywrightConfig
import io.orangebuffalo.testcontainers.playwright.junit.PlaywrightExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(
    SpringExtension::class,
    DatabaseCleanupExtension::class,
    PlaywrightExtension::class,
    PreconditionsFactoryExtension::class
)
@PlaywrightConfig(configurer = FullStackTestsPlaywrightConfigurer::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import(TestPasswordEncoderConfig::class, TestsMocksConfiguration::class, TestsUtilsConfiguration::class)
@TestExecutionListeners(
    listeners = [TestPasswordEncoderListener::class],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@ContextConfiguration(initializers = [FullStackTestsSpringContextInitializer::class])
@TestPropertySource(properties = ["spring.profiles.active=test"])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SimpleAccountingFullStackTest
