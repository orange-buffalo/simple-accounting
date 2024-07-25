package io.orangebuffalo.simpleaccounting.business.integration.pushnotifications

import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/push-notifications")
class PushNotificationsApi(
    private val pushNotificationService: PushNotificationService
) {

    @GetMapping(produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    suspend fun getPushNotificationMessages(): Flow<CurrentUserPushNotificationMessage> = pushNotificationService
        .subscribeToEventsForCurrentUser()
        .map { pushNotificationMessage ->
            CurrentUserPushNotificationMessage(
                eventName = pushNotificationMessage.eventName,
                data = pushNotificationMessage.data
            )
        }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CurrentUserPushNotificationMessage(
    val eventName: String,
    val data: Any?
)
