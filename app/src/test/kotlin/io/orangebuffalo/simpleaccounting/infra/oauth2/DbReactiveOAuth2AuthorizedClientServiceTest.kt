package io.orangebuffalo.simpleaccounting.infra.oauth2

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
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

        actualToken.shouldNotBeNull()
        actualToken.accessToken.shouldNotBeNull()
        actualToken.accessToken.scopes.shouldBe(setOf("scope"))
        actualToken.accessToken.tokenType.shouldBe(OAuth2AccessToken.TokenType.BEARER)
        actualToken.accessToken.expiresAt.shouldBe(data.accessTokenExpireTime)
        actualToken.accessToken.issuedAt.shouldBe(data.accessTokenIssueTime)
        actualToken.accessToken.tokenValue.shouldBe("accessToken")
        actualToken.clientRegistration.shouldBe(clientRegistration)
        actualToken.principalName.shouldBe("fullClient")
        actualToken.refreshToken.shouldNotBeNull()
        actualToken.refreshToken!!.tokenValue.shouldBe("refreshToken")
        actualToken.refreshToken!!.issuedAt.shouldBe(data.refreshTokenIssueTime)
    }

    @Test
    fun `should load client information on a client without refresh token`() {
        val data = setupPreconditions()
        val actualToken: OAuth2AuthorizedClient? =
            clientService.loadAuthorizedClient<OAuth2AuthorizedClient?>("clientRegistration", "noRefreshTokenClient")
                .block()

        actualToken.shouldNotBeNull()
        actualToken.accessToken.shouldNotBeNull()
        actualToken.accessToken.scopes.shouldBe(setOf("scope"))
        actualToken.accessToken.tokenType.shouldBe(OAuth2AccessToken.TokenType.BEARER)
        actualToken.accessToken.expiresAt.shouldBe(data.accessTokenExpireTime)
        actualToken.accessToken.issuedAt.shouldBe(data.accessTokenIssueTime)
        actualToken.accessToken.tokenValue.shouldBe("accessToken")
        actualToken.clientRegistration.shouldBe(clientRegistration)
        actualToken.principalName.shouldBe("noRefreshTokenClient")
        actualToken.refreshToken.shouldBeNull()
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

        actualClient.shouldNotBeNull()
        actualClient.accessToken.shouldBe("accessToken")
        actualClient.accessTokenExpiresAt.shouldBe(accessTokenExpireTime)
        actualClient.accessTokenIssuedAt.shouldBe(accessTokenIssueTime)
        actualClient.accessTokenScopes.shouldBe(setOf(ClientTokenScope("newScope")))
        actualClient.refreshToken.shouldBe("refreshToken")
        actualClient.refreshTokenIssuedAt.shouldBe(refreshTokenIssueTime)
        actualClient.userName.shouldBe("userName")
        actualClient.clientRegistrationId.shouldBe("clientRegistration")
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

        actualClient.shouldNotBeNull()
        actualClient.accessToken.shouldBe("accessToken")
        actualClient.accessTokenExpiresAt.shouldBe(accessTokenExpireTime)
        actualClient.accessTokenIssuedAt.shouldBe(accessTokenIssueTime)
        actualClient.accessTokenScopes.shouldBe(setOf(ClientTokenScope("newScope")))
        actualClient.refreshToken.shouldBeNull()
        actualClient.refreshTokenIssuedAt.shouldBeNull()
        actualClient.userName.shouldBe("userName")
        actualClient.clientRegistrationId.shouldBe("clientRegistration")
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

        actualClient.shouldNotBeNull()
        actualClient.accessToken.shouldBe("newAccessToken")
        actualClient.accessTokenExpiresAt.shouldBe(accessTokenExpireTime)
        actualClient.accessTokenIssuedAt.shouldBe(accessTokenIssueTime)
        actualClient.accessTokenScopes.shouldBe(setOf(ClientTokenScope("newScope")))
        actualClient.refreshToken.shouldBe("newRefreshToken")
        actualClient.refreshTokenIssuedAt.shouldBe(refreshTokenIssueTime)
        actualClient.userName.shouldBe("fullClient")
        actualClient.clientRegistrationId.shouldBe("clientRegistration")
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

        actualClient.shouldNotBeNull()
        actualClient.accessToken.shouldBe("newAccessToken")
        actualClient.accessTokenExpiresAt.shouldBe(accessTokenExpireTime)
        actualClient.accessTokenIssuedAt.shouldBe(accessTokenIssueTime)
        actualClient.accessTokenScopes.shouldBe(setOf(ClientTokenScope("newScope")))
        actualClient.refreshToken.shouldBe("newRefreshToken")
        actualClient.refreshTokenIssuedAt.shouldBe(refreshTokenIssueTime)
        actualClient.userName.shouldBe("noRefreshTokenClient")
        actualClient.clientRegistrationId.shouldBe("clientRegistration")
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

        actualClient.shouldNotBeNull()
        actualClient.accessToken.shouldBe("newAccessToken")
        actualClient.accessTokenExpiresAt.shouldBe(accessTokenExpireTime)
        actualClient.accessTokenIssuedAt.shouldBe(accessTokenIssueTime)
        actualClient.accessTokenScopes.shouldBe(setOf(ClientTokenScope("newScope")))
        actualClient.refreshToken.shouldBeNull()
        actualClient.refreshTokenIssuedAt.shouldBeNull()
        actualClient.userName.shouldBe("noRefreshTokenClient")
        actualClient.clientRegistrationId.shouldBe("clientRegistration")
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

        actualClient.shouldNotBeNull()
        actualClient.accessToken.shouldBe("newAccessToken")
        actualClient.accessTokenExpiresAt.shouldBe(accessTokenExpireTime)
        actualClient.accessTokenIssuedAt.shouldBe(accessTokenIssueTime)
        actualClient.accessTokenScopes.shouldBe(setOf(ClientTokenScope("newScope")))
        actualClient.refreshToken.shouldBe("refreshToken")
        actualClient.refreshTokenIssuedAt.shouldBe(data.refreshTokenIssueTime)
        actualClient.userName.shouldBe("fullClient")
        actualClient.clientRegistrationId.shouldBe("clientRegistration")
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
