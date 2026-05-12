package io.orangebuffalo.simpleaccounting.business.standalonedocuments.impl

import io.orangebuffalo.simpleaccounting.business.standalonedocuments.StandaloneDocument
import io.orangebuffalo.simpleaccounting.business.standalonedocuments.StandaloneDocumentsRepositoryExt
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class StandaloneDocumentsRepositoryExtImpl(
    private val dslContext: DSLContext,
) : StandaloneDocumentsRepositoryExt {

    private val standaloneDocument = Tables.STANDALONE_DOCUMENT
    private val document = Tables.DOCUMENT

    override fun findByIdAndWorkspaceId(id: String, workspaceId: String): StandaloneDocument? = dslContext
        .select(standaloneDocument.fields().toList())
        .from(standaloneDocument)
        .join(document).on(document.id.eq(standaloneDocument.documentId))
        .where(standaloneDocument.id.eq(id))
        .and(document.workspaceId.eq(workspaceId))
        .fetchOne { record ->
            StandaloneDocument(
                id = record[standaloneDocument.id]!!,
                version = record[standaloneDocument.version]!!,
                createdAt = record[standaloneDocument.createdAt]!!,
                title = record[standaloneDocument.title]!!,
                documentId = record[standaloneDocument.documentId]!!,
            )
        }
}
