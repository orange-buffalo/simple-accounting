package io.orangebuffalo.simpleaccounting.business.api.workspaces

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.countries.CountryFinancialRegistry
import io.orangebuffalo.simpleaccounting.infra.graphql.Query
import org.springframework.stereotype.Component

@Component
class CountriesQuery : Query {
    @GraphQLDescription("Supported ISO 3166-1 alpha-2 country codes, optionally restricted to countries supporting a currency.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    fun countries(
        @GraphQLDescription("Only return countries supporting this ISO 4217 currency code.")
        currency: String? = null,
    ): List<String> = CountryFinancialRegistry.byResidency
        .filterValues { currency == null || currency in it.supportedCurrencies }
        .keys.toList()
}
