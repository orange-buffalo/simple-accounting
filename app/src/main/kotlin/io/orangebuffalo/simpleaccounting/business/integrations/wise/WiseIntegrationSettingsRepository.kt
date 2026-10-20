package io.orangebuffalo.simpleaccounting.business.integrations.wise

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository
import org.springframework.data.jdbc.repository.query.Query

interface WiseIntegrationSettingsRepository : AbstractEntityRepository<WiseIntegrationSettings> {
    fun existsByWorkspaceId(workspaceId: String): Boolean
    fun findByWorkspaceId(workspaceId: String): WiseIntegrationSettings?

    fun findAllByFetchTransactionsPeriodicallyTrueAndAuthenticationRequiredFalse(): List<WiseIntegrationSettings>

    @Query("select * from wise_integration_settings where id = :id for update")
    fun lockById(id: String): WiseIntegrationSettings?
}
