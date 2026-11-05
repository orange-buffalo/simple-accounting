package io.orangebuffalo.simpleaccounting.business.standalonedocuments

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface StandaloneDocumentsRepository : AbstractEntityRepository<StandaloneDocument>, StandaloneDocumentsRepositoryExt

interface StandaloneDocumentsRepositoryExt {
    fun existsByWorkspaceIdAndTitle(workspaceId: String, title: String): Boolean
    fun findByIdAndWorkspaceId(id: String, workspaceId: String): StandaloneDocument?
}
