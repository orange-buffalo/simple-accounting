package io.orangebuffalo.simpleaccounting.business.api.integrations

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import graphql.schema.DataFetchingEnvironment
import io.orangebuffalo.simpleaccounting.infra.graphql.getBean
import io.orangebuffalo.simpleaccounting.infra.graphql.withRequestAuthentication
import io.orangebuffalo.simpleaccounting.business.api.directives.RequiredAuth
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessError
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseAccountSelection
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseIntegrationService
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseInvalidAccountsException
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseInvalidTokenException
import io.orangebuffalo.simpleaccounting.business.integrations.wise.WiseUnavailableException
import io.orangebuffalo.simpleaccounting.infra.graphql.Mutation
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

class IntegrationsGqlApi {
    @Component
    @Validated
    class VerifyWiseIntegrationTokenMutation(
        private val wise: WiseIntegrationService,
    ) : Mutation {
        @GraphQLDescription("Lists balances and Jars with saved selections and deleted accounts. Verifies a supplied token without persisting it; null uses the saved credential.")
        @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
        @BusinessError(WiseInvalidTokenException::class, "INVALID_TOKEN", "Wise rejected the personal token.")
        @BusinessError(WiseUnavailableException::class, "UNAVAILABLE", "Wise accounts could not be loaded.")
        fun verifyWiseIntegrationToken(
            workspaceId: String,
            @Size(max = 2000) token: String? = null,
        ): WiseAccountsResult {
            return WiseAccountsResult(accounts = wise.loadAccounts(workspaceId, token).map {
                val account = it.account
                WiseAccountGqlDto(account.profileId.toString(), account.profileName, account.accountId.toString(), account.currency, account.name, account.type, it.selected, it.deleted)
            })
        }
    }

    @Component
    @Validated
    class SetupWiseIntegrationMutation(
        private val wise: WiseIntegrationService,
    ) : Mutation {
        @GraphQLDescription("Saves Wise settings for an owned workspace, verifying every selection. A null token retains the saved credential.")
        @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
        @BusinessError(WiseInvalidTokenException::class, "INVALID_TOKEN", "Wise rejected the personal token.")
        @BusinessError(WiseUnavailableException::class, "UNAVAILABLE", "Wise accounts could not be loaded.")
        @BusinessError(WiseInvalidAccountsException::class, "INVALID_ACCOUNTS", "The selected Wise accounts are invalid or have changed.")
        fun setupWiseIntegration(
            workspaceId: String,
            @Size(max = 2000) token: String? = null,
            @Valid @Size(min = 1) accounts: List<WiseAccountInput>,
        ): WiseSetupResult {
            wise.setup(workspaceId, token, accounts.map { WiseAccountSelection(it.profileId, it.accountId, it.currency) })
            return WiseSetupResult(true)
        }
    }
}

@GraphQLDescription("A Wise account selection. Identifiers are opaque values returned by token verification.")
data class WiseAccountInput(
    @field:Size(min = 1, max = 19) val profileId: String,
    @field:Size(min = 1, max = 19) val accountId: String,
    @field:Size(min = 3, max = 3) val currency: String,
)

@GraphQLDescription("A currency balance or Jar belonging to a Wise profile. Identifiers are opaque values.")
data class WiseAccountGqlDto(
    val profileId: String,
    val profileName: String,
    val accountId: String,
    val currency: String,
    val name: String?,
    val type: String,
    @GraphQLDescription("Whether this account is part of the saved configuration.")
    val selected: Boolean,
    @GraphQLDescription("Whether the saved account is no longer available with this credential and cannot be selected.")
    val deleted: Boolean,
)

@GraphQLDescription("Workspace-scoped integration providers.")
class IntegrationsGqlDto(private val workspaceId: String) {
    @GraphQLDescription("Wise integration metadata for this workspace.")
    fun wise() = WiseIntegrationGqlDto(workspaceId)
}

@GraphQLDescription("Wise integration metadata. Credentials are never returned.")
class WiseIntegrationGqlDto(private val workspaceId: String) {
    @GraphQLDescription("Whether Wise is active for this workspace.")
    fun active(env: DataFetchingEnvironment): Boolean = env.withRequestAuthentication {
        env.graphQlContext.getBean<WiseIntegrationService>().isActive(workspaceId)
    }
}

@GraphQLDescription("Verified balances across all Wise profiles.")
data class WiseAccountsResult(val accounts: List<WiseAccountGqlDto>)

@GraphQLDescription("Whether the integration was saved successfully.")
data class WiseSetupResult(val success: Boolean)
