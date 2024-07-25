package io.orangebuffalo.simpleaccounting.tests.infra

import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClientConfig
import io.orangebuffalo.simpleaccounting.tests.infra.database.DatabaseCleanupExtension
import io.orangebuffalo.simpleaccounting.tests.infra.database.PreconditionsFactoryExtension
import io.orangebuffalo.simpleaccounting.tests.infra.security.TestPasswordEncoderConfig
import io.orangebuffalo.simpleaccounting.tests.infra.security.TestPasswordEncoderListener
import io.orangebuffalo.simpleaccounting.tests.infra.utils.TestsMocksConfiguration
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class, DatabaseCleanupExtension::class, PreconditionsFactoryExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = ["spring.profiles.active=test"])
@Import(TestPasswordEncoderConfig::class, TestsMocksConfiguration::class, ApiTestClientConfig::class)
@TestExecutionListeners(
    listeners = [TestPasswordEncoderListener::class],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SimpleAccountingIntegrationTest
