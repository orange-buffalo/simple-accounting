package io.orangebuffalo.simpleaccounting.business.integrations.wise

import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WiseIntegrationService internal constructor(
    private val workspaces: WorkspacesService,
    private val settings: WiseIntegrationSettingsRepository,
    private val client: WiseApiClient,
) {
    fun isActive(workspaceId: String): Boolean {
        workspaces.validateWorkspaceAccess(workspaceId = workspaceId, accessMode = WorkspaceAccessMode.ADMIN)
        return settings.existsByWorkspaceId(workspaceId)
    }

    fun getSettings(workspaceId: String): WiseIntegrationSettings? {
        workspaces.validateWorkspaceAccess(workspaceId = workspaceId, accessMode = WorkspaceAccessMode.ADMIN)
        return settings.findByWorkspaceId(workspaceId)
    }

    fun verifyToken(workspaceId: String, token: String?): List<WiseAccount> {
        workspaces.validateWorkspaceAccess(workspaceId = workspaceId, accessMode = WorkspaceAccessMode.ADMIN)
        return client.getAccounts(resolveToken(token = token, existing = settings.findByWorkspaceId(workspaceId)))
    }

    fun loadAccounts(workspaceId: String, token: String?): List<WiseSelectableAccount> {
        workspaces.validateWorkspaceAccess(workspaceId = workspaceId, accessMode = WorkspaceAccessMode.ADMIN)
        val existing = settings.findByWorkspaceId(workspaceId)
        val available = client.getAccounts(resolveToken(token = token, existing = existing))
        val selected = existing?.accounts.orEmpty()
        val current = available.map { account ->
            WiseSelectableAccount(
                account = account,
                selected = selected.any { it.profileId == account.profileId && it.accountId == account.accountId && it.currency == account.currency },
                deleted = false,
            )
        }
        val missing = selected.filter { saved ->
            available.none { it.profileId == saved.profileId && it.accountId == saved.accountId && it.currency == saved.currency }
        }.map { saved ->
            WiseSelectableAccount(
                account = WiseAccount(
                    profileId = saved.profileId, profileName = available.firstOrNull { it.profileId == saved.profileId }?.profileName.orEmpty(),
                    profileType = available.firstOrNull { it.profileId == saved.profileId }?.profileType,
                    accountId = saved.accountId, currency = saved.currency, name = null, type = "", amountInCents = null,
                ),
                selected = true,
                deleted = true,
            )
        }
        return current + missing
    }

    @Transactional
    fun setup(workspaceId: String, token: String?, accounts: List<WiseAccountSelection>, fetchTransactionsPeriodically: Boolean = true) {
        workspaces.validateWorkspaceAccess(workspaceId = workspaceId, accessMode = WorkspaceAccessMode.ADMIN)
        val existing = settings.findByWorkspaceId(workspaceId)?.let { settings.lockById(it.id!!) }
        val resolvedToken = resolveToken(token = token, existing = existing)
        val available = client.getAccounts(resolvedToken).map {
            WiseAccountSelection(profileId = it.profileId.toString(), accountId = it.accountId.toString(), currency = it.currency)
        }.toSet()
        if (accounts.isEmpty() || accounts.toSet().size != accounts.size || !available.containsAll(accounts)) {
            throw WiseInvalidAccountsException()
        }
        val selected = accounts.map {
            existing?.accounts?.find { saved -> saved.profileId.toString() == it.profileId && saved.accountId.toString() == it.accountId && saved.currency == it.currency }
                ?: WiseIntegrationAccount(profileId = it.profileId.toLong(), accountId = it.accountId.toLong(), currency = it.currency)
        }.toSet()
        settings.save(existing?.copy(token = resolvedToken, accounts = selected,
            fetchTransactionsPeriodically = fetchTransactionsPeriodically, authenticationRequired = false)
            ?: WiseIntegrationSettings(workspaceId = workspaceId, token = resolvedToken, accounts = selected,
                fetchTransactionsPeriodically = fetchTransactionsPeriodically))
    }

    private fun resolveToken(token: String?, existing: WiseIntegrationSettings?): String =
        (token ?: existing?.token)?.takeIf { it.isNotBlank() } ?: throw WiseInvalidTokenException()
}

data class WiseSelectableAccount(val account: WiseAccount, val selected: Boolean, val deleted: Boolean)

data class WiseAccountSelection(val profileId: String, val accountId: String, val currency: String)

data class WiseAccount(
    val profileId: Long,
    val profileName: String,
    val profileType: String?,
    val accountId: Long,
    val currency: String,
    val name: String?,
    val type: String,
    val amountInCents: Long?,
)

class WiseInvalidTokenException : RuntimeException("Wise rejected the token")
class WiseUnavailableException : RuntimeException("Wise accounts could not be loaded")
class WiseInvalidAccountsException : RuntimeException("The selected Wise accounts are invalid or have changed")
