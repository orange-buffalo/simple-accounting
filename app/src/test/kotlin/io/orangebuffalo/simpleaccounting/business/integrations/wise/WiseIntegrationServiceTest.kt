package io.orangebuffalo.simpleaccounting.business.integrations.wise

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@WireMockTest
class WiseIntegrationServiceTest {
    private lateinit var service: WiseIntegrationService
    private lateinit var workspaces: WorkspacesService
    private val workspaceId = "bender"
    private val token = "bender-personal-token"

    @BeforeEach
    fun setup(runtime: WireMockRuntimeInfo) {
        workspaces = mock(WorkspacesService::class.java)
        service = WiseIntegrationService(workspaces, mock(WiseIntegrationSettingsRepository::class.java),
            WiseApiClient(WiseProperties(apiBaseUrl = runtime.httpBaseUrl)))
    }

    @Test
    fun `should preserve long identifiers and ignore additional provider fields`() {
        stubFor(get(urlEqualTo("/2026Q4/profiles"))
            .withHeader("Authorization", equalTo("Bearer $token"))
            .willReturn(okJson("""[{"id":9007199254740993,"fullName":"Bender","type":"PERSONAL"}]""")))
        stubFor(get(urlEqualTo("/2026Q4/profiles/9007199254740993/balances?types=STANDARD,SAVINGS"))
            .withHeader("Authorization", equalTo("Bearer $token"))
            .willReturn(okJson("""[{"id":9007199254740995,"currency":"USD","type":"STANDARD","amount":{"value":42}}]""")))

        service.verifyToken(workspaceId, token).shouldBe(listOf(
            WiseAccount(9007199254740993, "Bender", 9007199254740995, "USD", null, "STANDARD"),
        ))
        verify(workspaces).validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.ADMIN)
    }

    @Test
    fun `should sanitize rejected token and upstream error bodies`() {
        for (status in listOf(401, 403, 429, 503)) {
            stubFor(get(urlEqualTo("/2026Q4/profiles")).willReturn(aResponse().withStatus(status).withBody(token)))
            val exception = if (status == 401 || status == 403) {
                shouldThrow<WiseInvalidTokenException> { service.verifyToken(workspaceId, token) }
                    .also { it.message.shouldBe("Wise rejected the token") }
            } else {
                shouldThrow<WiseUnavailableException> { service.verifyToken(workspaceId, token) }
                    .also { it.message.shouldBe("Wise accounts could not be loaded") }
            }
            exception.cause.shouldBe(null)
        }
    }

    @Test
    fun `should treat malformed responses as unavailable without retaining the payload`() {
        stubFor(get(urlEqualTo("/2026Q4/profiles")).willReturn(okJson("""{"unexpected":"$token"}""")))
        val exception = shouldThrow<WiseUnavailableException> { service.verifyToken(workspaceId, token) }
        exception.message.shouldBe("Wise accounts could not be loaded")
        exception.cause.shouldBe(null)
    }
}
