@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.orangebuffalo.accounting.simpleaccounting.services.integration

import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
class PushNotificationService(
    private val platformUserService: PlatformUserService
) {
    suspend fun subscribeToEventsForCurrentUser(): Flow<PushNotificationMessage> = coroutineScope {
        val currentUser = platformUserService.getCurrentUser()
        val subscriber = PushNotificationsSubscriber(currentUser.id!!)

        // subscribe in background to get back to the client as fast as we can
        launch {
            pushNotificationsBus.send(AddSubscriberCommand(subscriber))
        }

        flow {
            logger.trace { "Starting consuming messages for ${subscriber.id}" }

            try {
                subscriber.listeningChannel.consumeEach { pushNotificationMessage ->
                    logger.trace { "Received $pushNotificationMessage in ${subscriber.id}" }

                    emit(pushNotificationMessage)
                }
            } finally {
                logger.trace { "Cancelling subscriber ${subscriber.id}" }

                // unsubscribe and release resources when user cancelled the flow (typically closed the connection)
                pushNotificationsBus.send(RemoveSubscriberCommand(subscriber))
            }
        }
    }

    suspend fun sendPushNotification(
        eventName: String,
        user: PlatformUser? = null,
        data: Any? = null
    ) {
        pushNotificationsBus.send(BroadcastNotificationCommand(PushNotificationMessage(eventName, user?.id, data)))
    }

    /**
     * An actor responsible for managing subscribers and delivering push events to the subscribers
     */
    private companion object PushMessagesOrchestrator {
        private val subscribers = mutableListOf<PushNotificationsSubscriber>()

        private val pushNotificationsBus = GlobalScope.actor<OrchestratorCommand> {
            consumeEach { command ->
                when (command) {
                    is AddSubscriberCommand -> addSubscriber(command)
                    is RemoveSubscriberCommand -> removeSubscriber(command)
                    is BroadcastNotificationCommand -> broadcastNotification(command)
                }
            }
        }

        private fun addSubscriber(command: AddSubscriberCommand) {
            val subscriber = command.subscriber
            subscribers.add(command.subscriber)
            logger.debug { "Registered new subscriber ${subscriber.id} (${subscriber.userId})" }
        }

        private fun removeSubscriber(command: RemoveSubscriberCommand) {
            val subscriber = command.subscriber
            logger.debug { "Removing subscriber ${subscriber.id}" }

            subscriber.listeningChannel.close()
            subscribers.remove(subscriber)
        }

        private suspend fun broadcastNotification(command: BroadcastNotificationCommand) {
            val message = command.message

            logger.debug { "Received broadcast request for $message" }

            subscribers.removeIf { it.listeningChannel.isClosedForSend }

            subscribers.asSequence()
                .filter { subscriber ->
                    message.userId == null || message.userId == subscriber.userId
                }
                .forEach { subscriber ->
                    logger.debug { "Sending message to ${subscriber.id}" }

                    subscriber.listeningChannel.send(message)
                }

            logger.debug { "All subscribers have been notified" }
        }
    }
}

data class PushNotificationMessage(
    val eventName: String,
    val userId: Long?,
    val data: Any?
)

/**
 * A subscriber for push notifications, providing a [listeningChannel] to retrieve events from.
 * The channel is conflated, so that broadcasting is not suspended and all subscribers have a chance
 * to react to the message.
 */
private class PushNotificationsSubscriber(val userId: Long) {

    val listeningChannel = Channel<PushNotificationMessage>(capacity = Channel.CONFLATED)

    val id = UUID.randomUUID().toString()

    override fun hashCode(): Int = id.hashCode()

    override fun equals(other: Any?): Boolean =
        other is PushNotificationsSubscriber && other.id == this.id
}

private sealed class OrchestratorCommand

private class AddSubscriberCommand(val subscriber: PushNotificationsSubscriber) : OrchestratorCommand()
private class RemoveSubscriberCommand(val subscriber: PushNotificationsSubscriber) : OrchestratorCommand()
private class BroadcastNotificationCommand(val message: PushNotificationMessage) : OrchestratorCommand()
