package io.orangebuffalo.simpleaccounting.infra.oauth2

import io.orangebuffalo.simpleaccounting.infra.TokenGenerator
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

const val AUTH_CALLBACK_PATH = "/api/auth/oauth2/callback"

private val logger = KotlinLogging.logger {}

@RestController
class OAuth2CallbackApi(
    private val clientAuthorizationProvider: OAuth2ClientAuthorizationProvider,
    private val tokenGenerator: TokenGenerator,
) {

    @PostMapping(AUTH_CALLBACK_PATH)
    suspend fun authCallback(@RequestBody callbackRequest: OAuth2AuthorizationCallbackRequest) {
        logger.debug { "Received new OAuth2 authorization callback" }

        clientAuthorizationProvider.handleAuthorizationResponse(callbackRequest)

        logger.debug { "OAuth2 authorization callback successfully processed" }
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun onError(error: Exception): ErrorResponse {
        val errorId = tokenGenerator.generateUuid()
        logger.error(error) { "Failure to process OAuth2 authorization callback. Error ID is $errorId" }

        return ErrorResponse(errorId)
    }

    data class ErrorResponse(val errorId: String)
}
