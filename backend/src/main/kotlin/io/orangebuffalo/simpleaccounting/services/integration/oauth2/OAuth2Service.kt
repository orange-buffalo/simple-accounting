package io.orangebuffalo.simpleaccounting.services.integration.oauth2

import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.security.ensureRegularUserPrincipal
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.time.temporal.ChronoUnit

// todo #225: split and re-write
@Service
class OAuth2Service(
    private val clientRegistrationRepository: ReactiveClientRegistrationRepository,
    private val persistentAuthorizedClientRepository: PersistentOAuth2AuthorizedClientRepository,
    private val timeService: TimeService,
    private val authorizedClientRepository: ServerOAuth2AuthorizedClientRepository,
    private val clientService: ReactiveOAuth2AuthorizedClientService
) {

    // todo #225: evaluate usage of user id to avoid extra db roundtrips
    suspend fun getOAuth2AuthorizedClient(clientRegistrationId: String, userName: String): OAuth2AuthorizedClient? {
        return clientService
            .loadAuthorizedClient<OAuth2AuthorizedClient>(clientRegistrationId, userName)
            .awaitFirstOrNull()
            ?.let { client -> if (expiresSoon(client)) null else client }
    }

    private fun expiresSoon(client: OAuth2AuthorizedClient): Boolean {
        val expiresAt = client.accessToken.expiresAt
        return expiresAt != null
                && client.refreshToken == null
                && Duration.between(timeService.currentTime(), expiresAt).get(ChronoUnit.SECONDS) < 60
    }

    fun createWebClient(baseUrl: String): WebClient = WebClient.builder()
        .filter(
            // todo #225: with new version of security library, this can delete the client. we should remove related custom code
            // io/orangebuffalo/simpleaccounting/services/storage/gdrive/GoogleDriveDocumentsStorageService.kt:305
            ServerOAuth2AuthorizedClientExchangeFilterFunction(
                clientRegistrationRepository,
                authorizedClientRepository
            )
        )
        .baseUrl(baseUrl)
        .build()

    suspend fun deleteAuthorizedClient(clientRegistrationId: String) = withDbContext {
        persistentAuthorizedClientRepository.deleteByClientRegistrationIdAndUserName(
            clientRegistrationId, ensureRegularUserPrincipal().userName
        )
    }
}
