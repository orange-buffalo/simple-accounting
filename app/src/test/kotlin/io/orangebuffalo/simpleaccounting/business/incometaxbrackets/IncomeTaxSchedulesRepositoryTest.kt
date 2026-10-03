package io.orangebuffalo.simpleaccounting.business.incometaxbrackets

import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

class IncomeTaxSchedulesRepositoryTest(
    @Autowired private val schedulesRepository: IncomeTaxSchedulesRepository,
) : SaIntegrationTestBase() {

    @Test
    @Transactional
    fun `should replace a schedule and its brackets through the repository`() {
        val periodStart = LocalDate.of(3025, 1, 1)
        val schedule = IncomeTaxSchedule(
            countryCode = "AU",
            jurisdiction = "national",
            taxpayer = "Fry, resident individual",
            taxPeriodLabel = "3025",
            periodStart = periodStart,
            periodEndExclusive = LocalDate.of(3026, 1, 1),
            currency = "AUD",
            basis = "annual_taxable_income",
            limitations = "Excludes Slurm delivery levies.",
            brackets = setOf(IncomeTaxBracket(BigDecimal.ZERO, BigDecimal("0.10"))),
            sources = setOf(
                IncomeTaxScheduleSource("Planet Express tax office", "https://example.com/planet-express", periodStart)
            ),
        )

        val saved = schedulesRepository.save(schedule)
        schedulesRepository.deleteById(saved.id!!)
        val replacement = schedulesRepository.save(
            schedule.copy(brackets = setOf(IncomeTaxBracket(BigDecimal.ZERO, BigDecimal("0.20"))))
        )

        val loaded = schedulesRepository.findByCountryCodeAndPeriodStartLessThanEqualAndPeriodEndExclusiveGreaterThan(
            "AU", periodStart, periodStart
        ).single { it.id == replacement.id }
        loaded.brackets.single().rate.compareTo(BigDecimal("0.20")).shouldBe(0)
        loaded.sources.single().title.shouldBe("Planet Express tax office")
    }
}
