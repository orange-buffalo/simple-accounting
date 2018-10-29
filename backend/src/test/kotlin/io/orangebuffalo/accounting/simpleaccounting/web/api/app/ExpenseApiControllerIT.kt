package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Fry
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.ExpenseRepository
import io.orangebuffalo.accounting.simpleaccounting.web.DbHelper
import io.orangebuffalo.accounting.simpleaccounting.web.expectThatJsonBody
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
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

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should create a new expense`(fry: Fry) {
        val expenseId = dbHelper.getNextId()

        client.post()
            .uri("/api/v1/user/workspaces/${fry.workspace.id}/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .syncBody(
                // todo datePaid
                """{
                    "category": ${fry.slurmCategory.id},
                    "currency": "USD",
                    "originalAmount": 30000,
                    "amountInDefaultCurrency": 42000,
                    "actualAmountInDefaultCurrency": 41500,
                    "attachments": [],
                    "notes": "coffee",
                    "percentOnBusinessInBps": 10000
                }"""
            )
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                isEqualTo(
                    json(
                        //todo dates
                        """{
                            category: ${fry.slurmCategory.id},
                            currency: "USD",
                            originalAmount: 30000,
                            amountInDefaultCurrency: 42000,
                            actualAmountInDefaultCurrency: 41500,
                            attachments: [],
                            notes: "coffee",
                            percentOnBusinessInBps: 10000,
                            id: $expenseId,
                            version: 0
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
                            attachments: [],
                            notes: null,
                            percentOnBusinessInBps: 10000,
                            id: ${fry.firstSlurm.id},
                            version: 0
                    }"""
                    ),

                    json(
                        """{
                            category: ${fry.slurmCategory.id},
                            currency: "ZZB",
                            originalAmount: 5100,
                            amountInDefaultCurrency: 510,
                            actualAmountInDefaultCurrency: 460,
                            attachments: [],
                            notes: "nice!",
                            percentOnBusinessInBps: 9900,
                            id: ${fry.secondSlurm.id},
                            version: 0
                    }"""
                    )
                )
            }
    }
}