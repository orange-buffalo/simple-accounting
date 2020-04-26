package io.orangebuffalo.simpleaccounting.web.api.integration

import io.orangebuffalo.simpleaccounting.services.business.DocumentsService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.stereotype.Component

// todo #222: remove
@Component
class ApiControllersExtensions(
    private val documentsService: DocumentsService
) {

    suspend fun getValidDocuments(
        workspace: Workspace,
        documentIds: List<Long>?
    ): Set<Document> = documentsService.getValidDocuments(workspace, documentIds)

}
