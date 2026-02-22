package io.orangebuffalo.simpleaccounting.infra.oauth2

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import io.orangebuffalo.simpleaccounting.infra.oauth2.impl.ClientTokenScope
import io.orangebuffalo.simpleaccounting.infra.oauth2.impl.DbReactiveOAuth2AuthorizedClientService
import io.orangebuffalo.simpleaccounting.infra.oauth2.impl.PersistentOAuth2AuthorizedClient
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
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
import org.springframework.test.context.bean.override.mockito.MockitoBean
import reactor.core.publisher.Mono
import java.time.Instant

internal class DbReactiveOAuth2AuthorizedClientServiceTest(
    @Autowired private val clientService: DbReactiveOAuth2AuthorizedClientService,
    @Autowired private val repository: PersistentOAuth2AuthorizedClientRepository,
) : SaIntegrationTestBase() {

    @field:MockitoBean
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
        @Suppress("ReactiveStreamsUnusedPublisher")
        whenever(clientRegistrationRepository.findByRegistrationId("clientRegistration")) doReturn
                Mono.just(clientRegistration)
    }

    @Test
    fun `should load client information on fully specified client`() {
        val data = setupPreconditions()
        val actualToken: OAuth2AuthorizedClient? =
            clientService.loadAuthorizedClient<OAuth2AuthorizedClient?>("clientRegistration", "fullClient").block()

        assertThat(actualToken).isNotNull().all {
            prop("accessToken") { it.accessToken }.isNotNull().all {
                prop("scopes") { it.scopes }.isEqualTo(setOf("scope"))
                prop("tokenType") { it.tokenType }.isEqualTo(OAuth2AccessToken.TokenType.BEARER)
                prop("expiresAt") { it.expiresAt }.isEqualTo(data.accessTokenExpireTime)
                prop("issuedAt") { it.issuedAt }.isEqualTo(data.accessTokenIssueTime)
                prop("tokenValue") { it.tokenValue }.isEqualTo("accessToken")
            }

            prop("clientRegistration") { it.clientRegistration }.isEqualTo(clientRegistration)
            prop("principalName") { it.principalName }.isEqualTo("fullClient")

            prop("refreshToken") { it.refreshToken }.isNotNull().all {
                prop("tokenValue") { it.tokenValue }.isEqualTo("refreshToken")
                prop("issuedAt") { it.issuedAt }.isEqualTo(data.refreshTokenIssueTime)
            }
        }
    }

    @Test
    fun `should load client information on a client without refresh token`() {
        val data = setupPreconditions()
        val actualToken: OAuth2AuthorizedClient? =
            clientService.loadAuthorizedClient<OAuth2AuthorizedClient?>("clientRegistration", "noRefreshTokenClient")
                .block()

        assertThat(actualToken).isNotNull().all {
            prop("accessToken") { it.accessToken }.isNotNull().all {
                prop("scopes") { it.scopes }.isEqualTo(setOf("scope"))
                prop("tokenType") { it.tokenType }.isEqualTo(OAuth2AccessToken.TokenType.BEARER)
                prop("expiresAt") { it.expiresAt }.isEqualTo(data.accessTokenExpireTime)
                prop("issuedAt") { it.issuedAt }.isEqualTo(data.accessTokenIssueTime)
                prop("tokenValue") { it.tokenValue }.isEqualTo("accessToken")
            }

            prop("clientRegistration") { it.clientRegistration }.isEqualTo(clientRegistration)
            prop("principalName") { it.principalName }.isEqualTo("noRefreshTokenClient")

            prop("refreshToken") { it.refreshToken }.isNull()
        }
    }

    @Test
    fun `should save new authorized client with full information provided`() {
        setupPreconditions()

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
    fun `should save new authorized client without refresh token provided`() {
        setupPreconditions()

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
    fun `should update full authorized client with full information provided`() {
        setupPreconditions()

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
    fun `should update authorized client without refresh token with full information provided`() {
        setupPreconditions()

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
    fun `should update authorized client without refresh token with information without refresh token`() {
        setupPreconditions()

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
    fun `should preserve refresh token on update full authorized client with no refresh token`() {
        val data = setupPreconditions()

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

    private fun setupPreconditions() = preconditions {
        object {
            val refreshTokenIssueTime: Instant = Instant.ofEpochMilli(47733)
            val accessTokenIssueTime: Instant = Instant.ofEpochMilli(47734)
            val accessTokenExpireTime: Instant = Instant.ofEpochMilli(47735)

            init {
                save(
                    PersistentOAuth2AuthorizedClient(
                        clientRegistrationId = "clientRegistration",
                        refreshTokenIssuedAt = refreshTokenIssueTime,
                        refreshToken = "refreshToken",
                        accessToken = "accessToken",
                        accessTokenIssuedAt = accessTokenIssueTime,
                        accessTokenExpiresAt = accessTokenExpireTime,
                        userName = "fullClient",
                        accessTokenScopes = setOf(ClientTokenScope("scope"))
                    )
                )

                save(
                    PersistentOAuth2AuthorizedClient(
                        clientRegistrationId = "clientRegistration",
                        refreshTokenIssuedAt = null,
                        refreshToken = null,
                        accessToken = "accessToken",
                        accessTokenIssuedAt = accessTokenIssueTime,
                        accessTokenExpiresAt = accessTokenExpireTime,
                        userName = "noRefreshTokenClient",
                        accessTokenScopes = setOf(ClientTokenScope("scope"))
                    )
                )
            }
        }
    }
}
