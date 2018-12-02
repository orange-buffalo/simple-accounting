package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Bender
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Farnsworth
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Fry
import io.orangebuffalo.accounting.simpleaccounting.services.business.TimeService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.ExpenseRepository
import io.orangebuffalo.accounting.simpleaccounting.web.*
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Expense API ")
internal class ExpenseApiControllerIT(
    @Autowired val client: WebTestClient,
    @Autowired val dbHelper: DbHelper,
    @Autowired val expenseRepository: ExpenseRepository
) {

    @MockBean
    lateinit var timeService: TimeService

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should create a new expense`(fry: Fry) {
        val expenseId = dbHelper.getNextId()
        mockCurrentTime(timeService)

        client.post()
            .uri("/api/v1/user/workspaces/${fry.workspace.id}/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "category": ${fry.slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "AUD",
                    "originalAmount": 30000,
                    "amountInDefaultCurrency": 42000,
                    "actualAmountInDefaultCurrency": 41500,
                    "attachments": [${fry.slurmReceipt.id}],
                    "notes": "coffee",
                    "percentOnBusiness": 100,
                    "datePaid": "$MOCK_DATE_VALUE"
                }"""
            )
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                isEqualTo(
                    json(
                        """{
                            category: ${fry.slurmCategory.id},
                            title: "ever best drink",
                            currency: "AUD",
                            originalAmount: 30000,
                            amountInDefaultCurrency: 42000,
                            actualAmountInDefaultCurrency: 41500,
                            reportedAmountInDefaultCurrency: 41500,
                            attachments: [${fry.slurmReceipt.id}],
                            notes: "coffee",
                            percentOnBusiness: 100,
                            id: $expenseId,
                            version: 0,
                            datePaid: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED"
                    }"""
                    )
                )
            }

        val expense = expenseRepository.findById(expenseId)
        assertThat(expense).isPresent.hasValueSatisfying {
            assertThat(it.category).isEqualTo(fry.slurmCategory)
        }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if workspace is not found when creating expense`(fry: Fry) {
        mockCurrentTime(timeService)

        client.post()
            .uri("/api/v1/user/workspaces/995943/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "category": ${fry.slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 30000,
                    "amountInDefaultCurrency": 42000,
                    "actualAmountInDefaultCurrency": 41500,
                    "attachments": [${fry.slurmReceipt.id}],
                    "notes": "coffee",
                    "percentOnBusiness": 100,
                    "datePaid": "$MOCK_DATE_VALUE"
                }"""
            )
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().isEqualTo("Workspace 995943 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if workspace belongs to another user when creating expense`(fry: Fry, bender: Bender) {
        mockCurrentTime(timeService)

        client.post()
            .uri("/api/v1/user/workspaces/${bender.planetExpress.id}/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "category": ${fry.slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 30000,
                    "amountInDefaultCurrency": 42000,
                    "actualAmountInDefaultCurrency": 41500,
                    "attachments": [${fry.slurmReceipt.id}],
                    "notes": "coffee",
                    "percentOnBusiness": 100,
                    "datePaid": "$MOCK_DATE_VALUE"
                }"""
            )
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().isEqualTo("Workspace ${bender.planetExpress.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should create a new expense with minimum data for default currency`(fry: Fry) {
        val expenseId = dbHelper.getNextId()
        mockCurrentTime(timeService)

        client.post()
            .uri("/api/v1/user/workspaces/${fry.workspace.id}/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "category": ${fry.slurmCategory.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE"
                }"""
            )
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                isEqualTo(
                    json(
                        """{
                            category: ${fry.slurmCategory.id},
                            title: "ever best drink",
                            currency: "USD",
                            originalAmount: 150,
                            amountInDefaultCurrency: 150,
                            actualAmountInDefaultCurrency: 150,
                            reportedAmountInDefaultCurrency: 150,
                            percentOnBusiness: 100,
                            id: $expenseId,
                            version: 0,
                            datePaid: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            attachments: [],
                            status: "FINALIZED"
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should create return 404 when category of new expense is not found`(fry: Fry) {
         mockCurrentTime(timeService)

        client.post()
            .uri("/api/v1/user/workspaces/${fry.workspace.id}/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "category": 537453,
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE"
                }"""
            )
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().isEqualTo("Category 537453 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should create return 404 when category of new expense belongs to another user`(fry: Fry, bender: Bender) {
         mockCurrentTime(timeService)

        client.post()
            .uri("/api/v1/user/workspaces/${fry.workspace.id}/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                """{
                    "category": ${bender.suicideBooth.id},
                    "title": "ever best drink",
                    "currency": "USD",
                    "originalAmount": 150,
                    "datePaid": "$MOCK_DATE_VALUE"
                }"""
            )
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().isEqualTo("Category ${bender.suicideBooth.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return expenses of current user`(fry: Fry) {
        client.get()
            .uri("/api/v1/user/workspaces/${fry.workspace.id}/expenses")
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("2")

                inPath("$.data").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            category: ${fry.slurmCategory.id},
                            title: "best ever slurm",
                            currency: "THF",
                            originalAmount: 5000,
                            amountInDefaultCurrency: 500,
                            actualAmountInDefaultCurrency: 450,
                            reportedAmountInDefaultCurrency: 450,
                            attachments: [],
                            percentOnBusiness: 100,
                            id: ${fry.firstSlurm.id},
                            version: 0,
                            datePaid: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED"
                    }"""
                    ),

                    json(
                        """{
                            category: ${fry.slurmCategory.id},
                            title: "another great slurm",
                            currency: "ZZB",
                            originalAmount: 5100,
                            amountInDefaultCurrency: 510,
                            actualAmountInDefaultCurrency: 460,
                            reportedAmountInDefaultCurrency: 455,
                            attachments: [${fry.slurmReceipt.id}],
                            notes: "nice!",
                            percentOnBusiness: 99,
                            id: ${fry.secondSlurm.id},
                            version: 0,
                            datePaid: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED"
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if workspace is not found`(fry: Fry) {
        client.get()
            .uri("/api/v1/user/workspaces/27347947239/expenses")
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().isEqualTo("Workspace 27347947239 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if workspace belongs to another user`(fry: Fry, farnsworth: Farnsworth) {
        client.get()
            .uri("/api/v1/user/workspaces/${farnsworth.workspace.id}/expenses")
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().isEqualTo("Workspace ${farnsworth.workspace.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return expense by id`(fry: Fry) {
        client.get()
            .uri("/api/v1/user/workspaces/${fry.workspace.id}/expenses/${fry.firstSlurm.id}")
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            category: ${fry.slurmCategory.id},
                            title: "best ever slurm",
                            currency: "THF",
                            originalAmount: 5000,
                            amountInDefaultCurrency: 500,
                            actualAmountInDefaultCurrency: 450,
                            reportedAmountInDefaultCurrency: 450,
                            attachments: [],
                            percentOnBusiness: 100,
                            id: ${fry.firstSlurm.id},
                            version: 0,
                            datePaid: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE",
                            status: "FINALIZED"
                    }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if workspace is not found when requesting expense by id`(fry: Fry) {
        client.get()
            .uri("/api/v1/user/workspaces/5634632/expenses/${fry.firstSlurm.id}")
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().isEqualTo("Workspace 5634632 is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return 404 if workspace belongs to another user when requesting expense by id`(
        fry: Fry,
        farnsworth: Farnsworth
    ) {
        client.get()
            .uri("/api/v1/user/workspaces/${farnsworth.workspace.id}/expenses/${fry.firstSlurm.id}")
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().isEqualTo("Workspace ${farnsworth.workspace.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Bender")
    fun `should return 404 if expense belongs to another workspace when requesting expense by id`(bender: Bender) {
        client.get()
            .uri("/api/v1/user/workspaces/${bender.leagueOfRobots.id}/expenses/${bender.boothOne.id}")
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>().isEqualTo("Expense ${bender.boothOne.id} is not found")
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Bender")
    fun `should return expenses of a workspace only`(bender: Bender) {
        client.get()
            .uri("/api/v1/user/workspaces/${bender.leagueOfRobots.id}/expenses")
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                inPath("$.pageNumber").isNumber.isEqualTo("1")
                inPath("$.pageSize").isNumber.isEqualTo("10")
                inPath("$.totalElements").isNumber.isEqualTo("0")
            }
    }
}