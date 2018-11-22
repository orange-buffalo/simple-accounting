package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/api/v1/user/currencies")
class CurrencyApiController {

    val currencies = Currency.getAvailableCurrencies().asSequence()
        .map { currency ->
            CurrencyDto(
                code = currency.currencyCode,
                precision = currency.defaultFractionDigits
            )
        }
        .sortedBy(CurrencyDto::code)
        .toList()

    @GetMapping
    fun getCurrencies(): Mono<List<CurrencyDto>> = Mono.just(currencies)
}

data class CurrencyDto(
    val code: String,
    val precision: Int
)