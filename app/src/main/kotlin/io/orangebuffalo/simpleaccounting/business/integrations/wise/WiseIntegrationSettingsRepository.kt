package io.orangebuffalo.simpleaccounting.business.integrations.wise

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface WiseIntegrationSettingsRepository : AbstractEntityRepository<WiseIntegrationSettings> {
    fun existsByWorkspaceId(workspaceId: String): Boolean
}
