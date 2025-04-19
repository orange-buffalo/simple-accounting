package io.orangebuffalo.simpleaccounting.tests.infra.thirdparty

import io.kotest.assertions.fail
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.infra.oauth2.impl.ClientTokenScope
import io.orangebuffalo.simpleaccounting.infra.oauth2.impl.PersistentOAuth2AuthorizedClient
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.context.ApplicationContext
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

// spring oauth has clock skew of 1 minute, so expiration must be greater than that
private const val TOKEN_EXPIRY_SECONDS = 300L

object OAuthMocks {
    private val recordedRequests = mutableListOf<RecordedRequest>()

    // Stable time is required as mockOAuthServer requires two actions to queue token as response
    // and generate actual token value. Without fixing the point in time, those two actions might
    // be executed in different instants, which will cause mismatching tokens
    private var mockServerTime: Instant? = null
    private val mockServerTimeProvider = {
        mockServerTime ?: Instant.now()
    }

    // exists only during a test execution
    private var applicationContext: ApplicationContext? = null

    private val mockOAuthServer = MockOAuth2Server(
        config = OAuth2Config(tokenProvider = OAuth2TokenProvider(timeProvider = mockServerTimeProvider))
    ).apply {
        start()
        Runtime.getRuntime().addShutdownHook(Thread { shutdown() })
    }

    fun issuerUrl(issuerId: String) = mockOAuthServer.issuerUrl(issuerId)

    // workaround - MockOAuth2Server does not provide API to clear the queue
    private fun resetCurrentTokensQueue() {
        val handlerField = MockOAuth2Server::class.java.getDeclaredField("defaultRequestHandler")
        handlerField.isAccessible = true
        val handler = handlerField.get(mockOAuthServer)

        val queueField = handler.javaClass.getDeclaredField("tokenCallbackQueue")
        queueField.isAccessible = true
        val queue = queueField.get(handler)

        val clearMethod = queue.javaClass.getMethod("clear")
        clearMethod.invoke(queue)
    }

    /**
     * Cleans up and sets up the state before each test.
     */
    fun beforeTest(applicationContext: ApplicationContext) {
        // deplete the queue so other tests are not affected by current test requests
        collectAllRecordedRequests()
        recordedRequests.clear()
        // mocking time required for stable tokens generation
        mockServerTime = Instant.now()
        this.applicationContext = applicationContext
    }

    fun afterTest() {
        mockServerTime = null
        applicationContext = null
        recordedRequests.clear()
    }

    private fun recordedRequests(
        issuerId: String,
        expectedClientId: String,
        expectedClientSecret: String,
        expectedScopes: List<String>,
    ): List<OAuthRecordedRequest> {
        collectAllRecordedRequests()
        return recordedRequests
            .filter { it.path != null }
            .filter { it.path!!.startsWith("/$issuerId/") }
            .map { request ->
                val path = request.path!!
                when {
                    path.startsWith("/$issuerId/authorize") -> {
                        val url = request.requestUrl!!
                        withClue("Should provide proper authorization code flow parameters") {
                            url.queryParameter("client_id").shouldBe(expectedClientId)
                            (url.queryParameter("scope") ?: "").split(" ")
                                .shouldContainExactlyInAnyOrder(expectedScopes)
                        }
                        OAuthRecordedRequest.Authorize
                    }

                    path.startsWith("/$issuerId/token") -> {
                        withClue("Should invoke token endpoint with proper credentials") {
                            val auth = request.headers["Authorization"].shouldNotBeNull()
                            auth.shouldStartWith("Basic ")
                            val encoded = auth.substring(6)
                            val decoded = String(Base64.getDecoder().decode(encoded))
                            decoded.split(":").shouldContainExactly(expectedClientId, expectedClientSecret)
                        }
                        OAuthRecordedRequest.Token
                    }

                    else -> fail("Unknown request path: ${request.path}")
                }
            }
    }

