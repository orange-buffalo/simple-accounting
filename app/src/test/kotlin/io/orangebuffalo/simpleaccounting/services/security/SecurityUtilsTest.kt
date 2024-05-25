package io.orangebuffalo.simpleaccounting.services.security

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.orangebuffalo.simpleaccounting.domain.users.I18nSettings
import io.orangebuffalo.simpleaccounting.domain.users.PlatformUser
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class SecurityUtilsTest {

    @Test
    fun `should propagate security principal via runAs`() {
        val result = runBlocking {
            runAs(fry().toSecurityPrincipal()) {
                assertThat(getCurrentPrincipal().userName).isEqualTo("Fry")
                assertThat(getAuthentication().isAuthenticated).isTrue()
                "result"
            }
        }
        assertThat(result).isEqualTo("result")
    }

    private fun fry() = PlatformUser(
        userName = "Fry",
        isAdmin = false,
        passwordHash = "hash",
        activated = true,
        i18nSettings = I18nSettings(locale = "en", language = "en")
    )
}
