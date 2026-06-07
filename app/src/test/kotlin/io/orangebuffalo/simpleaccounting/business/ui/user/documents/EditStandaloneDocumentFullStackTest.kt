package io.orangebuffalo.simpleaccounting.business.ui.user.documents

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.standalonedocuments.StandaloneDocument
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.documents.EditStandaloneDocumentPage.Companion.openCreateStandaloneDocumentPage
import io.orangebuffalo.simpleaccounting.business.ui.user.documents.EditStandaloneDocumentPage.Companion.openEditStandaloneDocumentPage
import io.orangebuffalo.simpleaccounting.business.ui.user.documents.EditStandaloneDocumentPage.Companion.shouldBeEditStandaloneDocumentPage
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

class EditStandaloneDocumentFullStackTest : SaFullStackTestBase() {

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

    @Test
    fun `should edit standalone document title without changing linked document`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val document = document(workspace = workspace, name = "delivery-permit.pdf")
                val standaloneDocument = standaloneDocument(
                    workspace = workspace,
                    document = document,
                    title = "Old delivery permit",
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openEditStandaloneDocumentPage(testData.standaloneDocument.id!!) {
            title {
                input.shouldHaveValue("Old delivery permit")
                input.fill("Updated delivery permit")
            }
            documentsUpload.shouldBeHidden()
            reportRendering("edit-standalone-document.ready-to-save")
            saveButton.click()
        }

        page.shouldBeDocumentsOverviewPage()

        aggregateTemplate.findSingle<StandaloneDocument>(testData.standaloneDocument.id!!)
            .shouldBeEntityWithFields(
                StandaloneDocument(
                    title = "Updated delivery permit",
                    documentId = testData.document.id!!,
                )
            )
    }

    @Test
    fun `should show warning when saving outdated standalone document state`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val standaloneDocument = standaloneDocument(workspace = workspace)
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openEditStandaloneDocumentPage(testData.standaloneDocument.id!!) {
            title { input.fill("Updated delivery permit") }
            aggregateTemplate.save(testData.standaloneDocument.copy(title = "Delivery permit changed elsewhere"))

            saveButton.click()

            shouldHaveNotifications {
                warning("This record has changed since you opened it. Reload the page and apply your changes again.")
            }
        }
        page.shouldBeEditStandaloneDocumentPage {
            title { input.shouldHaveValue("Updated delivery permit") }
        }

        aggregateTemplate.findSingle<StandaloneDocument>(testData.standaloneDocument.id!!)
            .title.shouldBe("Delivery permit changed elsewhere")
    }

    @Test
    fun `should show validation errors when editing standalone document`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val standaloneDocument = standaloneDocument(
                    workspace = workspace,
                    document = document(workspace = workspace, name = "slurm-license.pdf"),
                    title = "Slurm license",
                )
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openEditStandaloneDocumentPage(testData.standaloneDocument.id!!) {
            title { input.fill("") }
            saveButton.click()

            title {
                shouldHaveValidationError("This value is required and should not be blank")
            }
            shouldHaveNotifications { validationFailed() }

            title { input.fill("x".repeat(256)) }
            saveButton.click()

            title {
                shouldHaveValidationError("The length of this value should be no longer than 255 characters")
            }
            shouldHaveNotifications { validationFailed() }
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
