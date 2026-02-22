package io.orangebuffalo.simpleaccounting.infra.oauth2

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import io.orangebuffalo.simpleaccounting.business.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.getCurrentPrincipal
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

class OAuth2SucceededEventTest {

    @Test
    fun `should execute the client registration request within user context`() {
        val event = OAuth2SucceededEvent(
            user = fry(),
            context = EmptyCoroutineContext,
            clientRegistrationId = "test-client"
        )

        var principal: SecurityPrincipal? = null
        event.executeInSourceContext("test-client") {
            principal = getCurrentPrincipal()
        }

        assertThat(principal).isNotNull()
        assertThat(principal!!.userName).isEqualTo("Fry")
    }

    private fun fry() = PlatformUser(
        userName = "Fry",
        isAdmin = false,
        passwordHash = "hash",
        activated = true,
        i18nSettings = I18nSettings(locale = "en", language = "en")
    )
}
