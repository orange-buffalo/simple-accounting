package io.orangebuffalo.simpleaccounting.tests.infra.thirdparty

import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import okhttp3.mockwebserver.RecordedRequest
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

object OAuthMocks {
    private val recordedRequests = mutableListOf<RecordedRequest>()

    val mockOAuthServer = MockOAuth2Server()
        .apply {
            start()
            Runtime.getRuntime().addShutdownHook(Thread { shutdown() })
        }

    /**
     * Configures the given provider to return a valid access token. Returns this token
     * for assertions / stubbing.
     */
    fun enqueueAccessToken(
        issuerId: String,
        clientId: String,
    ): String {
        resetCurrentTokensQueue()

        // mock the response on token endpoint
        val callback = DefaultOAuth2TokenCallback(
            issuerId = issuerId,
            subject = "test-subject",
            audience = listOf(clientId),
            // mockOAuth2Server does not have support for real token mocking;
            // hence overriding dynamically generated claims with predefined ones to
            // keep the generated token stable and available for assertions / wiremock stubbing
            claims = mapOf(
                "jti" to "test-jti",
                "nbf" to Instant.now().toEpochMilli(),
                "exp" to Instant.now().toEpochMilli() + 60_000,
                "iat" to Instant.now().toEpochMilli(),
            )
        )
        mockOAuthServer.enqueueCallback(callback)

        // build the same JWT now to provide it to the caller
        val jwt = mockOAuthServer.issueToken(
            issuerId = issuerId,
            clientId = clientId,
            callback,
        )
        return jwt.serialize()
    }

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
     * Resets the recorded requests queue. To be invoked after/before each test method to ensure
     * a clean slate for the next test.
     */
    fun resetRecordedRequests() {
        // deplete the queue so other tests are not affected by current test requests
        collectAllRecordedRequests()
        recordedRequests.clear()
    }

    fun recordedRequests(issuerId: String): List<OAuthRecordedRequest> {
        collectAllRecordedRequests()
        return recordedRequests
            .filter { it.path != null }
            .filter { it.path!!.startsWith("/$issuerId/") }
            .map { request ->
                val path = request.path!!
                when {
                    path.startsWith("/$issuerId/authorize") -> {
                        val url = request.requestUrl!!
                        OAuthRecordedRequest.Authorize(
                            clientId = url.queryParameter("client_id"),
                            scopes = (url.queryParameter("scope") ?: "").split(" ")
                        )
                    }

                    path.startsWith("/$issuerId/token") -> {
                        val auth = request.headers["Authorization"]
                            ?: throw IllegalStateException("Authorization header is missing")
                        if (!auth.startsWith("Basic ")) {
                            throw IllegalStateException("Authorization header is not Basic")
                        }
                        val encoded = auth.substring(6)
                        val decoded = String(Base64.getDecoder().decode(encoded))
                        val parts = decoded.split(":")
                        if (parts.size != 2) {
                            throw IllegalStateException("Invalid Basic authorization header")
                        }
                        OAuthRecordedRequest.Token(
                            clientId = parts[0],
                            clientSecret = parts[1],
                        )
                    }

                    else -> throw IllegalStateException("Unknown request path: ${request.path}")
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
}

sealed interface OAuthRecordedRequest {
    data class Authorize(
        val clientId: String?,
        val scopes: List<String>,
    ) : OAuthRecordedRequest

    data class Token(
        val clientId: String,
        val clientSecret: String,
    ) : OAuthRecordedRequest
}
