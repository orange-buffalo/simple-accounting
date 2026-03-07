package io.orangebuffalo.simpleaccounting.business.security

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class SecurityUtilsTest {

    @Test
    fun `should propagate security principal via runAs`() {
        val result = runBlocking {
            runAs(fry().toSecurityPrincipal()) {
                getCurrentPrincipal().userName.shouldBe("Fry")
                getAuthentication().isAuthenticated.shouldBeTrue()
                "result"
            }
        }
        result.shouldBe("result")
    }

    private fun fry() = PlatformUser(
        userName = "Fry",
        isAdmin = false,
        passwordHash = "hash",
        activated = true,
        i18nSettings = I18nSettings(locale = "en", language = "en")
    )
}
