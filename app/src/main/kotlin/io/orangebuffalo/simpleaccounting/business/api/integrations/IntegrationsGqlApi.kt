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
    class ListWiseAccountsMutation(
        private val wise: WiseIntegrationService,
    ) : Mutation {
        @GraphQLDescription("Lists balances and Jars with saved selections and deleted accounts. Verifies a supplied token without persisting it; null uses the saved credential.")
        @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
        @BusinessError(
            exceptionClass = WiseInvalidTokenException::class,
            errorCode = "INVALID_TOKEN",
            errorCodeDescription = "Wise rejected the personal token.",
        )
        @BusinessError(
            exceptionClass = WiseUnavailableException::class,
            errorCode = "UNAVAILABLE",
            errorCodeDescription = "Wise accounts could not be loaded.",
        )
        fun listWiseAccounts(
            @GraphQLDescription("The workspace whose Wise accounts should be listed. Only its owner can access these accounts.")
            workspaceId: String,
            @GraphQLDescription("A supplied non-blank token is used without saving it. Null or omission uses the saved token; if none exists, INVALID_TOKEN is returned. Empty or whitespace-only tokens return INVALID_TOKEN.")
            @Size(max = 2000) token: String? = null,
        ): WiseAccountsResult {
            return WiseAccountsResult(accounts = wise.loadAccounts(workspaceId = workspaceId, token = token).map {
                val account = it.account
                WiseAccountGqlDto(
                    profileId = account.profileId.toString(),
                    profileName = account.profileName,
                    accountId = account.accountId.toString(),
                    currency = account.currency,
                    name = account.name,
                    type = account.type,
                    usedByIntegration = it.selected,
                    deleted = it.deleted,
                )
            })
        }
    }

    @Component
    @Validated
    class SaveWiseIntegrationSettingsMutation(
        private val wise: WiseIntegrationService,
    ) : Mutation {
        @GraphQLDescription("Saves Wise settings for an owned workspace, verifying every selection. A null token retains the saved credential.")
        @RequiredAuth(RequiredAuth.AuthType.REGULAR_USER)
        @BusinessError(
            exceptionClass = WiseInvalidTokenException::class,
            errorCode = "INVALID_TOKEN",
            errorCodeDescription = "Wise rejected the personal token.",
        )
        @BusinessError(
            exceptionClass = WiseUnavailableException::class,
            errorCode = "UNAVAILABLE",
            errorCodeDescription = "Wise accounts could not be loaded.",
        )
        @BusinessError(
            exceptionClass = WiseInvalidAccountsException::class,
            errorCode = "INVALID_ACCOUNTS",
            errorCodeDescription = "The selected Wise accounts are invalid or have changed.",
        )
        fun saveWiseIntegrationSettings(
            @GraphQLDescription("The workspace whose Wise integration settings should be created or updated. Only its owner can save settings.")
            workspaceId: String,
            @GraphQLDescription("A supplied non-blank token is verified and saved, replacing any existing token. Null or omission retains the saved token; if none exists, INVALID_TOKEN is returned. Empty or whitespace-only tokens return INVALID_TOKEN.")
            @Size(max = 2000) token: String? = null,
            @GraphQLDescription("The complete non-empty set of accounts to use. Replaces the saved set; every account must be available with the effective token.")
            @Valid @Size(min = 1) accounts: List<WiseAccountInput>,
        ): WiseSettingsSaveResult {
            wise.setup(
                workspaceId = workspaceId,
                token = token,
                accounts = accounts.map { WiseAccountSelection(profileId = it.profileId, accountId = it.accountId, currency = it.currency) },
            )
            return WiseSettingsSaveResult(success = true)
        }
    }
}

@GraphQLDescription("A Wise account to use in the integration. Identifiers are opaque values returned by listWiseAccounts.")
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
    val usedByIntegration: Boolean,
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
data class WiseSettingsSaveResult(val success: Boolean)
