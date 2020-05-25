package io.orangebuffalo.simpleaccounting.services.integration.oauth2

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.junit.TestData
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.impl.ClientTokenScope
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.impl.DbReactiveOAuth2AuthorizedClientService
import io.orangebuffalo.simpleaccounting.services.integration.oauth2.impl.PersistentOAuth2AuthorizedClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import reactor.core.publisher.Mono
import java.time.Instant

@SimpleAccountingIntegrationTest
internal class DbReactiveOAuth2AuthorizedClientServiceTest(
    @Autowired val clientService: DbReactiveOAuth2AuthorizedClientService,
    @Autowired val repository: PersistentOAuth2AuthorizedClientRepository
) {

    @field:MockBean
    lateinit var clientRegistrationRepository: ReactiveClientRegistrationRepository

    val clientRegistration: ClientRegistration = ClientRegistration.withRegistrationId("clientRegistration")
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .tokenUri("tokenuri")
        .clientId("cid")
        .build()

    @field:Mock
    lateinit var principal: Authentication

    @BeforeEach
    fun setup() {
        whenever(clientRegistrationRepository.findByRegistrationId("clientRegistration")) doReturn
                Mono.just(clientRegistration)
    }

    @Test
    fun `should load client information on fully specified client`(data: AuthorizedClientData) {
        val actualToken: OAuth2AuthorizedClient? =
            clientService.loadAuthorizedClient<OAuth2AuthorizedClient?>("clientRegistration", "fullClient").block()

        assertThat(actualToken).isNotNull().all {
            prop(OAuth2AuthorizedClient::getAccessToken).isNotNull().all {
                prop(OAuth2AccessToken::getScopes).isEqualTo(setOf("scope"))
                prop(OAuth2AccessToken::getTokenType).isEqualTo(OAuth2AccessToken.TokenType.BEARER)
                prop(OAuth2AccessToken::getExpiresAt).isEqualTo(data.accessTokenExpireTime)
                prop(OAuth2AccessToken::getIssuedAt).isEqualTo(data.accessTokenIssueTime)
                prop(OAuth2AccessToken::getTokenValue).isEqualTo("accessToken")
            }

            prop(OAuth2AuthorizedClient::getClientRegistration).isEqualTo(clientRegistration)
            prop(OAuth2AuthorizedClient::getPrincipalName).isEqualTo("fullClient")

            prop(OAuth2AuthorizedClient::getRefreshToken).all {
                prop(OAuth2RefreshToken::getTokenValue).isEqualTo("refreshToken")
                prop(OAuth2RefreshToken::getIssuedAt).isEqualTo(data.refreshTokenIssueTime)
            }
        }
    }

    @Test
    fun `should load client information on a client without refresh token`(data: AuthorizedClientData) {
        val actualToken: OAuth2AuthorizedClient? =
            clientService.loadAuthorizedClient<OAuth2AuthorizedClient?>("clientRegistration", "noRefreshTokenClient")
                .block()

        assertThat(actualToken).isNotNull().all {
            prop(OAuth2AuthorizedClient::getAccessToken).isNotNull().all {
                prop(OAuth2AccessToken::getScopes).isEqualTo(setOf("scope"))
                prop(OAuth2AccessToken::getTokenType).isEqualTo(OAuth2AccessToken.TokenType.BEARER)
                prop(OAuth2AccessToken::getExpiresAt).isEqualTo(data.accessTokenExpireTime)
                prop(OAuth2AccessToken::getIssuedAt).isEqualTo(data.accessTokenIssueTime)
                prop(OAuth2AccessToken::getTokenValue).isEqualTo("accessToken")
            }

            prop(OAuth2AuthorizedClient::getClientRegistration).isEqualTo(clientRegistration)
            prop(OAuth2AuthorizedClient::getPrincipalName).isEqualTo("noRefreshTokenClient")

            prop(OAuth2AuthorizedClient::getRefreshToken).isNull()
        }
    }

    @Test
    fun `should save new authorized client with full information provided`(data: AuthorizedClientData) {
        val refreshTokenIssueTime: Instant = Instant.ofEpochMilli(57733)
        val accessTokenIssueTime: Instant = Instant.ofEpochMilli(57734)
        val accessTokenExpireTime: Instant = Instant.ofEpochMilli(57735)

        whenever(principal.name) doReturn "userName"

        clientService.saveAuthorizedClient(
            OAuth2AuthorizedClient(
                clientRegistration,
                "userName",
                OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "accessToken",
                    accessTokenIssueTime,
                    accessTokenExpireTime,
                    setOf("newScope")
                ),
                OAuth2RefreshToken(
                    "refreshToken",
                    refreshTokenIssueTime
                )
            ),
            principal
        ).block()

        val actualClient: PersistentOAuth2AuthorizedClient? = repository
            .findByClientRegistrationIdAndUserName("clientRegistration", "userName")

        assertThat(actualClient).isNotNull().all {
            prop(PersistentOAuth2AuthorizedClient::accessToken).isEqualTo("accessToken")
            prop(PersistentOAuth2AuthorizedClient::accessTokenExpiresAt).isEqualTo(accessTokenExpireTime)
            prop(PersistentOAuth2AuthorizedClient::accessTokenIssuedAt).isEqualTo(accessTokenIssueTime)
            prop(PersistentOAuth2AuthorizedClient::accessTokenScopes).isEqualTo(setOf(ClientTokenScope("newScope")))
            prop(PersistentOAuth2AuthorizedClient::refreshToken).isEqualTo("refreshToken")
            prop(PersistentOAuth2AuthorizedClient::refreshTokenIssuedAt).isEqualTo(refreshTokenIssueTime)
            prop(PersistentOAuth2AuthorizedClient::userName).isEqualTo("userName")
            prop(PersistentOAuth2AuthorizedClient::clientRegistrationId).isEqualTo("clientRegistration")
        }
    }

    @Test
    fun `should save new authorized client without refresh token provided`(data: AuthorizedClientData) {
        val accessTokenIssueTime: Instant = Instant.ofEpochMilli(57734)
        val accessTokenExpireTime: Instant = Instant.ofEpochMilli(57735)

        whenever(principal.name) doReturn "userName"

        clientService.saveAuthorizedClient(
            OAuth2AuthorizedClient(
                clientRegistration,
                "userName",
                OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "accessToken",
                    accessTokenIssueTime,
                    accessTokenExpireTime,
                    setOf("newScope")
                )
            ),
            principal
        ).block()

        val actualClient: PersistentOAuth2AuthorizedClient? = repository
            .findByClientRegistrationIdAndUserName("clientRegistration", "userName")

        assertThat(actualClient).isNotNull().all {
            prop(PersistentOAuth2AuthorizedClient::accessToken).isEqualTo("accessToken")
            prop(PersistentOAuth2AuthorizedClient::accessTokenExpiresAt).isEqualTo(accessTokenExpireTime)
            prop(PersistentOAuth2AuthorizedClient::accessTokenIssuedAt).isEqualTo(accessTokenIssueTime)
            prop(PersistentOAuth2AuthorizedClient::accessTokenScopes).isEqualTo(setOf(ClientTokenScope("newScope")))
            prop(PersistentOAuth2AuthorizedClient::refreshToken).isNull()
            prop(PersistentOAuth2AuthorizedClient::refreshTokenIssuedAt).isNull()
            prop(PersistentOAuth2AuthorizedClient::userName).isEqualTo("userName")
            prop(PersistentOAuth2AuthorizedClient::clientRegistrationId).isEqualTo("clientRegistration")
        }
    }

    @Test
    fun `should update full authorized client with full information provided`(data: AuthorizedClientData) {
        val refreshTokenIssueTime: Instant = Instant.ofEpochMilli(57733)
        val accessTokenIssueTime: Instant = Instant.ofEpochMilli(57734)
        val accessTokenExpireTime: Instant = Instant.ofEpochMilli(57735)

        whenever(principal.name) doReturn "fullClient"

        clientService.saveAuthorizedClient(
            OAuth2AuthorizedClient(
                clientRegistration,
                "fullClient",
                OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "newAccessToken",
                    accessTokenIssueTime,
                    accessTokenExpireTime,
                    setOf("newScope")
                ),
                OAuth2RefreshToken(
                    "newRefreshToken",
                    refreshTokenIssueTime
                )
            ),
            principal
        ).block()

        val actualClient: PersistentOAuth2AuthorizedClient? = repository
            .findByClientRegistrationIdAndUserName("clientRegistration", "fullClient")

        assertThat(actualClient).isNotNull().all {
            prop(PersistentOAuth2AuthorizedClient::accessToken).isEqualTo("newAccessToken")
            prop(PersistentOAuth2AuthorizedClient::accessTokenExpiresAt).isEqualTo(accessTokenExpireTime)
            prop(PersistentOAuth2AuthorizedClient::accessTokenIssuedAt).isEqualTo(accessTokenIssueTime)
            prop(PersistentOAuth2AuthorizedClient::accessTokenScopes).isEqualTo(setOf(ClientTokenScope("newScope")))
            prop(PersistentOAuth2AuthorizedClient::refreshToken).isEqualTo("newRefreshToken")
            prop(PersistentOAuth2AuthorizedClient::refreshTokenIssuedAt).isEqualTo(refreshTokenIssueTime)
            prop(PersistentOAuth2AuthorizedClient::userName).isEqualTo("fullClient")
            prop(PersistentOAuth2AuthorizedClient::clientRegistrationId).isEqualTo("clientRegistration")
        }
    }

    @Test
    fun `should update authorized client without refresh token with full information provided`(data: AuthorizedClientData) {
        val refreshTokenIssueTime: Instant = Instant.ofEpochMilli(57733)
        val accessTokenIssueTime: Instant = Instant.ofEpochMilli(57734)
        val accessTokenExpireTime: Instant = Instant.ofEpochMilli(57735)

        whenever(principal.name) doReturn "noRefreshTokenClient"

        clientService.saveAuthorizedClient(
            OAuth2AuthorizedClient(
                clientRegistration,
                "noRefreshTokenClient",
                OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "newAccessToken",
                    accessTokenIssueTime,
                    accessTokenExpireTime,
                    setOf("newScope")
                ),
                OAuth2RefreshToken(
                    "newRefreshToken",
                    refreshTokenIssueTime
                )
            ),
            principal
        ).block()

        val actualClient: PersistentOAuth2AuthorizedClient? = repository
            .findByClientRegistrationIdAndUserName("clientRegistration", "noRefreshTokenClient")

        assertThat(actualClient).isNotNull().all {
            prop(PersistentOAuth2AuthorizedClient::accessToken).isEqualTo("newAccessToken")
            prop(PersistentOAuth2AuthorizedClient::accessTokenExpiresAt).isEqualTo(accessTokenExpireTime)
            prop(PersistentOAuth2AuthorizedClient::accessTokenIssuedAt).isEqualTo(accessTokenIssueTime)
            prop(PersistentOAuth2AuthorizedClient::accessTokenScopes).isEqualTo(setOf(ClientTokenScope("newScope")))
            prop(PersistentOAuth2AuthorizedClient::refreshToken).isEqualTo("newRefreshToken")
            prop(PersistentOAuth2AuthorizedClient::refreshTokenIssuedAt).isEqualTo(refreshTokenIssueTime)
            prop(PersistentOAuth2AuthorizedClient::userName).isEqualTo("noRefreshTokenClient")
            prop(PersistentOAuth2AuthorizedClient::clientRegistrationId).isEqualTo("clientRegistration")
        }
    }

    @Test
    fun `should update authorized client without refresh token with information without refresh token`(data: AuthorizedClientData) {
        val accessTokenIssueTime: Instant = Instant.ofEpochMilli(57734)
        val accessTokenExpireTime: Instant = Instant.ofEpochMilli(57735)

        whenever(principal.name) doReturn "noRefreshTokenClient"

        clientService.saveAuthorizedClient(
            OAuth2AuthorizedClient(
                clientRegistration,
                "noRefreshTokenClient",
                OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "newAccessToken",
                    accessTokenIssueTime,
                    accessTokenExpireTime,
                    setOf("newScope")
                )
            ),
            principal
        ).block()

        val actualClient: PersistentOAuth2AuthorizedClient? = repository
            .findByClientRegistrationIdAndUserName("clientRegistration", "noRefreshTokenClient")

        assertThat(actualClient).isNotNull().all {
            prop(PersistentOAuth2AuthorizedClient::accessToken).isEqualTo("newAccessToken")
            prop(PersistentOAuth2AuthorizedClient::accessTokenExpiresAt).isEqualTo(accessTokenExpireTime)
            prop(PersistentOAuth2AuthorizedClient::accessTokenIssuedAt).isEqualTo(accessTokenIssueTime)
            prop(PersistentOAuth2AuthorizedClient::accessTokenScopes).isEqualTo(setOf(ClientTokenScope("newScope")))
            prop(PersistentOAuth2AuthorizedClient::refreshToken).isNull()
            prop(PersistentOAuth2AuthorizedClient::refreshTokenIssuedAt).isNull()
            prop(PersistentOAuth2AuthorizedClient::userName).isEqualTo("noRefreshTokenClient")
            prop(PersistentOAuth2AuthorizedClient::clientRegistrationId).isEqualTo("clientRegistration")
        }
    }

    @Test
    fun `should preserve refresh token on update full authorized client with no refresh token`(data: AuthorizedClientData) {
        val accessTokenIssueTime: Instant = Instant.ofEpochMilli(57734)
        val accessTokenExpireTime: Instant = Instant.ofEpochMilli(57735)

        whenever(principal.name) doReturn "fullClient"

        clientService.saveAuthorizedClient(
            OAuth2AuthorizedClient(
                clientRegistration,
                "fullClient",
                OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "newAccessToken",
                    accessTokenIssueTime,
                    accessTokenExpireTime,
                    setOf("newScope")
                )
            ),
            principal
        ).block()

        val actualClient: PersistentOAuth2AuthorizedClient? = repository
            .findByClientRegistrationIdAndUserName("clientRegistration", "fullClient")

        assertThat(actualClient).isNotNull().all {
            prop(PersistentOAuth2AuthorizedClient::accessToken).isEqualTo("newAccessToken")
            prop(PersistentOAuth2AuthorizedClient::accessTokenExpiresAt).isEqualTo(accessTokenExpireTime)
            prop(PersistentOAuth2AuthorizedClient::accessTokenIssuedAt).isEqualTo(accessTokenIssueTime)
            prop(PersistentOAuth2AuthorizedClient::accessTokenScopes).isEqualTo(setOf(ClientTokenScope("newScope")))
            prop(PersistentOAuth2AuthorizedClient::refreshToken).isEqualTo("refreshToken")
            prop(PersistentOAuth2AuthorizedClient::refreshTokenIssuedAt).isEqualTo(data.refreshTokenIssueTime)
            prop(PersistentOAuth2AuthorizedClient::userName).isEqualTo("fullClient")
            prop(PersistentOAuth2AuthorizedClient::clientRegistrationId).isEqualTo("clientRegistration")
        }
    }
}

