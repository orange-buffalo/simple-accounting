package io.orangebuffalo.simpleaccounting.business.ui.user.documents

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.standalonedocuments.StandaloneDocument
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.documents.CreateStandaloneDocumentPage.Companion.openCreateStandaloneDocumentPage
import io.orangebuffalo.simpleaccounting.business.ui.user.documents.DocumentsOverviewPage.Companion.shouldBeDocumentsOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload.DocumentState.PENDING
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload.Companion.EmptyDocument
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload.UploadedDocument
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveTitles
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeBytes

class CreateStandaloneDocumentFullStackTest : SaFullStackTestBase() {

    @TempDir
    private lateinit var tempDir: Path

    @Test
    fun `should create standalone document from uploaded file`(page: Page) {
        val fileContent = "Planet Express crew roster".toByteArray()
        val testFile = createTestFile("crew-roster.pdf", fileContent)

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateStandaloneDocumentPage {
            title {
                input.fill("Planet Express crew roster")
            }
            documentsUpload {
                shouldHaveDocuments(EmptyDocument)
                selectFileForUpload(testFile)
                shouldHaveDocuments(UploadedDocument("crew-roster.pdf", PENDING))
            }
            reportRendering("create-standalone-document.ready-to-save")
            saveButton.click()
        }

        page.shouldBeDocumentsOverviewPage {
            pageItems.shouldHaveTitles("crew-roster.pdf")
        }

        val standaloneDocument = aggregateTemplate.findSingle<StandaloneDocument>()
        val uploadedDocument = aggregateTemplate.findSingle<Document>(standaloneDocument.documentId)

        standaloneDocument.shouldBeEntityWithFields(
            StandaloneDocument(
                title = "Planet Express crew roster",
                documentId = uploadedDocument.id!!,
            )
        )
        uploadedDocument.shouldBeEntityWithFields(
            Document(
                name = "crew-roster.pdf",
                workspaceId = preconditions.workspace.id!!,
                timeUploaded = MOCK_TIME,
                storageId = TestDocumentsStorage.STORAGE_ID,
                storageLocation = uploadedDocument.storageLocation,
                sizeInBytes = fileContent.size.toLong(),
                mimeType = "application/pdf",
            )
        )
        testDocumentsStorage.getUploadedContent(uploadedDocument.storageLocation!!).shouldBe(fileContent)
    }

    @Test
    fun `should allow selecting only one file with removal and replacement`(page: Page) {
        val firstFile = createTestFile("slurm-contract.pdf", "First file".toByteArray())
        val secondFile = createTestFile("delivery-manifest.pdf", "Second file".toByteArray())

        page.authenticateViaCookie(preconditions.fry)
        page.openCreateStandaloneDocumentPage {
            documentsUpload {
                shouldHaveDocuments(EmptyDocument)

                selectFileForUpload(firstFile)
                shouldHaveDocuments(UploadedDocument("slurm-contract.pdf", PENDING))

                removeDocument("slurm-contract.pdf")
                shouldHaveDocuments(EmptyDocument)

                selectFileForUpload(secondFile)
                shouldHaveDocuments(UploadedDocument("delivery-manifest.pdf", PENDING))
            }
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
            val workspace = workspace(owner = fry)
        }
    }

    private fun createTestFile(fileName: String, content: ByteArray): Path {
        val testFile = tempDir.resolve(fileName)
        testFile.writeBytes(content)
        return testFile
    }
}
