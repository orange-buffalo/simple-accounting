package io.orangebuffalo.simpleaccounting.business.api.pushnotifications

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Subscription
import com.fasterxml.jackson.databind.ObjectMapper
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.integration.pushnotifications.PushNotificationService
import io.orangebuffalo.simpleaccounting.business.security.SpringSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.runAs
import io.orangebuffalo.simpleaccounting.infra.graphql.SUBSCRIPTION_AUTHENTICATION_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class PushNotificationsSubscription(
    private val pushNotificationService: PushNotificationService,
    private val objectMapper: ObjectMapper,
) : Subscription {
    @Suppress("unused")
    @GraphQLDescription(
        "Subscribes to push notifications for the current user. " +
                "Returns a stream of push notification messages targeted at the authenticated user " +
                "or broadcast to all users. " +
                "Uses the `graphql-transport-ws` WebSocket subprotocol. " +
                "Clients must provide a JWT token in the `connection_init` payload: " +
                "`{ \"type\": \"connection_init\", \"payload\": { \"token\": \"<JWT>\" } }`. " +
                "Once `connection_ack` is received, subscribe with a standard `subscribe` message."
    )
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_USER)
    fun pushNotifications(env: DataFetchingEnvironment): Flow<PushNotificationMessage> {
        val authentication = env.graphQlContext.get<Authentication?>(SUBSCRIPTION_AUTHENTICATION_KEY)
        val principal = authentication?.principal as? SpringSecurityPrincipal
        val flow = if (principal != null) {
            runBlocking {
                runAs(principal) {
                    pushNotificationService.subscribeToEventsForCurrentUser()
                }
            }
        } else {
            runBlocking {
                pushNotificationService.subscribeToEventsForCurrentUser()
            }
        }
        return flow.map { notification ->
            PushNotificationMessage(
                eventName = notification.eventName,
                data = notification.data?.let { objectMapper.writeValueAsString(it) }
            )
        }
    }
}

@GraphQLDescription("A push notification message.")
data class PushNotificationMessage(
    @GraphQLDescription("The name of the event.")
    val eventName: String,
    @GraphQLDescription("Optional event data payload, serialized as a JSON string.")
    val data: String? = null,
)
