package io.orangebuffalo.simpleaccounting.infra

import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.time.LocalDate

class JacksonConfigTest(
    @Autowired private val objectMapper: ObjectMapper,
) : SaIntegrationTestBase() {

    @Test
    fun `should serialize Java time values as ISO strings`() {
        val json = objectMapper.readTree(
            objectMapper.writeValueAsString(
                DateTimePayload(
                    deliveryDate = LocalDate.of(3025, 1, 15),
                    deliveryTime = Instant.parse("3025-01-15T10:30:00Z"),
                    optionalNote = null,
                )
            )
        )

        json.path("deliveryDate").asString().shouldBe("3025-01-15")
        json.path("deliveryTime").asString().shouldBe("3025-01-15T10:30:00Z")
        json.has("optionalNote").shouldBe(false)
    }

    private data class DateTimePayload(
        val deliveryDate: LocalDate,
        val deliveryTime: Instant,
        val optionalNote: String?,
    )
}
