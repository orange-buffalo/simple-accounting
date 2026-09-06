package io.orangebuffalo.simpleaccounting.business.api.profile

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.oauthproviders.OAuthUserAuthenticationService
import io.orangebuffalo.simpleaccounting.infra.graphql.Mutation
import jakarta.validation.constraints.NotBlank
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class UnlinkOAuthIdentityMutation(
    private val oauthUserAuthenticationService: OAuthUserAuthenticationService,
) : Mutation {

    @Suppress("unused")
    @GraphQLDescription(
        "Removes the identity linked at the provided provider from the current user profile. " +
                "Once the last identity is removed, password login becomes available again."
    )
    @RequiredAuth(RequiredAuth.AuthType.AUTHENTICATED_USER)
    fun unlinkOAuthIdentity(
        @GraphQLDescription("ID of the OAuth2 provider to unlink the identity at.")
        @NotBlank
        providerId: String,
    ): UnlinkOAuthIdentityResponse {
        oauthUserAuthenticationService.unlinkIdentity(providerId)
        return UnlinkOAuthIdentityResponse(success = true)
    }

    @GraphQLDescription("Response for the unlinkOAuthIdentity mutation.")
    data class UnlinkOAuthIdentityResponse(
        @GraphQLDescription("Whether the identity has been unlinked.")
        val success: Boolean,
    )
}
