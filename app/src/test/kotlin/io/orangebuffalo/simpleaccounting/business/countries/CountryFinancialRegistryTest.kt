package io.orangebuffalo.simpleaccounting.business.countries

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.MonthDay
import java.util.Locale

class CountryFinancialRegistryTest {

    @Test
    fun `should register every ISO residency country`() {
        CountryFinancialRegistry.byResidency.keys.shouldBe(Locale.getISOCountries().toSet())
    }

    @Test
    fun `should keep income tax years distinct from government fiscal years`() {
        CountryFinancialRegistry.byResidency.getValue("GB").individualIncomeTaxYearStart.shouldBe(MonthDay.of(4, 6))
        CountryFinancialRegistry.byResidency.getValue("AU").individualIncomeTaxYearStart.shouldBe(MonthDay.of(7, 1))
        CountryFinancialRegistry.byResidency.getValue("NZ").individualIncomeTaxYearStart.shouldBe(MonthDay.of(4, 1))
        CountryFinancialRegistry.byResidency.getValue("HK").individualIncomeTaxYearStart.shouldBe(MonthDay.of(4, 1))
        CountryFinancialRegistry.byResidency.getValue("IN").individualIncomeTaxYearStart.shouldBe(MonthDay.of(4, 1))
        CountryFinancialRegistry.byResidency.getValue("ZA").individualIncomeTaxYearStart.shouldBe(MonthDay.of(3, 1))
        CountryFinancialRegistry.byResidency.getValue("US").individualIncomeTaxYearStart.shouldBe(MonthDay.of(1, 1))
    }

    @Test
    fun `should include national and supported foreign currencies`() {
        CountryFinancialRegistry.byResidency.getValue("PL").supportedCurrencies.shouldBe(setOf("PLN", "EUR"))
        CountryFinancialRegistry.byResidency.getValue("BG").supportedCurrencies.shouldBe(setOf("EUR"))
        CountryFinancialRegistry.byResidency.getValue("PA").supportedCurrencies.shouldBe(setOf("PAB", "USD"))
        CountryFinancialRegistry.byResidency.getValue("AU").supportedCurrencies.shouldBe(setOf("AUD"))
    }
}
