package io.orangebuffalo.simpleaccounting.business.api.pushnotifications

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.integration.pushnotifications.PushNotificationService
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import kotlinx.coroutines.runBlocking
import io.github.artsok.RepeatedIfExceptionsTest
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@DisplayName("Push Notifications Subscription")
class PushNotificationsSubscriptionTest(
    @Autowired private val pushNotificationService: PushNotificationService,
    @Autowired private val jwtService: JwtService,
    @Autowired private val environment: Environment,
    @Autowired private val objectMapper: ObjectMapper,
) : SaIntegrationTestBase() {

    @Test
    fun `should receive a single broadcast event`() {
        val subscription = subscribeToNotifications(preconditions.fry)

        try {
            runBlocking {
                pushNotificationService.sendPushNotification("good-news-everyone")
            }

            await().atMost(Duration.ofSeconds(5)).untilAsserted {
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
            runBlocking {
                pushNotificationService.sendPushNotification(
                    eventName = "good-news-everyone", data = "deadly delivery"
                )
                pushNotificationService.sendPushNotification(
                    eventName = "good-news-everyone", data = "all fired"
                )
            }

            await().atMost(Duration.ofSeconds(5)).untilAsserted {
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

    @Test
    fun `should not receive events addressed to another user`() {
        val subscription = subscribeToNotifications(preconditions.fry)

        try {
            runBlocking {
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
            }

            await().atMost(Duration.ofSeconds(5)).untilAsserted {
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
        val port = environment.getProperty("local.server.port")
        val wsUri = URI("ws://localhost:$port/api/graphql/subscriptions")
        val jwtToken = jwtService.buildJwtToken(user.toSecurityPrincipal())
        val probeEventName = "probe-${System.nanoTime()}"

        val client = ReactorNettyWebSocketClient(
            reactor.netty.http.client.HttpClient.create()
        ) {
            reactor.netty.http.client.WebsocketClientSpec.builder()
                .protocols("graphql-transport-ws")
        }

        val outgoingSink = Sinks.many().unicast().onBackpressureBuffer<String>()

        val webSocketSession = client.execute(wsUri) { session ->
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

            outgoingSink.tryEmitNext(connectionInit)

            val outgoing = outgoingSink.asFlux().map { session.textMessage(it) }

            val incoming = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext { text ->
                    val json = objectMapper.readTree(text)
                    when (json.get("type")?.asText()) {
                        "connection_ack" -> {
                            connectionAcknowledged.set(true)
                            outgoingSink.tryEmitNext(subscribe)
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
                .then()

            Mono.zip(session.send(outgoing), incoming).then()
        }.subscribe()

        await().atMost(Duration.of(5, ChronoUnit.SECONDS)).until {
            connectionAcknowledged.get()
        }

        await().atMost(Duration.of(5, ChronoUnit.SECONDS)).until {
            val now = System.currentTimeMillis()
            if (now - lastProbeSentAtMillis.get() >= 250) {
                runBlocking {
                    pushNotificationService.sendPushNotification(
                        userId = user.id!!,
                        eventName = probeEventName,
                    )
                }
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
        private val webSocketSession: Disposable,
    ) {
        fun getReceivedMessages(): List<String> = receivedMessages.toList()

        fun dispose() {
            webSocketSession.dispose()
        }
    }
}
