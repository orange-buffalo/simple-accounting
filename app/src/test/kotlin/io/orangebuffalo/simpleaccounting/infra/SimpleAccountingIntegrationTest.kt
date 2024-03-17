package io.orangebuffalo.simpleaccounting.infra

import io.orangebuffalo.simpleaccounting.infra.database.TestDataDeprecatedExtension
import io.orangebuffalo.simpleaccounting.infra.database.TestDataExtension
import io.orangebuffalo.simpleaccounting.infra.security.TestPasswordEncoderConfig
import io.orangebuffalo.simpleaccounting.infra.security.TestPasswordEncoderListener
import io.orangebuffalo.simpleaccounting.infra.utils.TestsMocksConfiguration
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class, TestDataDeprecatedExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = ["spring.profiles.active=test"])
@Import(TestPasswordEncoderConfig::class, TestsMocksConfiguration::class)
@TestExecutionListeners(
    listeners = [TestPasswordEncoderListener::class],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SimpleAccountingIntegrationTest
