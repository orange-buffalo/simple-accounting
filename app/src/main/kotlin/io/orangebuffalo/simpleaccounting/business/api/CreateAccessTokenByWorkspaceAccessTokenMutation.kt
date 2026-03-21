package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.security.createTransientUserPrincipal
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.workspaces.InvalidWorkspaceAccessTokenException
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessTokensService
import jakarta.validation.constraints.NotBlank
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
class CreateAccessTokenByWorkspaceAccessTokenMutation(
    private val jwtService: JwtService,
    private val workspaceAccessTokensService: WorkspaceAccessTokensService,
) : Mutation {
    @Suppress("unused")
    @GraphQLDescription(
        "Authenticates a user by a shared workspace access token and returns an access token. " +
                "This is used for login-by-link functionality."
    )
    @RequiredAuth(RequiredAuth.AuthType.ANONYMOUS)
    @BusinessError(
        exceptionClass = InvalidWorkspaceAccessTokenException::class,
        errorCode = "INVALID_WORKSPACE_ACCESS_TOKEN",
        description = "The provided workspace access token is not valid (unknown, expired, or revoked).",
    )
    suspend fun createAccessTokenByWorkspaceAccessToken(
        @GraphQLDescription("The shared workspace access token.")
        @NotBlank
        workspaceAccessToken: String,
    ): CreateAccessTokenByWorkspaceAccessTokenResponse {
        val token = workspaceAccessTokensService.getValidToken(workspaceAccessToken)
            ?: throw InvalidWorkspaceAccessTokenException(workspaceAccessToken)
        val jwtToken = jwtService.buildJwtToken(
            createTransientUserPrincipal(token.token),
            token.validTill
        )
        return CreateAccessTokenByWorkspaceAccessTokenResponse(accessToken = jwtToken)
    }

    @GraphQLDescription("Response for the createAccessTokenByWorkspaceAccessToken mutation.")
    data class CreateAccessTokenByWorkspaceAccessTokenResponse(
        @param:GraphQLDescription("The JWT access token for the authenticated user.")
        val accessToken: String,
    )
}