    private fun collectAllRecordedRequests() {
        var recordedRequest: RecordedRequest?
        do {
            recordedRequest = try {
                mockOAuthServer.takeRequest(timeout = 0, unit = TimeUnit.MILLISECONDS)
            } catch (e: RuntimeException) {
                // mockOAuthServer does not support nullable return result, they throw an exception
                null
            }
            if (recordedRequest != null) {
                recordedRequests.add(recordedRequest)
            }
        } while (recordedRequest != null)
    }

    fun provider(
        issuerId: String,
        clientId: String,
        clientSecret: String,
        scopes: List<String>,
        clientRegistrationId: String,
    ): OAuthMocksProvider = object : OAuthMocksProvider {
        override fun recordedRequests(): List<OAuthRecordedRequest> = recordedRequests(
            issuerId = issuerId,
            expectedClientId = clientId,
            expectedClientSecret = clientSecret,
            expectedScopes = scopes,
        )

        override fun token(): OAuthMocksToken = OAuthMocksTokenImpl(
            issuerId = issuerId,
            clientId = clientId,
            clientRegistrationId = clientRegistrationId,
            scopes = scopes,
        )
    }

    private class OAuthMocksTokenImpl(
        issuerId: String,
        clientId: String,
        private val clientRegistrationId: String,
        private val scopes: List<String>,
    ) : OAuthMocksToken {
        val issuedAt: Instant = mockServerTimeProvider()
        val expiresAt: Instant = issuedAt.plusSeconds(TOKEN_EXPIRY_SECONDS)

        private val mockServerCallback = DefaultOAuth2TokenCallback(
            issuerId = issuerId,
            subject = "test-subject",
            audience = listOf(clientId),
            // mockOAuth2Server generates unique token ID on each invocation of the callback,
            // which makes it impossible to make stable mocks;
            // hence we override dynamically generated ID for testing purposes
            claims = mapOf(
                "jti" to "test-jti",
            ),
            expiry = TOKEN_EXPIRY_SECONDS,
        )

        override val tokenValue: String = mockOAuthServer
            .issueToken(
                issuerId = issuerId,
                clientId = clientId,
                mockServerCallback,
            ).serialize()

        override fun enqueue(
            expectedRequestsCount: Int,
        ): OAuthMocksToken {
            resetCurrentTokensQueue()
            for (i in 0 until expectedRequestsCount) {
                mockOAuthServer.enqueueCallback(mockServerCallback)
            }
            return this
        }

        override fun persist(user: PlatformUser, expired: Boolean): OAuthMocksToken {
            val aggregateTemplate = applicationContext
                .shouldWithClue("This method can only be invoked within the test method execution") {
                    shouldNotBeNull()
                }!!.getBean(JdbcAggregateTemplate::class.java)

            val now = Instant.now()
            aggregateTemplate.insert(
                PersistentOAuth2AuthorizedClient(
                    clientRegistrationId = clientRegistrationId,
                    userName = user.userName,
                    accessToken = tokenValue,
                    accessTokenIssuedAt = if (expired) now.minusSeconds(100) else now,
                    accessTokenExpiresAt = if (expired) now.minusSeconds(10) else expiresAt,
                    accessTokenScopes = scopes.map { ClientTokenScope(it) }.toSet(),
                    refreshToken = null,
                    refreshTokenIssuedAt = null,
                )
            )
            return this
        }
    }
}

interface OAuthMocksProvider {
    fun recordedRequests(): List<OAuthRecordedRequest>
    fun token(): OAuthMocksToken
}

interface OAuthMocksToken {
    /**
     * The value of the JWT access token.
     */
    val tokenValue: String

    /**
     * Enqueues the token to be returned by the mock server.
     */
    fun enqueue(
        expectedRequestsCount: Int = 1,
    ): OAuthMocksToken

    /**
     * Persists the token in the database (as if was obtained by the application previously).
     */
    fun persist(
        /**
         * Owner of the token.
         */
        user: PlatformUser,
        /**
         * Marks this token as expired in the database.
         */
        expired: Boolean = false,
    ): OAuthMocksToken
}

sealed interface OAuthRecordedRequest {
    data object Authorize : OAuthRecordedRequest
    data object Token : OAuthRecordedRequest
}
