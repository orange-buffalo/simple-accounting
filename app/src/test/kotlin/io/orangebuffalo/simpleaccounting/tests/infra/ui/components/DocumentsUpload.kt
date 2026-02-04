package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Download
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.kotestplaywrightassertions.shouldNotBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import java.nio.file.Files
import java.nio.file.Path

class DocumentsUpload private constructor(
    private val rootLocator: Locator,
    private val page: Page,
) : UiComponent<DocumentsUpload>() {

    private val loadingPlaceholder = rootLocator.locator(".sa-documents-upload__loading-placeholder")
    private val storageErrorAlert = rootLocator.locator(".el-alert--error")
    private val documentElements = rootLocator.locator(".sa-documents-upload__document")
    private val emptyUploadSlots = rootLocator.locator(".sa-document-upload__file-selector")

    fun shouldBeVisible(): DocumentsUpload {
        rootLocator.shouldBeVisible()
        return this
    }

    fun shouldHaveLoadingPlaceholder(): DocumentsUpload {
        loadingPlaceholder.shouldBeVisible()
        return this
    }

    fun shouldHaveStorageErrorMessage(): DocumentsUpload {
        storageErrorAlert.shouldBeVisible()
        return this
    }

    fun shouldHaveEmptyUploadSlot(): DocumentsUpload {
        emptyUploadSlots.first().shouldBeVisible()
        return this
    }

    fun uploadFile(filePath: Path): DocumentsUpload {
        val fileInput = rootLocator.locator("input[type='file']").first()
        fileInput.setInputFiles(filePath)
        return this
    }

    fun uploadFiles(vararg filePaths: Path): DocumentsUpload {
        val fileInput = rootLocator.locator("input[type='file']").first()
        fileInput.setInputFiles(filePaths.toList().toTypedArray())
        return this
    }

    fun shouldHaveDocuments(count: Int): DocumentsUpload {
        documentElements.shouldHaveCount(count)
        return this
    }

    fun shouldHaveDocumentWithName(fileName: String): DocumentsUpload {
        rootLocator.locator(".sa-document__file-description__header__file-name")
            .locator("text=$fileName")
            .shouldBeVisible()
        return this
    }

    fun shouldHaveDocumentInState(fileName: String, state: DocumentState): DocumentsUpload {
        val documentLocator = findDocumentByName(fileName)
        when (state) {
            DocumentState.LOADING -> {
                documentLocator.locator(".sa-document__loader__file-icon").shouldBeVisible()
            }
            DocumentState.PENDING -> {
                documentLocator.locator(".sa-document-upload__status")
                    .filter(Locator.FilterOptions().setHasText("Scheduled for upload"))
                    .shouldBeVisible()
            }
            DocumentState.UPLOADING -> {
                documentLocator.locator(".el-progress").shouldBeVisible()
            }
            DocumentState.UPLOAD_FAILED -> {
                documentLocator.locator(".sa-document-upload__status_error").shouldBeVisible()
            }
            DocumentState.COMPLETED -> {
                documentLocator.locator(".sa-document__file-description__file-extras__download-link")
                    .shouldBeVisible()
            }
        }
        return this
    }

    fun shouldHaveUploadProgress(fileName: String, percentage: Int): DocumentsUpload {
        val documentLocator = findDocumentByName(fileName)
        val progressText = documentLocator.locator(".el-progress__text")
        progressText.shouldHaveText("$percentage%")
        return this
    }

    fun removeDocument(fileName: String): DocumentsUpload {
        val documentLocator = findDocumentByName(fileName)
        val removeIcon = documentLocator.locator(".sa-document__file-description__header__remove-icon")
        removeIcon.click()
        return this
    }

    fun shouldNotHaveDocument(fileName: String): DocumentsUpload {
        rootLocator.locator(".sa-document__file-description__header__file-name")
            .locator("text=$fileName")
            .shouldNotBeVisible()
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

    fun reportRendering(name: String): DocumentsUpload {
        rootLocator.reportRendering(name)
        return this
    }

    private fun findDocumentByName(fileName: String): Locator {
        return rootLocator.locator(".sa-documents-upload__document")
            .filter(Locator.FilterOptions().setHas(
                page.locator(".sa-document__file-description__header__file-name")
                    .locator("text=$fileName")
            ))
    }

    enum class DocumentState {
        LOADING,
        PENDING,
        UPLOADING,
        UPLOAD_FAILED,
        COMPLETED
    }

    companion object {
        fun byPage(page: Page): DocumentsUpload {
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
