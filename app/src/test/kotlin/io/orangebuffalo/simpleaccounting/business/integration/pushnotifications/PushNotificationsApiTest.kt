package io.orangebuffalo.simpleaccounting.business.integration.pushnotifications

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.assertNextJsonIs
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFryUser
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.test.StepVerifier
import java.time.Duration
import java.time.temporal.ChronoUnit

@DisplayName("Push Notifications API ")
class PushNotificationsApiTest(
    @Autowired private val client: WebTestClient,
    @Autowired private val pushNotificationService: PushNotificationService,
) : SaIntegrationTestBase() {

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    @WithMockFryUser
    fun `should receive a single broadcast event`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry
        val result = GlobalScope.async {
            client.get()
                .uri("/api/push-notifications")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk
                .returnResult(String::class.java)
        }
        waitUntilSubscribed()

        runBlocking {
            pushNotificationService.sendPushNotification("good-news-everyone")

            StepVerifier.create(result.await().responseBody)
                .assertNextJsonIs(
                    """{
                       "eventName": "good-news-everyone"
                    }"""
                )
                .expectNoEvent(Duration.ofSeconds(1))
                .thenCancel()
                .verify(Duration.ofSeconds(5))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    @WithMockFryUser
    fun `should receive multiple broadcast events`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry
        val result = GlobalScope.async {
            client.get()
                .uri("/api/push-notifications")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk
                .returnResult(String::class.java)
        }
        waitUntilSubscribed()

        runBlocking {
            pushNotificationService.sendPushNotification(
                eventName = "good-news-everyone", data = "deadly delivery"
            )

            pushNotificationService.sendPushNotification(
                eventName = "good-news-everyone", data = "all fired"
            )

            StepVerifier.create(result.await().responseBody)
                .assertNextJsonIs(
                    """{
                       "eventName": "good-news-everyone",
                       "data": "deadly delivery"
                    }"""
                )
                .assertNextJsonIs(
                    """{
                       "eventName": "good-news-everyone",
                       "data": "all fired"
                    }"""
                )
                .expectNoEvent(Duration.ofSeconds(1))
                .thenCancel()
                .verify(Duration.ofSeconds(5))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    @WithMockFryUser
    fun `should not receive events addressed to another user`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry

        val result = GlobalScope.async {
            client.get()
                .uri("/api/push-notifications")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk
                .returnResult(String::class.java)
        }
        waitUntilSubscribed()

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

            StepVerifier.create(result.await().responseBody)
                .assertNextJsonIs(
                    """{
                       "eventName": "good-news-everyone",
                       "data": "deadly delivery"
                    }"""
                )
                .assertNextJsonIs(
                    """{
                       "eventName": "watch-tv"
                    }"""
                )
                .assertNextJsonIs(
                    """{
                       "eventName": "end-of-season"
                    }"""
                )
                .expectNoEvent(Duration.ofSeconds(1))
                .thenCancel()
                .verify(Duration.ofSeconds(5))
        }
    }

    private fun waitUntilSubscribed() {
        await().atMost(Duration.of(5, ChronoUnit.SECONDS)).until {
            runBlocking { pushNotificationService.getActiveSubscribersCount() } == 1
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val bender = bender()
        }
    }
}
