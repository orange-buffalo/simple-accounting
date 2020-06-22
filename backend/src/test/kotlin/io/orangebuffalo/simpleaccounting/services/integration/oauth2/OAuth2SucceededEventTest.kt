package io.orangebuffalo.simpleaccounting.services.integration.oauth2

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.services.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.services.security.getCurrentPrincipal
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

class OAuth2SucceededEventTest {

    @Test
    fun `should execute the client registration request within user context`() {
        val event = OAuth2SucceededEvent(
            user = Prototypes.fry(),
            context = EmptyCoroutineContext,
            clientRegistrationId = "test-client"
        )

        var principal: SecurityPrincipal? = null
        val job = event.launchIfClientMatches("test-client") {
            principal = getCurrentPrincipal()
        }

        assertThat(job).isNotNull()

        runBlocking { job!!.join() }

        assertThat(principal).isNotNull()
        assertThat(principal!!.userName).isEqualTo("Fry")
    }
}
