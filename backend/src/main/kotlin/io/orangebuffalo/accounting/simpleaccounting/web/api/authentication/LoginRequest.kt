package io.orangebuffalo.accounting.simpleaccounting.web.api.authentication

data class LoginRequest(
        val userName: String,
        val password: String,
        val rememberMe: Boolean = false
)