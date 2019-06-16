package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Roberto
import io.orangebuffalo.accounting.simpleaccounting.web.expectThatJsonBody
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Statistics API ")
internal class StatisticsApiControllerIT(
    @Autowired val client: WebTestClient
) {

    @Test
    @WithMockUser(roles = ["USER"], username = "Roberto")
    fun `should calculate expenses statistics`(roberto: Roberto) {
        client.get()
            .uri(
                "/api/workspaces/${roberto.workspace.id}/statistics/expenses" +
                        "?fromDate=3000-04-10&toDate=3000-10-01"
            )
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                inPath("$.totalAmount").isNumber.isEqualTo("644")
                inPath("$.finalizedCount").isNumber.isEqualTo("4")
                inPath("$.pendingCount").isNumber.isEqualTo("3")
                inPath("$.items").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            "categoryId": ${roberto.firstCategory.id},
                            "totalAmount": 223,
                            "finalizedCount": 2,
                            "pendingCount": 0
                        }"""
                    ),
                    json(
                        """{
                            "categoryId": ${roberto.secondCategory.id},
                            "totalAmount": 421,
                            "finalizedCount": 2,
                            "pendingCount": 3
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Roberto")
    fun `should calculate incomes statistics`(roberto: Roberto) {
        client.get()
            .uri(
                "/api/workspaces/${roberto.workspace.id}/statistics/incomes" +
                        "?fromDate=3010-04-21&toDate=3010-09-15"
            )
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                inPath("$.totalAmount").isNumber.isEqualTo("568")
                inPath("$.finalizedCount").isNumber.isEqualTo("3")
                inPath("$.pendingCount").isNumber.isEqualTo("2")
                inPath("$.currencyExchangeGain").isNumber.isEqualTo("25")
                inPath("$.items").isArray.containsExactlyInAnyOrder(
                    json(
                        """{
                            "categoryId": ${roberto.firstCategory.id},
                            "totalAmount": 335,
                            "finalizedCount": 2,
                            "pendingCount": 0,
                            "currencyExchangeGain":25
                        }"""
                    ),
                    json(
                        """{
                            "categoryId": ${roberto.secondCategory.id},
                            "totalAmount": 233,
                            "finalizedCount": 1,
                            "pendingCount": 2,
                            "currencyExchangeGain": 0
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Roberto")
    fun `should calculate tax payments statistics`(roberto: Roberto) {
        client.get()
            .uri(
                "/api/workspaces/${roberto.workspace.id}/statistics/tax-payments" +
                        "?fromDate=3005-07-02&toDate=3005-08-01"
            )
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                inPath("$.totalTaxPayments").isNumber.isEqualTo("77")
            }
    }
}