package io.orangebuffalo.simpleaccounting.services.security

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class SecurityUtilsTest {

    @Test
    fun `should propagate security principal via runAs`() {
        val result = runBlocking {
            runAs(Prototypes.fry().toSecurityPrincipal()) {
                assertThat(getCurrentPrincipal().userName).isEqualTo("Fry")
                assertThat(getAuthentication().isAuthenticated).isTrue()
                "result"
            }
        }
        assertThat(result).isEqualTo("result")
    }

}
