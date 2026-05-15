package io.orangebuffalo.simpleaccounting.infra.oauth2

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.getCurrentPrincipal
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

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

        principal.shouldNotBeNull()
        principal.userName.shouldBe("Fry")
    }

    @Test
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    fun `should not dispatch back to source context dispatcher`() = runBlocking {
        val sourceContext = newSingleThreadContext("oauth-source-context")

        try {
            withContext(sourceContext) {
                val event = OAuth2SucceededEvent(
                    user = fry(),
                    context = coroutineContext,
                    clientRegistrationId = "test-client"
                )

                event.executeInSourceContext("test-client") {
                    getCurrentPrincipal().userName.shouldBe("Fry")
                }
            }
        } finally {
            sourceContext.close()
        }
    }

    private fun fry() = PlatformUser(
        userName = "Fry",
        isAdmin = false,
        passwordHash = "hash",
        activated = true,
        i18nSettings = I18nSettings(locale = "en", language = "en")
    )
}
