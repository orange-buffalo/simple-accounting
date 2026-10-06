package io.orangebuffalo.simpleaccounting.business.integrations.wise

import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import org.springframework.dao.DuplicateKeyException
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

    fun verifyToken(workspaceId: String, token: String): List<WiseAccount> {
        workspaces.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.ADMIN)
        return client.getAccounts(token)
    }

    fun setup(workspaceId: String, token: String, accounts: List<WiseAccountSelection>) {
        workspaces.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.ADMIN)
        if (settings.existsByWorkspaceId(workspaceId)) throw WiseAlreadyActiveException()
        val available = client.getAccounts(token).map {
            WiseAccountSelection(it.profileId.toString(), it.accountId.toString(), it.currency)
        }.toSet()
        if (accounts.isEmpty() || accounts.toSet().size != accounts.size || !available.containsAll(accounts)) {
            throw WiseInvalidAccountsException()
        }
        val selected = accounts.map {
            WiseIntegrationAccount(it.profileId.toLong(), it.accountId.toLong(), it.currency)
        }.toSet()
        settings.save(WiseIntegrationSettings(workspaceId, token, selected))
    }
}

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
class WiseAlreadyActiveException : DuplicateKeyException("Wise is already active for this workspace")
class WiseInvalidAccountsException : RuntimeException("The selected Wise accounts are invalid or have changed")
