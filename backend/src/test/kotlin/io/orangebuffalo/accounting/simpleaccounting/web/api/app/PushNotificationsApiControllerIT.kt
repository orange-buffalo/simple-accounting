package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Prototypes
import io.orangebuffalo.accounting.simpleaccounting.services.integration.PushNotificationService
import io.orangebuffalo.accounting.simpleaccounting.web.assertNextJsonIs
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.test.StepVerifier
import java.time.Duration

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@DisplayName("Push Notifications API ")
class PushNotificationsApiControllerIT(
    @Autowired val client: WebTestClient,
    @Autowired val pushNotificationService: PushNotificationService
) {

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
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
    @WithMockUser(roles = ["USER"], username = "Fry")
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
    @WithMockUser(roles = ["USER"], username = "Fry")
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
                user = testData.fry, eventName = "watch-tv"
            )

            pushNotificationService.sendPushNotification(
                user = testData.bender, eventName = "kill-all-humans"
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