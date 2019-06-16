package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.integration.CurrentUserPushNotificationMessage
import io.orangebuffalo.accounting.simpleaccounting.services.integration.PushNotificationService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/push-notifications")
class PushNotificationsController(
    private val pushNotificationService: PushNotificationService
) {

    @GetMapping(produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getPushNotificationMessages(): Flux<CurrentUserPushNotificationMessage> = pushNotificationService.getCurrentUserMessages()

}
