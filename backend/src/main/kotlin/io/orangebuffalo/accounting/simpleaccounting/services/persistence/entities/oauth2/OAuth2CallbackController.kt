package io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.oauth2

import io.orangebuffalo.accounting.simpleaccounting.services.oauth2.OAuth2Service
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.reactor.mono
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

const val AUTH_CALLBACK_PATH = "/api/auth/oauth2/callback"

@RestController
class OAuth2CallbackController(
    private val oauth2Service: OAuth2Service
) {

    @RequestMapping(AUTH_CALLBACK_PATH, produces = [MediaType.TEXT_HTML_VALUE])
    fun authCallback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) state: String?
    ): Mono<String> = GlobalScope.mono {
        oauth2Service.onAuthCallback(code = code, error = error, state = state)
        //todo #83: render "nice" page
        "Done.. hold on a second.."
    }
}