package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/currencies")
class CurrenciesApiController {

    val availableCurrencies = Currency.getAvailableCurrencies().asSequence()
        .map { currency ->
            CurrencyDto(
                code = currency.currencyCode,
                precision = currency.defaultFractionDigits
            )
        }
        .sortedBy(CurrencyDto::code)
        .toList()

    @GetMapping
    fun getCurrencies(): List<CurrencyDto> = availableCurrencies
}

data class CurrencyDto(
    val code: String,
    val precision: Int
)