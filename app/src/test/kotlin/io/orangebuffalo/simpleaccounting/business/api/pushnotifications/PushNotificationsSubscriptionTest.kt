package io.orangebuffalo.simpleaccounting.business.api.pushnotifications

import io.github.artsok.RepeatedIfExceptionsTest
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.integration.pushnotifications.PushNotificationService
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.time.Duration
import java.util.concurrent.CompletionStage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@DisplayName("Push Notifications Subscription")
class PushNotificationsSubscriptionTest(
    @Autowired private val pushNotificationService: PushNotificationService,
    @Autowired private val environment: Environment,
    @Autowired private val objectMapper: ObjectMapper,
) : SaIntegrationTestBase() {

    private val asyncTimeout: Duration = Duration.ofSeconds(15)

    @Test
    fun `should receive a single broadcast event`() {
        val subscription = subscribeToNotifications(preconditions.fry)

        try {
            pushNotificationService.sendPushNotification("good-news-everyone")

            await().atMost(asyncTimeout).untilAsserted {
                val receivedMessages = subscription.getReceivedMessages()
                withClue("Should receive the broadcast event") {
                    receivedMessages.size.shouldBe(1)
                    val message = objectMapper.readTree(receivedMessages[0])
                    message.payloadEventName().shouldBe("good-news-everyone")
                    message.payloadData().shouldBe(null)
                }
            }
        } finally {
            subscription.dispose()
        }
    }

    @RepeatedIfExceptionsTest(repeats = 3)
    fun `should receive multiple broadcast events`() {
        val subscription = subscribeToNotifications(preconditions.fry)

        try {
            pushNotificationService.sendPushNotification(
                eventName = "good-news-everyone", data = "deadly delivery"
            )
            pushNotificationService.sendPushNotification(
                eventName = "good-news-everyone", data = "all fired"
            )

            await().atMost(asyncTimeout).untilAsserted {
                val receivedMessages = subscription.getReceivedMessages()
                withClue("Should receive both broadcast events") {
                    receivedMessages.size.shouldBe(2)
                    val first = objectMapper.readTree(receivedMessages[0])
                    first.payloadEventName().shouldBe("good-news-everyone")
                    first.payloadData().shouldBe("\"deadly delivery\"")

                    val second = objectMapper.readTree(receivedMessages[1])
                    second.payloadEventName().shouldBe("good-news-everyone")
                    second.payloadData().shouldBe("\"all fired\"")
                }
            }
        } finally {
            subscription.dispose()
        }
    }

    @RepeatedIfExceptionsTest(repeats = 3)
    fun `should not receive events addressed to another user`() {
        val subscription = subscribeToNotifications(preconditions.fry)

        try {
            pushNotificationService.sendPushNotification(
                eventName = "good-news-everyone", data = "deadly delivery"
            )
            pushNotificationService.sendPushNotification(
                userId = preconditions.fry.id!!, eventName = "watch-tv"
            )
            pushNotificationService.sendPushNotification(
                userId = preconditions.bender.id!!, eventName = "kill-all-humans"
            )
            pushNotificationService.sendPushNotification(
                eventName = "end-of-season"
            )

            await().atMost(asyncTimeout).untilAsserted {
                val receivedMessages = subscription.getReceivedMessages()
                withClue("Should receive only messages for the current user and broadcasts") {
                    receivedMessages.size.shouldBe(3)
                    objectMapper.readTree(receivedMessages[0]).payloadEventName()
                        .shouldBe("good-news-everyone")
                    objectMapper.readTree(receivedMessages[1]).payloadEventName()
                        .shouldBe("watch-tv")
                    objectMapper.readTree(receivedMessages[2]).payloadEventName()
                        .shouldBe("end-of-season")
                }
            }
        } finally {
            subscription.dispose()
        }
    }

    private fun subscribeToNotifications(user: PlatformUser): NotificationsSubscription {
        val receivedMessages = CopyOnWriteArrayList<String>()
        val connectionAcknowledged = AtomicBoolean(false)
        val probeReceived = AtomicBoolean(false)
        val lastProbeSentAtMillis = AtomicLong(0L)
        val connectionError = AtomicReference<Throwable>()
        val port = environment.getProperty("local.server.port")
        val wsUri = URI("ws://localhost:$port/api/graphql/subscriptions")
        val jwtToken = jwtService.buildJwtToken(user.toSecurityPrincipal())
        val probeEventName = "probe-${System.nanoTime()}"
        val connectionInit = objectMapper.writeValueAsString(
            mapOf(
                "type" to "connection_init",
                "payload" to mapOf("token" to jwtToken)
            )
        )
        val subscribe = objectMapper.writeValueAsString(
            mapOf(
                "id" to "1",
                "type" to "subscribe",
                "payload" to mapOf(
                    "query" to "subscription { pushNotifications { eventName data } }"
                )
            )
        )
        val messageBuffer = StringBuilder()
        val listener = object : WebSocket.Listener {
            override fun onOpen(webSocket: WebSocket) {
                webSocket.request(1)
                webSocket.sendText(connectionInit, true)
            }

            override fun onText(
                webSocket: WebSocket,
                data: CharSequence,
                last: Boolean,
            ): CompletionStage<*>? {
                messageBuffer.append(data)
                if (last) {
                    val text = messageBuffer.toString()
                    messageBuffer.setLength(0)
                    val json = objectMapper.readTree(text)
                    when (json.get("type")?.asText()) {
                        "connection_ack" -> {
                            connectionAcknowledged.set(true)
                            webSocket.sendText(subscribe, true)
                        }
                        "next" -> {
                            if (json.payloadEventName() == probeEventName) {
                                probeReceived.set(true)
                            } else {
                                receivedMessages.add(text)
                            }
                        }
                    }
                }
                webSocket.request(1)
                return null
            }

            override fun onError(webSocket: WebSocket, error: Throwable) {
                connectionError.set(error)
            }
        }
        val webSocketSession = HttpClient.newHttpClient()
            .newWebSocketBuilder()
            .subprotocols("graphql-transport-ws")
            .buildAsync(wsUri, listener)
            .join()

        await().atMost(asyncTimeout).untilAsserted {
            connectionError.get()?.let { throw AssertionError("WebSocket subscription failed", it) }
            connectionAcknowledged.get().shouldBe(true)
        }

        await().atMost(asyncTimeout).until {
            val now = System.currentTimeMillis()
            if (now - lastProbeSentAtMillis.get() >= 250) {
                pushNotificationService.sendPushNotification(
                    userId = user.id!!,
                    eventName = probeEventName,
                )
                lastProbeSentAtMillis.set(now)
            }
            probeReceived.get()
        }

        return NotificationsSubscription(
            receivedMessages = receivedMessages,
            webSocketSession = webSocketSession,
        )
    }

    private fun JsonNode.payloadEventName(): String? =
        this.path("payload").path("data").path("pushNotifications").path("eventName").asText(null)

    private fun JsonNode.payloadData(): String? {
        val dataNode = this.path("payload").path("data").path("pushNotifications").path("data")
        return if (dataNode.isMissingNode || dataNode.isNull) null else dataNode.asText()
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val bender = bender()
        }
    }

    private data class NotificationsSubscription(
        private val receivedMessages: CopyOnWriteArrayList<String>,
        private val webSocketSession: WebSocket,
    ) {
        fun getReceivedMessages(): List<String> = receivedMessages.toList()

        fun dispose() {
            webSocketSession.abort()
        }
    }
}
