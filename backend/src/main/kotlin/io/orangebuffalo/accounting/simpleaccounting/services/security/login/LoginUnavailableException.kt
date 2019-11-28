package io.orangebuffalo.accounting.simpleaccounting.services.security.login

import org.springframework.security.core.AuthenticationException

class LoginUnavailableException : AuthenticationException("Login is temporary unavailable")
