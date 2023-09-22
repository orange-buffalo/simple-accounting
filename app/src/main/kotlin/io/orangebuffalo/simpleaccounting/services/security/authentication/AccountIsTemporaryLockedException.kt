package io.orangebuffalo.simpleaccounting.services.security.authentication

import org.springframework.security.core.AuthenticationException

class AccountIsTemporaryLockedException(
    val lockExpiresInSec: Long
) : AuthenticationException("Account is temporary locked")
