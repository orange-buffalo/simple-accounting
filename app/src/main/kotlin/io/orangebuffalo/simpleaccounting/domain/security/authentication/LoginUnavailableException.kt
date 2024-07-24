package io.orangebuffalo.simpleaccounting.domain.security.authentication

import org.springframework.security.core.AuthenticationException

class LoginUnavailableException : AuthenticationException("Login is temporary unavailable")
