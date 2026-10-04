package io.orangebuffalo.simpleaccounting.business.api.integrations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationAccount
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationSettings
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationSettingsRepository
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.graphql.Mutation
import io.orangebuffalo.simpleaccounting.infra.graphql.Query
import io.orangebuffalo.simpleaccounting.infra.thirdparty.wise.WiseApiClient
import io.orangebuffalo.simpleaccounting.infra.thirdparty.wise.WiseInvalidTokenException
import io.orangebuffalo.simpleaccounting.infra.thirdparty.wise.WiseUnavailableException
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

class WiseIntegrationGqlApi {
    @Component
    class Queries(
        private val workspaces: WorkspacesService,
        private val settings: WiseIntegrationSettingsRepository,
    ) : Query {
        @GraphQLDescription("Whether Wise is configured for the owned workspace. Never returns credentials.")
        @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
        fun integrations(workspaceId: String): IntegrationsGqlDto {
            workspaces.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.ADMIN)
            return IntegrationsGqlDto(WiseIntegrationGqlDto(settings.existsByWorkspaceId(workspaceId)))
        }
    }

    @Component
    @Validated
    class Mutations(
        private val workspaces: WorkspacesService,
        private val settings: WiseIntegrationSettingsRepository,
        private val wise: WiseApiClient,
    ) : Mutation {
        @GraphQLDescription("Verifies a personal token and lists balances and Jars across all Wise profiles without persisting the token.")
        @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
        @BusinessError(WiseInvalidTokenException::class, "INVALID_TOKEN", "Wise rejected the personal token.")
        @BusinessError(WiseUnavailableException::class, "UNAVAILABLE", "Wise accounts could not be loaded.")
        fun verifyWiseIntegrationToken(
            workspaceId: String,
            @NotBlank @Size(max = 2000) token: String,
        ): WiseAccountsResult {
            workspaces.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.ADMIN)
            return WiseAccountsResult(accounts = wise.getAccounts(token).map {
                WiseAccountGqlDto(it.profileId.toString(), it.profileName, it.accountId.toString(), it.currency, it.name, it.type)
            })
        }

        @GraphQLDescription("Activates Wise for an owned workspace, verifying every selected profile, balance and currency against the personal token.")
        @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
        @BusinessError(WiseInvalidTokenException::class, "INVALID_TOKEN", "Wise rejected the personal token.")
        @BusinessError(WiseUnavailableException::class, "UNAVAILABLE", "Wise accounts could not be loaded.")
        @BusinessError(DuplicateKeyException::class, "ALREADY_ACTIVE", "Wise is already active for this workspace.")
        @BusinessError(WiseInvalidAccountsException::class, "INVALID_ACCOUNTS", "The selected Wise accounts are invalid or have changed.")
        fun setupWiseIntegration(
            workspaceId: String,
            @NotBlank @Size(max = 2000) token: String,
            @Valid @Size(min = 1) accounts: List<WiseAccountInput>,
        ): WiseSetupResult {
            workspaces.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.ADMIN)
            if (settings.existsByWorkspaceId(workspaceId)) throw WiseAlreadyActiveException()
            val verified = verifyWiseIntegrationToken(workspaceId, token)
            val available = verified.accounts.map { WiseAccountInput(it.profileId, it.accountId, it.currency) }.toSet()
            if (accounts.toSet().size != accounts.size || !available.containsAll(accounts)) {
                throw WiseInvalidAccountsException()
            }
            val selected = accounts.map { WiseIntegrationAccount(it.profileId.toLong(), it.accountId.toLong(), it.currency) }.toSet()
            settings.save(WiseIntegrationSettings(workspaceId, token, selected))
            return WiseSetupResult(true)
        }
    }
}

@GraphQLDescription("A Wise account selection. IDs are decimal strings to preserve 64-bit precision in clients.")
data class WiseAccountInput(
    @field:Size(min = 1, max = 19) val profileId: String,
    @field:Size(min = 1, max = 19) val accountId: String,
    @field:Size(min = 3, max = 3) val currency: String,
)

@GraphQLDescription("A currency balance or Jar belonging to a Wise profile. IDs preserve 64-bit precision as strings.")
data class WiseAccountGqlDto(
    val profileId: String,
    val profileName: String,
    val accountId: String,
    val currency: String,
    val name: String?,
    val type: String,
)

@GraphQLDescription("Workspace-scoped integration providers.")
data class IntegrationsGqlDto(val wise: WiseIntegrationGqlDto)

@GraphQLDescription("Wise integration metadata. Credentials are never returned.")
data class WiseIntegrationGqlDto(val active: Boolean)

@GraphQLDescription("Verified balances across all Wise profiles.")
data class WiseAccountsResult(val accounts: List<WiseAccountGqlDto>)

@GraphQLDescription("Whether the integration was saved successfully.")
data class WiseSetupResult(val success: Boolean)

class WiseAlreadyActiveException : DuplicateKeyException("Wise is already active for this workspace")
class WiseInvalidAccountsException : RuntimeException("The selected Wise accounts are invalid or have changed")
