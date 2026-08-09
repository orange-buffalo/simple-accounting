package io.orangebuffalo.simpleaccounting.business.integration.pushnotifications

import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
class PushNotificationService(
    private val platformUsersRepository: PlatformUsersRepository,
) {
    private val notificationsSink = Sinks.many().multicast().onBackpressureBuffer<PushNotificationMessage>(500, false)
    private val emissionLock = Any()

    fun subscribeToEventsForUser(userName: String): Flux<PushNotificationMessage> {
        val currentUserId = platformUsersRepository.findByUserName(userName)?.id
            ?: throw IllegalStateException("User $userName is not found")
        val subscriberId = UUID.randomUUID().toString()
        logger.trace { "Subscribing $subscriberId (user: $currentUserId)" }
        return notificationsSink.asFlux()
            .filter { message ->
                message.userId == null || message.userId == currentUserId
            }
            .doOnNext { message ->
                logger.trace { "Received $message in $subscriberId" }
            }
    }

    fun sendPushNotification(
        eventName: String,
        userId: String? = null,
        data: Any? = null
    ) {
        val result = synchronized(emissionLock) {
            notificationsSink.tryEmitNext(PushNotificationMessage(eventName, userId, data))
        }
        if (result.isFailure && result != Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
            logger.warn { "Push notification was dropped: $result" }
        }
    }

    fun getActiveSubscribersCount() = notificationsSink.currentSubscriberCount()
}

data class PushNotificationMessage(
    val eventName: String,
    val userId: String?,
    val data: Any?
)
