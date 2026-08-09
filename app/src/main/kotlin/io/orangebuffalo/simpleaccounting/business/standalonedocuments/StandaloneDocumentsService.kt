package io.orangebuffalo.simpleaccounting.business.standalonedocuments

import io.orangebuffalo.simpleaccounting.business.documents.DocumentIsUsedException
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import org.springframework.stereotype.Service

@Service
class StandaloneDocumentsService(
    private val standaloneDocumentsRepository: StandaloneDocumentsRepository,
    private val documentsService: DocumentsService,
    private val workspacesService: WorkspacesService,
) {

    fun createStandaloneDocument(workspaceId: String, standaloneDocument: StandaloneDocument): StandaloneDocument {
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_WRITE)
        documentsService.validateDocuments(workspaceId, listOf(standaloneDocument.documentId))
        return standaloneDocumentsRepository.save(standaloneDocument)
    }

    fun saveStandaloneDocument(workspaceId: String, standaloneDocument: StandaloneDocument): StandaloneDocument {
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_WRITE)
        documentsService.validateDocuments(workspaceId, listOf(standaloneDocument.documentId))
        return standaloneDocumentsRepository.save(standaloneDocument)
    }

    fun getStandaloneDocumentByIdAndWorkspaceId(id: String, workspaceId: String): StandaloneDocument? =
        standaloneDocumentsRepository.findByIdAndWorkspaceId(id, workspaceId)

    fun removeStandaloneDocument(
        workspaceId: String,
        standaloneDocumentId: String,
        removeDocumentIfUnused: Boolean,
    ): StandaloneDocument? {
        workspacesService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.READ_WRITE)
        val standaloneDocument = getStandaloneDocumentByIdAndWorkspaceId(standaloneDocumentId, workspaceId)
            ?: return null

        standaloneDocumentsRepository.delete(standaloneDocument)

        if (removeDocumentIfUnused) {
            try {
                documentsService.deleteDocument(workspaceId, standaloneDocument.documentId)
            } catch (_: DocumentIsUsedException) {
                // The standalone reference is removed; keep the linked document when another entity still uses it.
            }
        }

        return standaloneDocument
    }
}