class AuthorizedClientData : TestData {
    val refreshTokenIssueTime: Instant = Instant.ofEpochMilli(47733)
    val accessTokenIssueTime: Instant = Instant.ofEpochMilli(47734)
    val accessTokenExpireTime: Instant = Instant.ofEpochMilli(47735)

    private val fullClient = PersistentOAuth2AuthorizedClient(
        clientRegistrationId = "clientRegistration",
        refreshTokenIssuedAt = refreshTokenIssueTime,
        refreshToken = "refreshToken",
        accessToken = "accessToken",
        accessTokenIssuedAt = accessTokenIssueTime,
        accessTokenExpiresAt = accessTokenExpireTime,
        userName = "fullClient",
        accessTokenScopes = setOf(ClientTokenScope("scope"))
    )

    private val noRefreshTokenClient = PersistentOAuth2AuthorizedClient(
        clientRegistrationId = "clientRegistration",
        refreshTokenIssuedAt = null,
        refreshToken = null,
        accessToken = "accessToken",
        accessTokenIssuedAt = accessTokenIssueTime,
        accessTokenExpiresAt = accessTokenExpireTime,
        userName = "noRefreshTokenClient",
        accessTokenScopes = setOf(ClientTokenScope("scope"))
    )

    override fun generateData() = listOf(fullClient, noRefreshTokenClient)
}
