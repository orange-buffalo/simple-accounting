package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Fry
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
import java.util.*

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Currency API ")
class CurrencyControllerIT(
    @Autowired val client: WebTestClient
) {

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return currencies list`(fry: Fry) {
        client.get()
            .uri("/api/v1/user/currencies")
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                isArray
                    .size().isEqualTo(Currency.getAvailableCurrencies().size).returnToIterable()
                    .contains(
                        json(
                            """{
                            code: "AUD",
                            precision: 2
                        }"""
                        ),
                        json(
                            """{
                            code: "JPY",
                            precision: 0
                        }"""
                        ),
                        json(
                            """{
                            code: "JOD",
                            precision: 3
                        }"""
                        )
                    )
            }
    }
}