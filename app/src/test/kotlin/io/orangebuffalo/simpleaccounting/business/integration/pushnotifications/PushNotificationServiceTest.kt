package io.orangebuffalo.simpleaccounting.business.integration.pushnotifications

import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.test.StepVerifier

class PushNotificationServiceTest {

    private val fry = PlatformUser(
        userName = "Fry",
        passwordHash = "good-news-everyone",
        isAdmin = false,
        activated = true,
        id = "fry-id",
    )
    private val platformUsersRepository = mock<PlatformUsersRepository>()
    private val service = PushNotificationService(platformUsersRepository)

    @Test
    fun `should retain newest notifications for a slow subscriber`() {
        whenever(platformUsersRepository.findByUserName(fry.userName)) doReturn fry

        StepVerifier.create(service.subscribeToEventsForUser(fry.userName), 0)
            .then {
                repeat(501) { index ->
                    service.sendPushNotification(
                        eventName = "delivery-$index",
                        userId = fry.id,
                    )
                }
            }
            .thenRequest(500)
            .expectNextSequence(
                (1..500).map { index ->
                    PushNotificationMessage(
                        eventName = "delivery-$index",
                        userId = fry.id,
                        data = null,
                    )
                }
            )
            .thenCancel()
            .verify()
    }
}
