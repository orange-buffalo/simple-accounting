package io.orangebuffalo.simpleaccounting.tests.infra.ui

import io.orangebuffalo.simpleaccounting.tests.infra.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksContextInitializer
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksListener
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners

/**
 * Base test class for all Full Stack tests - those which verify UI.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ExtendWith(SaPlaywrightExtension::class)
@TestExecutionListeners(
    listeners = [ThirdPartyApisMocksListener::class],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@ContextConfiguration(
    initializers = [
        FullStackTestsSpringContextInitializer::class,
        ThirdPartyApisMocksContextInitializer::class,
    ]
)
abstract class SaFullStackTestBase : SaIntegrationTestBase()
