package io.orangebuffalo.simpleaccounting.services.integration.oauth2

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.util.*

const val AUTH_CALLBACK_PATH = "/api/auth/oauth2/callback"

private val logger = KotlinLogging.logger {}

@RestController
class OAuth2CallbackController(
    private val clientAuthorizationProvider: OAuth2ClientAuthorizationProvider
) {

    // todo #225: remove this method
    @GetMapping(AUTH_CALLBACK_PATH, produces = [MediaType.TEXT_HTML_VALUE])
    suspend fun authCallbackLegacy(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) state: String?
    ): String {
        clientAuthorizationProvider.handleAuthorizationResponse(code = code, error = error, state = state)
        //todo #83: render "nice" page
        return "Done.. hold on a second.."
    }

    @PostMapping(AUTH_CALLBACK_PATH)
    suspend fun authCallback(@RequestBody callbackRequest: OAuth2AuthorizationCallbackRequest) {
        logger.debug { "Received new OAuth2 authorization callback" }

        clientAuthorizationProvider.handleAuthorizationResponse(callbackRequest)

        logger.debug { "OAuth2 authorization callback successfully processed" }
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun onError(error: Exception): ErrorResponse {
        val errorId = UUID.randomUUID().toString()
        logger.error(error) { "Failure to process OAuth2 authorization callback. Error ID is $errorId" }

        return ErrorResponse(errorId)
    }

    data class ErrorResponse(val errorId: String)
}
