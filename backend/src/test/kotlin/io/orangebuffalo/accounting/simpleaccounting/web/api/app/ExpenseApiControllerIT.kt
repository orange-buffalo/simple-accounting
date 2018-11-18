package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
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
            .expectStatus().isOk
            .expectThatJsonBody {
                isEqualTo(
                    json(
                        """{
                            category: ${fry.slurmCategory.id},
                            currency: "USD",
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
                            timeRecorded: "$MOCK_TIME_VALUE"
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
                            currency: "THF",
                            originalAmount: 5000,
                            amountInDefaultCurrency: 500,
                            actualAmountInDefaultCurrency: 450,
                            reportedAmountInDefaultCurrency: 450,
                            attachments: [],
                            notes: null,
                            percentOnBusiness: 100,
                            id: ${fry.firstSlurm.id},
                            version: 0,
                            datePaid: "$MOCK_DATE_VALUE",
                            timeRecorded: "$MOCK_TIME_VALUE"
                    }"""
                    ),

                    json(
                        """{
                            category: ${fry.slurmCategory.id},
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
                            timeRecorded: "$MOCK_TIME_VALUE"
                    }"""
                    )
                )
            }
    }
}