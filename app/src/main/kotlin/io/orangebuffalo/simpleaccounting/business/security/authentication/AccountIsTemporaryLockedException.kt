package io.orangebuffalo.simpleaccounting.business.security.authentication

import org.springframework.security.core.AuthenticationException

class AccountIsTemporaryLockedException(
    val lockExpiresInSec: Long
) : AuthenticationException("Account is temporary locked")
