package io.orangebuffalo.accounting.simpleaccounting.services.security.login

import org.springframework.security.core.AuthenticationException

class AccountIsTemporaryLockedException(
    val lockExpiresInSec: Long
) : AuthenticationException("Account is temporary locked")
