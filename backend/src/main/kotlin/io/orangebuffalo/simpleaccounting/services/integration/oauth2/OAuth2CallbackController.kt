package io.orangebuffalo.simpleaccounting.services.integration.oauth2

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val AUTH_CALLBACK_PATH = "/api/auth/oauth2/callback"

@RestController
class OAuth2CallbackController(
    private val clientAuthorizationProvider: OAuth2ClientAuthorizationProvider
) {

    @RequestMapping(AUTH_CALLBACK_PATH, produces = [MediaType.TEXT_HTML_VALUE])
    suspend fun authCallback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) state: String?
    ): String {
        clientAuthorizationProvider.handleAuthorizationResponse(code = code, error = error, state = state)
        //todo #83: render "nice" page
        return "Done.. hold on a second.."
    }
}
