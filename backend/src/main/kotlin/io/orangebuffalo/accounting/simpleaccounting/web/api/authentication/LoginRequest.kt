package io.orangebuffalo.accounting.simpleaccounting.web.api.authentication

import javax.validation.constraints.NotBlank

data class LoginRequest(
        @field:NotBlank val userName: String,
        @field:NotBlank val password: String,
        val rememberMe: Boolean = false
)