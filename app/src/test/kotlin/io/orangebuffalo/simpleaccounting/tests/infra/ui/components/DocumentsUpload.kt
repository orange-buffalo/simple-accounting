package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Download
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy
import org.junit.jupiter.api.fail
import java.nio.file.Files
import java.nio.file.Path

class DocumentsUpload private constructor(
    private val rootLocator: Locator,
    private val page: Page,
) : UiComponent<DocumentsUpload>() {

    private val loadingPlaceholder = rootLocator.locator(".sa-documents-upload__loading-placeholder")
    private val storageErrorAlert = rootLocator.locator(".el-alert--error")
    private val dropzoneTrigger = rootLocator.locator(".sa-document-upload__file-selector")

    fun shouldBeVisible(): DocumentsUpload {
        rootLocator.shouldBeVisible()
        return this
    }

    fun shouldHaveLoadingPlaceholder(): DocumentsUpload {
        loadingPlaceholder.shouldBeVisible()
        return this
    }

    fun shouldHaveStorageErrorMessage(expectedMessage: String): DocumentsUpload {
        storageErrorAlert.shouldBeVisible()
        storageErrorAlert.shouldSatisfy("Error message should contain expected text") {
            textContent()?.contains(expectedMessage, ignoreCase = false) shouldBe true
        }
        return this
    }

    fun selectFileForUpload(filePath: Path): DocumentsUpload {
        val fileChooser = page.waitForFileChooser { dropzoneTrigger.click() }
        fileChooser.setFiles(filePath)
        return this
    }

    fun shouldHaveDocuments(vararg expected: UploadedDocument): DocumentsUpload {
        shouldSatisfy("Documents should match expected state") {
            val documentLocators = rootLocator.locator(".sa-documents-upload__document").all()
            val actualDocuments = documentLocators.map { documentLocator ->
                extractDocumentInfo(documentLocator)
            }
            actualDocuments.shouldContainExactly(expected.toList())
        }
        return this
    }

    private fun extractDocumentInfo(documentLocator: Locator): UploadedDocument {
        // Check if this is an empty upload slot first
        val fileSelectorVisible = documentLocator.locator(".sa-document-upload__file-selector").isVisible
        if (fileSelectorVisible) {
            return UploadedDocument("", DocumentState.EMPTY)
        }

        val fileNameLocator = documentLocator.locator(".sa-document__file-description__header__file-name")
        val fileName = if (fileNameLocator.isVisible) fileNameLocator.textContent() ?: "" else ""

        val state = when {
            documentLocator.locator(".sa-document__loader__file-icon").isVisible -> DocumentState.LOADING
            documentLocator.locator(".sa-document-upload__status")
                .filter(Locator.FilterOptions().setHasText("New document to be uploaded"))
                .isVisible -> DocumentState.PENDING

            documentLocator.locator(".el-progress").isVisible -> DocumentState.UPLOADING
            documentLocator.locator(".sa-document-upload__status_error").isVisible -> DocumentState.UPLOAD_FAILED
            documentLocator.locator(".sa-document__file-description__file-extras__download-link").isVisible -> DocumentState.COMPLETED
            else -> fail { "Could not determine document state for file '$fileName'" }
        }

        val uploadPercentage = if (state == DocumentState.UPLOADING) {
            val progressText = documentLocator.locator(".el-progress__text").textContent() ?: "0%"
            progressText.removeSuffix("%").toIntOrNull()
        } else {
            null
        }

        return UploadedDocument(fileName, state, uploadPercentage)
    }

    fun removeDocument(fileName: String): DocumentsUpload {
        val documentLocator = findDocumentByName(fileName)
        val removeIcon = documentLocator.locator(".sa-document__file-description__header__remove-icon")
        removeIcon.click()
        return this
    }

    fun downloadDocument(fileName: String): ByteArray {
        val documentLocator = findDocumentByName(fileName)
        val downloadLink = documentLocator.locator(".sa-document__file-description__file-extras__download-link button")

        val download: Download = page.waitForDownload {
            downloadLink.click()
        }

        val downloadPath = download.path()
        val content = Files.readAllBytes(downloadPath)
        Files.delete(downloadPath)
        return content
    }

    private fun findDocumentByName(fileName: String): Locator {
        return rootLocator.locator(".sa-documents-upload__document")
            .filter(
                Locator.FilterOptions().setHas(
                    page.locator(".sa-document__file-description__header__file-name")
                        .locator("text=$fileName")
                )
            )
    }

    data class UploadedDocument(
        val fileName: String,
        val state: DocumentState,
        val uploadPercentage: Int? = null
    )

    enum class DocumentState {
        EMPTY,
        LOADING,
        PENDING,
        UPLOADING,
        UPLOAD_FAILED,
        COMPLETED
    }

    companion object {
        val EmptyDocument = UploadedDocument("", DocumentState.EMPTY)

        fun singleton(page: Page): DocumentsUpload {
            val container = page.locator(".sa-documents-upload")
            return DocumentsUpload(container, page)
        }

        fun byContainer(container: Locator): DocumentsUpload {
            val page = container.page()
            val documentsUploadContainer = container.locator(".sa-documents-upload")
            return DocumentsUpload(documentsUploadContainer, page)
        }
    }
}
