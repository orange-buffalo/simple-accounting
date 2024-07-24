package io.orangebuffalo.simpleaccounting.domain.security.authentication

import org.springframework.security.core.AuthenticationException

class AccountIsTemporaryLockedException(
    val lockExpiresInSec: Long
) : AuthenticationException("Account is temporary locked")
