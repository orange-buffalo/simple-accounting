package io.orangebuffalo.simpleaccounting.tests.infra.ui

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.Cookie
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshToken
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksContextInitializer
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksListener
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import java.time.Instant

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
abstract class SaFullStackTestBase : SaIntegrationTestBase() {

    protected fun Page.authenticateViaCookie(user: PlatformUser) {
        val tokenValue = "test-refresh-token:${user.userName}"
        val refreshToken = RefreshToken(
            userId = user.id!!,
            token = tokenValue,
            expirationTime = Instant.parse("9999-12-31T23:59:59Z")
        )
        aggregateTemplate.insert(refreshToken)
        this.context().addCookies(listOf(
            Cookie("refreshToken", tokenValue)
                .setUrl(getBrowserUrl())
                .setHttpOnly(true),
        ))
    }
}
