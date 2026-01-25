package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible

class DocumentsUpload private constructor(
    private val rootLocator: Locator,
) : UiComponent<DocumentsUpload>() {

    fun shouldBeVisible() = rootLocator.shouldBeVisible()

    // The component auto-creates an empty upload slot, so we can just upload to it
    fun uploadDocument(filePath: String) {
        val fileInput = rootLocator.locator("input[type='file']").first()
        fileInput.setInputFiles(filePath)
    }

    fun shouldHaveDocument(fileName: String) {
        rootLocator.locator(".sa-document__name", Locator.LocatorOptions().setHasText(fileName))
            .shouldBeVisible()
    }

    companion object {
        fun byContainer(container: Locator) = DocumentsUpload(container.locator(".sa-documents-upload"))
    }
}
