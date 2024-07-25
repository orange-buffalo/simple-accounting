@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.orangebuffalo.simpleaccounting.business.integration.pushnotifications

import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
class PushNotificationService(
    private val platformUsersService: PlatformUsersService
) {
    private val notificationsFlow = MutableSharedFlow<PushNotificationMessage>(
        extraBufferCapacity = 500,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    suspend fun subscribeToEventsForCurrentUser(): Flow<PushNotificationMessage> {
        val currentUserId = platformUsersService.getCurrentUser().id
        val subscriberId = UUID.randomUUID().toString()
        logger.trace { "Subscribing $subscriberId (user: $currentUserId)" }
        return notificationsFlow
            .filter { message ->
                message.userId == null || message.userId == currentUserId
            }
            .onEach { message ->
                logger.trace { "Received $message in $subscriberId" }
            }
    }

    suspend fun sendPushNotification(
        eventName: String,
        userId: Long? = null,
        data: Any? = null
    ) {
        notificationsFlow.emit(PushNotificationMessage(eventName, userId, data))
    }

    suspend fun getActiveSubscribersCount() = notificationsFlow.subscriptionCount.first()
}

data class PushNotificationMessage(
    val eventName: String,
    val userId: Long?,
    val data: Any?
)
