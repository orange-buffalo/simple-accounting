package io.orangebuffalo.simpleaccounting.business.api.oauthproviders

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAUTH_IDENTITY_CALLBACK_PATH
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingProperties
import io.orangebuffalo.simpleaccounting.infra.graphql.Query
import org.springframework.stereotype.Component

@Component
class OAuthCallbackUrlQuery(
    private val properties: SimpleAccountingProperties,
) : Query {

    @Suppress("unused")
    @GraphQLDescription(
        "Returns the redirect URL that must be whitelisted at the OAuth2 providers " +
                "in order for the authentication flows to work."
    )
    @RequiredAuth(RequiredAuth.AuthType.ADMIN_USER)
    fun oauthCallbackUrl(): String = "${properties.publicUrl}$OAUTH_IDENTITY_CALLBACK_PATH"
}
