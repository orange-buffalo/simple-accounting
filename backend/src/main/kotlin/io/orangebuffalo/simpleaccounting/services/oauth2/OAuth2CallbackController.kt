package io.orangebuffalo.simpleaccounting.services.oauth2

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

const val AUTH_CALLBACK_PATH = "/api/auth/oauth2/callback"

@RestController
class OAuth2CallbackController(
    private val oauth2Service: OAuth2Service
) {

    @RequestMapping(AUTH_CALLBACK_PATH, produces = [MediaType.TEXT_HTML_VALUE])
    suspend fun authCallback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) state: String?
    ): String  {
        oauth2Service.onAuthCallback(code = code, error = error, state = state)
        //todo #83: render "nice" page
        return "Done.. hold on a second.."
    }
}
