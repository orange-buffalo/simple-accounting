package io.orangebuffalo.simpleaccounting.business.documents

import io.kotest.assertions.throwables.shouldThrow
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentRequest
import io.orangebuffalo.simpleaccounting.business.documents.storage.SaveDocumentResponse
import io.orangebuffalo.simpleaccounting.business.integration.TokensRepository
import io.orangebuffalo.simpleaccounting.business.integration.downloads.DownloadsService
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.business.workspaces.WorkspacesService
import io.orangebuffalo.simpleaccounting.infra.InputStreamProvider
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.infra.TokenGenerator
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException

class DocumentsServiceTest {
    private val documentStorage = mock<DocumentsStorage>()
    private val documentRepository = mock<DocumentsRepository>()
    private val platformUsersService = mock<PlatformUsersService>()
    private val timeService = mock<TimeService>()
    private val documentsService = DocumentsService(
        documentsStorages = listOf(documentStorage),
        documentRepository = documentRepository,
        timeService = timeService,
        workspacesService = mock<WorkspacesService>(),
        platformUsersService = platformUsersService,
        downloadsService = mock<DownloadsService>(),
        tokensRepository = mock<TokensRepository>(),
        tokenGenerator = mock<TokenGenerator>(),
    )

    @Test
    fun `should delete stored content when document persistence fails`() {
        val workspace = Workspace(
            id = "planet-express-workspace",
            name = "Planet Express",
            ownerId = "professor-farnsworth",
            defaultCurrency = "USD",
        )
        val request = SaveDocumentRequest(
            fileName = "interplanetary-tax-records.txt",
            content = mock<InputStreamProvider>(),
            workspace = workspace,
            contentType = "text/plain",
        )
        doReturn("test-storage").whenever(documentStorage).getId()
        doReturn(
            PlatformUser(
                id = workspace.ownerId,
                userName = "Farnsworth",
                passwordHash = "password-hash",
                isAdmin = false,
                activated = true,
                documentsStorage = "test-storage",
            )
        ).whenever(platformUsersService).getUserByUserId(workspace.ownerId)
        doReturn(SaveDocumentResponse("stored-document", 42))
            .whenever(documentStorage).saveDocument(request)
        doReturn(MOCK_TIME).whenever(timeService).currentTime()
        doThrow(DataIntegrityViolationException("Document metadata could not be stored"))
            .whenever(documentRepository).save(any<Document>())

        shouldThrow<DataIntegrityViolationException> {
            documentsService.saveDocument(request)
        }

        verify(documentStorage).deleteDocument(workspace, "stored-document")
    }
}
