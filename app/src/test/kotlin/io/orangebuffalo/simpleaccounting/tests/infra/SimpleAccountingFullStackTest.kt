package io.orangebuffalo.simpleaccounting.tests.infra

import com.microsoft.playwright.junit.UsePlaywright
import io.orangebuffalo.simpleaccounting.tests.infra.database.DatabaseCleanupExtension
import io.orangebuffalo.simpleaccounting.tests.infra.database.PreconditionsFactoryExtension
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksContextInitializer
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksListener
import io.orangebuffalo.simpleaccounting.tests.infra.ui.FullStackTestsPlaywrightOptions
import io.orangebuffalo.simpleaccounting.tests.infra.ui.FullStackTestsSpringContextInitializer
import io.orangebuffalo.simpleaccounting.tests.infra.utils.TestsMocksConfiguration
import io.orangebuffalo.simpleaccounting.tests.infra.utils.TestsMocksListener
import io.orangebuffalo.simpleaccounting.tests.infra.utils.TestsUtilsConfiguration
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
    PreconditionsFactoryExtension::class
)
@UsePlaywright(FullStackTestsPlaywrightOptions::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import(TestsMocksConfiguration::class, TestsUtilsConfiguration::class)
@TestExecutionListeners(
    listeners = [TestsMocksListener::class, ThirdPartyApisMocksListener::class],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@ContextConfiguration(
    initializers = [
        FullStackTestsSpringContextInitializer::class,
        ThirdPartyApisMocksContextInitializer::class,
    ]
)
@TestPropertySource(properties = ["spring.profiles.active=test"])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SimpleAccountingFullStackTest
