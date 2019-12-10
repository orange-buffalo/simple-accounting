package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.WithMockFryUser
import io.orangebuffalo.simpleaccounting.junit.TestData
import io.orangebuffalo.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.simpleaccounting.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.verifyUnauthorized
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.*

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Currencies API ")
class CurrenciesApiControllerIT(
    @Autowired val client: WebTestClient
) {

    @Test
    fun `should allow GET access only for logged in users`() {
        client.get()
            .uri("/api/currencies")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return currencies list`(data: CurrenciesApiTestData) {
        client.get()
            .uri("/api/currencies")
            .verifyOkAndJsonBody {
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

    class CurrenciesApiTestData : TestData {
        override fun generateData() = listOf(Prototypes.fry())
    }
}
