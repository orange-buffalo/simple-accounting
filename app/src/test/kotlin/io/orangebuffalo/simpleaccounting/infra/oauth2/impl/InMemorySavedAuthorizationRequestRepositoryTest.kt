package io.orangebuffalo.simpleaccounting.infra.oauth2.impl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.infra.oauth2.SavedAuthorizationRequest
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.scheduling.TaskScheduler
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class InMemorySavedAuthorizationRequestRepositoryTest {

    private val expiryScheduler = mock<TaskScheduler>()
    private val repository = InMemorySavedAuthorizationRequestRepository(expiryScheduler)

    @Test
    fun `should remove abandoned request when scheduled expiry runs`() {
        whenever(expiryScheduler.clock) doReturn Clock.fixed(MOCK_TIME, ZoneOffset.UTC)
        val request = authorizationRequest("planet-express-oauth-state")

        repository.save(request)

        val expiryTask = argumentCaptor<Runnable>()
        val expiryTime = argumentCaptor<Instant>()
        verify(expiryScheduler).schedule(expiryTask.capture(), expiryTime.capture())
        expiryTime.firstValue.shouldBe(MOCK_TIME.plus(Duration.ofDays(2)))

        expiryTask.firstValue.run()

        shouldThrow<IllegalStateException> {
            repository.findByStateAndRemove(request.state)
        }
    }

    @Test
    fun `should not remove replacement request when old expiry runs`() {
        whenever(expiryScheduler.clock) doReturn Clock.fixed(MOCK_TIME, ZoneOffset.UTC)
        val firstRequest = authorizationRequest("planet-express-oauth-state")
        val replacementRequest = authorizationRequest("planet-express-oauth-state")

        repository.save(firstRequest)
        repository.save(replacementRequest)

        val expiryTasks = argumentCaptor<Runnable>()
        verify(expiryScheduler, times(2)).schedule(
            expiryTasks.capture(),
            any<Instant>(),
        )
        expiryTasks.allValues.first().run()

        repository.findByStateAndRemove(replacementRequest.state).shouldBe(replacementRequest)
    }

    private fun authorizationRequest(state: String) = SavedAuthorizationRequest(
        owner = PlatformUser(
            userName = "Fry",
            passwordHash = "good-news-everyone",
            isAdmin = false,
            activated = true,
        ),
        state = state,
        clientRegistrationId = "google-drive",
        request = OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://accounts.google.com/o/oauth2/auth")
            .clientId("planet-express-client")
            .redirectUri("https://planet-express.example/oauth-callback")
            .state(state)
            .build(),
    )
}
