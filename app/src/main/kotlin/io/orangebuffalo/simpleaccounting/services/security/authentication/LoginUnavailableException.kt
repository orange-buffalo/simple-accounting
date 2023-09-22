package io.orangebuffalo.simpleaccounting.services.security.authentication

import org.springframework.security.core.AuthenticationException

class LoginUnavailableException : AuthenticationException("Login is temporary unavailable")
