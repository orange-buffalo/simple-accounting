package io.orangebuffalo.accounting.simpleaccounting.services.storage.gdrive

import io.orangebuffalo.accounting.simpleaccounting.services.integration.toMono
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.reactor.mono
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

const val AUTH_CALLBACK_PATH = "/api/v1/auth/storage/google-drive/callback"

@RestController
class GoogleDriveStorageController(
    private val storageService: GoogleDriveDocumentStorageService
) {

    @RequestMapping("/api/v1/user/storage/google-drive/status")
    fun getIntegrationStatus(): Mono<GoogleDriveStorageIntegrationStatus> = toMono {
        storageService.getCurrentUserIntegrationStatus()
    }

    @RequestMapping("/api/v1/user/storage/google-drive/auth")
    fun getAuthorizationUrl(): Mono<String?> = toMono {
        storageService.buildAuthorizationUrl()
    }

    @RequestMapping(AUTH_CALLBACK_PATH, produces = [MediaType.TEXT_HTML_VALUE])
    fun authCallback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) state: String?
    ): Mono<String> = GlobalScope.mono {
        if (state == null) {
            "Something bad happened, sorry :( Please try again"
        } else {
            if (error != null || code == null) {
                storageService.onAuthFailure(state)
            } else {
                storageService.onAuthSuccess(code = code, authStateToken = state)
            }
            "Done.. hold on a second.."
        }
    }
}