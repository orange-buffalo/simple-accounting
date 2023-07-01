package io.orangebuffalo.simpleaccounting.services.security.login

import org.springframework.security.core.AuthenticationException

class LoginUnavailableException : AuthenticationException("Login is temporary unavailable")
