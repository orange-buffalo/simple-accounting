package io.orangebuffalo.simpleaccounting.domain.users

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.infra.utils.mockCurrentTime
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.web.api.UserApiTestData
import net.javacrumbs.jsonunit.assertj.JsonAssertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.test.web.reactive.server.WebTestClient

@SimpleAccountingIntegrationTest
@DisplayName("Admin User API ")
internal class UsersApiControllerIT(
    @Autowired val client: WebTestClient,
    @Autowired val aggregateTemplate: JdbcAggregateTemplate,
    @Autowired val timeService: TimeService,
) {

    @Test
    @WithMockFryUser
    fun `should allow access only for Admin to read users`() {
        client.get()
            .uri("/api/users")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    @WithMockFryUser
    fun `should allow access only for Admin to create users`() {
        client.post()
            .uri("/api/users")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return a valid users page`(testData: UserApiTestData) {
        client.get()
            .uri("/api/users")
            .verifyOkAndJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("3")

                inPath("$.data").isArray.containsExactly(
                    JsonAssertions.json(
                        """{
                        userName: "Farnsworth",
                        id: ${testData.farnsworth.id},
                        version: 0,
                        admin: true,
                        activated: true
                    }"""
                    ),
                    JsonAssertions.json(
                        """{
                        userName: "Fry",
                        id: ${testData.fry.id},
                        version: 0,
                        admin: false,
                        activated: true
                    }"""
                    ),
                    JsonAssertions.json(
                        """{
                        userName: "Zoidberg",
                        id: ${testData.zoidberg.id},
                        version: 0,
                        admin: false,
                        activated: false
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockFarnsworthUser
    fun `should create a new user`(testData: UserApiTestData) {
        mockCurrentTime(timeService)

        client.post()
            .uri("/api/users")
            .sendJson(
                """{
                    "userName": "Leela",
                    "admin": false
                }"""
            )
            .verifyOkAndJsonBody {
                isEqualTo(
                    JsonAssertions.json(
                        """{
                        userName: "Leela",
                        id: "#{json-unit.any-number}",
                        version: 0,
                        admin: false,
                        activated: false
                    }"""
                    )
                )
            }

        val createdUserId = aggregateTemplate.findAll(PlatformUser::class.java)
            .filter { it.userName == "Leela"}
            .shouldHaveSize(1)
            .first()
            .id

        aggregateTemplate.findAll(UserActivationToken::class.java)
            .shouldHaveSize(1)
            .first()
            .should {
                it.userId.shouldBe(createdUserId)
                it.token.shouldNotBeNull()
                it.expiresAt.shouldBeEqualComparingTo(MOCK_TIME.plusSeconds(72 * 3600))
            }
    }
}
