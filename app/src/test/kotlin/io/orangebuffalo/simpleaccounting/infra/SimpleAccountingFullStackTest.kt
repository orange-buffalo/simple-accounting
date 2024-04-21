package io.orangebuffalo.simpleaccounting.infra

import io.orangebuffalo.simpleaccounting.infra.database.TestDataDeprecatedExtension
import io.orangebuffalo.simpleaccounting.infra.database.TestDataExtension
import io.orangebuffalo.simpleaccounting.infra.security.TestPasswordEncoderConfig
import io.orangebuffalo.simpleaccounting.infra.security.TestPasswordEncoderListener
import io.orangebuffalo.simpleaccounting.infra.ui.FullStackTestsPlaywrightConfigurer
import io.orangebuffalo.simpleaccounting.infra.ui.FullStackTestsSpringContextInitializer
import io.orangebuffalo.simpleaccounting.infra.utils.TestsMocksConfiguration
import io.orangebuffalo.simpleaccounting.infra.utils.TestsUtilsConfiguration
import io.orangebuffalo.testcontainers.playwright.junit.PlaywrightConfig
import io.orangebuffalo.testcontainers.playwright.junit.PlaywrightExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class, TestDataDeprecatedExtension::class, TestDataExtension::class, PlaywrightExtension::class)
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
