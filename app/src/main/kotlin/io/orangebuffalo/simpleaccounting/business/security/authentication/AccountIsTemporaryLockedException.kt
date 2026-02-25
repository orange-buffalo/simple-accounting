package io.orangebuffalo.simpleaccounting.business.security.authentication

import io.orangebuffalo.simpleaccounting.business.api.errors.GraphQlBusinessErrorExtensionsProvider
import org.springframework.security.core.AuthenticationException

class AccountIsTemporaryLockedException(
    val lockExpiresInSec: Long
) : AuthenticationException("Account is temporary locked"), GraphQlBusinessErrorExtensionsProvider {
    override fun getBusinessErrorExtensions() = mapOf("lockExpiresInSec" to lockExpiresInSec)
}
