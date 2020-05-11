package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.WithMockFryUser
import io.orangebuffalo.simpleaccounting.assertNextJsonIs
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.junit.TestData
import io.orangebuffalo.simpleaccounting.services.integration.PushNotificationService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.test.StepVerifier
import java.time.Duration

@SimpleAccountingIntegrationTest
@DisplayName("Push Notifications API ")
class PushNotificationsApiControllerIT(
    @Autowired val client: WebTestClient,
    @Autowired val pushNotificationService: PushNotificationService
) {

    @Test
    @WithMockFryUser
    fun `should receive a single broadcast event`(testData: PushNotificationsApiTestData) {
        val result = GlobalScope.async {
            client.get()
                .uri("/api/push-notifications")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk
                .returnResult(String::class.java)
        }

        runBlocking {
            //todo #94: check deeper if we can get an event that headers are consumed and start pushing events after that
            delay(1000)

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

    @Test
    @WithMockFryUser
    fun `should receive multiple broadcast events`(testData: PushNotificationsApiTestData) {
        val result = GlobalScope.async {
            client.get()
                .uri("/api/push-notifications")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk
                .returnResult(String::class.java)
        }

        runBlocking {
            //todo #94: check deeper if we can get an event that headers are consumed and start pushing events after that
            delay(1000)

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

    @Test
    @WithMockFryUser
    fun `should not receive events addressed to another user`(testData: PushNotificationsApiTestData) {
        val result = GlobalScope.async {
            client.get()
                .uri("/api/push-notifications")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk
                .returnResult(String::class.java)
        }

        runBlocking {
            //todo #94: check deeper if we can get an event that headers are consumed and start pushing events after that
            delay(1000)

            pushNotificationService.sendPushNotification(
                eventName = "good-news-everyone", data = "deadly delivery"
            )

            pushNotificationService.sendPushNotification(
                userId = testData.fry.id!!, eventName = "watch-tv"
            )

            pushNotificationService.sendPushNotification(
                userId = testData.bender.id!!, eventName = "kill-all-humans"
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

    class PushNotificationsApiTestData : TestData {
        val fry = Prototypes.fry()
        val bender = Prototypes.bender()

        override fun generateData() = listOf(fry, bender)
    }
}
