package io.orangebuffalo.accounting.simpleaccounting.services.integration

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class PushNotificationService(
    private val platformUserService: PlatformUserService
) {

    // we have one consumer, give it some buffer to marshall the messages
    private val messages: Channel<PushNotificationMessage> = Channel(500)

    // publish here so all subscribers receive all events (channel does fan-out)
    private val messagesFlux = messages.asFlux().publish(1)

    fun getCurrentUserMessages(): Flux<CurrentUserPushNotificationMessage> = ReactiveSecurityContextHolder.getContext()
        .map { it.authentication.principal }
        .cast(UserDetails::class.java)
        .flatMap { principal ->
            mono {
                platformUserService.getUserByUserName(principal.username)
            }
        }
        .flatMapMany { currentUser ->
            messagesFlux.autoConnect()
                .filter { message ->
                    message.userId == null || currentUser.id == message.userId
                }
                .map { message ->
                    CurrentUserPushNotificationMessage(eventName = message.eventName, data = message.data)
                }
        }

    suspend fun sendPushNotification(
        eventName: String,
        user: PlatformUser? = null,
        data: Any? = null
    ) {
        messages.send(PushNotificationMessage(eventName, user?.id, data))
    }
}

private class PushNotificationMessage(
    val eventName: String,
    val userId: Long?,
    val data: Any?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CurrentUserPushNotificationMessage(
    val eventName: String,
    val data: Any?
)