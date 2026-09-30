package io.orangebuffalo.simpleaccounting.business.api.workspaces

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.infra.graphql.Query
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class CountriesQuery : Query {
    @GraphQLDescription("All supported ISO 3166-1 alpha-2 country codes.")
    @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
    fun countries(): List<String> = Locale.getISOCountries().toList()
}
