package io.orangebuffalo.simpleaccounting.business.integrations.wise

import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import org.springframework.stereotype.Service

@Service
class WiseIntegrationService internal constructor(
    private val workspaces: WorkspacesService,
    private val settings: WiseIntegrationSettingsRepository,
    private val client: WiseApiClient,
) {
    fun isActive(workspaceId: String): Boolean {
        workspaces.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.ADMIN)
        return settings.existsByWorkspaceId(workspaceId)
    }

    fun verifyToken(workspaceId: String, token: String?): List<WiseAccount> {
        workspaces.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.ADMIN)
        return client.getAccounts(resolveToken(token, settings.findByWorkspaceId(workspaceId)))
    }

    fun loadAccounts(workspaceId: String, token: String?): List<WiseSelectableAccount> {
        workspaces.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.ADMIN)
        val existing = settings.findByWorkspaceId(workspaceId)
        val available = client.getAccounts(resolveToken(token, existing))
        val selected = existing?.accounts.orEmpty()
        val current = available.map { account ->
            WiseSelectableAccount(account, selected.contains(WiseIntegrationAccount(account.profileId, account.accountId, account.currency)), false)
        }
        val missing = selected.filter { saved ->
            available.none { it.profileId == saved.profileId && it.accountId == saved.accountId && it.currency == saved.currency }
        }.map { saved ->
            WiseSelectableAccount(WiseAccount(saved.profileId, "", saved.accountId, saved.currency, null, ""), true, true)
        }
        return current + missing
    }

    fun setup(workspaceId: String, token: String?, accounts: List<WiseAccountSelection>) {
        workspaces.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.ADMIN)
        val existing = settings.findByWorkspaceId(workspaceId)
        val resolvedToken = resolveToken(token, existing)
        val available = client.getAccounts(resolvedToken).map {
            WiseAccountSelection(it.profileId.toString(), it.accountId.toString(), it.currency)
        }.toSet()
        if (accounts.isEmpty() || accounts.toSet().size != accounts.size || !available.containsAll(accounts)) {
            throw WiseInvalidAccountsException()
        }
        val selected = accounts.map {
            WiseIntegrationAccount(it.profileId.toLong(), it.accountId.toLong(), it.currency)
        }.toSet()
        settings.save(existing?.copy(token = resolvedToken, accounts = selected)
            ?: WiseIntegrationSettings(workspaceId, resolvedToken, selected))
    }

    private fun resolveToken(token: String?, existing: WiseIntegrationSettings?): String =
        (token ?: existing?.token)?.takeIf { it.isNotBlank() } ?: throw WiseInvalidTokenException()
}

data class WiseSelectableAccount(val account: WiseAccount, val selected: Boolean, val deleted: Boolean)

data class WiseAccountSelection(val profileId: String, val accountId: String, val currency: String)

data class WiseAccount(
    val profileId: Long,
    val profileName: String,
    val accountId: Long,
    val currency: String,
    val name: String?,
    val type: String,
)

class WiseInvalidTokenException : RuntimeException("Wise rejected the token")
class WiseUnavailableException : RuntimeException("Wise accounts could not be loaded")
class WiseInvalidAccountsException : RuntimeException("The selected Wise accounts are invalid or have changed")
