package io.orangebuffalo.simpleaccounting.business.countries

import java.time.MonthDay
import java.util.Currency
import java.util.Locale

data class CountryFinancialDetails(
    val individualIncomeTaxYearStart: MonthDay,
    val supportedCurrencies: Set<String>,
)

object CountryFinancialRegistry {
    private val incomeTaxYearStarts = mapOf(
        "AU" to MonthDay.of(7, 1),
        "BD" to MonthDay.of(7, 1),
        "GB" to MonthDay.of(4, 6),
        "HK" to MonthDay.of(4, 1),
        "IN" to MonthDay.of(4, 1),
        "LK" to MonthDay.of(4, 1),
        "MU" to MonthDay.of(7, 1),
        "NZ" to MonthDay.of(4, 1),
        "PK" to MonthDay.of(7, 1),
        "ZA" to MonthDay.of(3, 1),
    )

    // Curated additions to the JVM's single currency per region; this is not an exhaustive list of accepted currencies.
    private val additionalCurrencies = mapOf(
        "BT" to setOf("INR"),
        "HT" to setOf("USD"),
        "LS" to setOf("ZAR"),
        "NA" to setOf("ZAR"),
        "PA" to setOf("USD"),
        "PL" to setOf("EUR"),
        "PS" to setOf("ILS", "JOD"),
    )

    // Bulgaria adopted EUR in 2026; older JDK currency data still reports BGN.
    private val currencyOverrides = mapOf(
        "BG" to setOf("EUR"),
    )

    // January 1 is a convention for countries without a registered individual income-tax year start.
    val byResidency: Map<String, CountryFinancialDetails> = Locale.getISOCountries().associateWith { countryCode ->
        val nationalCurrency = Currency.getInstance(Locale.of("", countryCode))?.currencyCode
        CountryFinancialDetails(
            individualIncomeTaxYearStart = incomeTaxYearStarts[countryCode] ?: MonthDay.of(1, 1),
            supportedCurrencies = currencyOverrides[countryCode]
                ?: (listOfNotNull(nationalCurrency) + additionalCurrencies[countryCode].orEmpty()).toSet(),
        )
    }
}
