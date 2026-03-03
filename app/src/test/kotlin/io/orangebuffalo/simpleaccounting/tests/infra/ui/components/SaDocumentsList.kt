package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Download
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy
import io.orangebuffalo.simpleaccounting.tests.infra.utils.visualToData
import java.nio.file.Files

class SaDocumentsList private constructor(
    private val rootLocator: Locator,
    private val page: Page,
) : UiComponent<SaDocumentsList>() {

    private val loadingPlaceholder = rootLocator.locator(".sa-documents-list__loading-placeholder")
    private val storageErrorAlert = rootLocator.locator(".el-alert--error")

    fun shouldBeVisible(): SaDocumentsList {
        rootLocator.shouldBeVisible()
        return this
    }

    fun shouldHaveStorageLoadingPlaceholder(): SaDocumentsList {
        loadingPlaceholder.shouldBeVisible()
        return this
    }

    fun shouldHaveStorageErrorMessage(expectedMessage: String): SaDocumentsList {
        storageErrorAlert.shouldBeVisible()
        storageErrorAlert.shouldSatisfy("Error message should contain expected text") {
            textContent()?.contains(expectedMessage, ignoreCase = false) shouldBe true
        }
        return this
    }

    fun shouldHaveDocumentsLoading(count: Int): SaDocumentsList {
        shouldSatisfy("$count loading document(s) should be visible") {
            val loadingDocuments = rootLocator.locator(".sa-document__loader__file-icon").all()
            loadingDocuments.size shouldBe count
        }
        return this
    }

    fun shouldHaveDocuments(vararg expectedNames: String): SaDocumentsList {
        shouldSatisfy("Documents should match expected names") {
            val documents = rootLocator.locator(".sa-document__file-description__header__file-name").all()
            val actualNames = documents.map { it.textContent() ?: "" }
            actualNames.shouldContainExactly(expectedNames.toList())
        }
        return this
    }

    fun downloadDocument(documentName: String): ByteArray {
        val documentLocator = findDocumentByName(documentName)
        val downloadLink = documentLocator.locator(".sa-document__file-description__file-extras__download-link button")
        val download: Download = page.waitForDownload {
            downloadLink.click()
        }
        val downloadPath = download.path()
        val content = Files.readAllBytes(downloadPath)
        Files.delete(downloadPath)
        return content
    }

    fun reportRendering(name: String): SaDocumentsList {
        rootLocator.reportRendering(name)
        return this
    }

    private fun findDocumentByName(documentName: String): Locator {
        return rootLocator.locator(".sa-document")
            .filter(
                Locator.FilterOptions().setHas(
                    page.locator(".sa-document__file-description__header__file-name")
                        .locator("text=$documentName")
                )
            )
    }

    companion object {

        private const val VISUAL_SEMANTIC = "documents"

        /**
         * Tests can use this method to produce documents list data value from document names.
         * This is used for JS-based data extractors, like [SaPageableItems].
         */
        fun documentsValue(vararg documentNames: String): String =
            visualToData(VISUAL_SEMANTIC, documentNames.joinToString(", "))

        /**
         * JavaScript function that extracts the documents list content from any element inside the documents list component.
         * Used in JS-based data extractors, like [SaPageableItems], indirectly via
         * [io.orangebuffalo.simpleaccounting.tests.infra.utils.injectJsUtils] `getDynamicContent`.
         * Not intended for direct use in tests.
         */
        fun jsDataExtractor() = /* language=JavaScript */ """
            (anyElement) => {
                const documentsListElement = utils.findClosestByClass(anyElement, 'sa-documents-list');
                if (!documentsListElement) {
                    return null;
                }
                
                // Check if storage is loading
                const loadingPlaceholder = documentsListElement.querySelector('.sa-documents-list__loading-placeholder');
                if (loadingPlaceholder) {
                    return '<loading>';
                }
                
                // Check if storage is not active (shows an error alert)
                const failedStorageMessage = documentsListElement.querySelector('.el-alert--error');
                if (failedStorageMessage) {
                    return utils.getDynamicContent(failedStorageMessage);
                }
                
                // Find all document elements (using the root class from SaDocument component)
                const documentElements = Array.from(documentsListElement.querySelectorAll('.sa-document'));
                if (documentElements.length === 0) {
                    return null;
                }
                
                const documentNames = documentElements
                    .map(doc => {
                        const loadinIndicator = doc.querySelector('.sa-document__loader__file-icon');
                        if (loadinIndicator) {
                            return '<document loading>';
                        }
                        const nameElement = doc.querySelector('.sa-document__file-description__header__file-name');
                        return utils.getDynamicContent(nameElement);
                    });
                
                return utils.visualToData('$VISUAL_SEMANTIC', documentNames.join(', '));
            }
        """

        fun byContainer(container: Locator): SaDocumentsList {
            val page = container.page()
            return SaDocumentsList(container.locator(".sa-documents-list"), page)
        }

        fun singleton(page: Page): SaDocumentsList {
            return SaDocumentsList(page.locator(".sa-documents-list"), page)
        }
    }
}
