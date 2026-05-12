package io.orangebuffalo.simpleaccounting.business.standalonedocuments

import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import org.springframework.stereotype.Service

@Service
class StandaloneDocumentsService(
    private val standaloneDocumentsRepository: StandaloneDocumentsRepository,
    private val documentsService: DocumentsService,
    private val workspacesService: WorkspacesService,
) {

    suspend fun createStandaloneDocument(workspaceId: String, standaloneDocument: StandaloneDocument): StandaloneDocument {
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_WRITE)
        documentsService.validateDocuments(workspaceId, listOf(standaloneDocument.documentId))
        return withDbContext { standaloneDocumentsRepository.save(standaloneDocument) }
    }

    suspend fun saveStandaloneDocument(workspaceId: String, standaloneDocument: StandaloneDocument): StandaloneDocument {
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_WRITE)
        documentsService.validateDocuments(workspaceId, listOf(standaloneDocument.documentId))
        return withDbContext { standaloneDocumentsRepository.save(standaloneDocument) }
    }

    suspend fun getStandaloneDocumentByIdAndWorkspaceId(id: String, workspaceId: String): StandaloneDocument? = withDbContext {
        standaloneDocumentsRepository.findByIdAndWorkspaceId(id, workspaceId)
    }
}
