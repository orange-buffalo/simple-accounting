package io.orangebuffalo.simpleaccounting.business.security.authentication

import org.springframework.security.core.AuthenticationException

class LoginUnavailableException : AuthenticationException("Login is temporary unavailable")
