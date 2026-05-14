package io.orangebuffalo.simpleaccounting.business.ui.user.documents

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ConfirmationDialog.Companion.confirmationDialog
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering

class DocumentsOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Documents")
    val pageItems = components.overviewItems()
    val uploadButton = components.buttonByText("Upload")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun confirmDocumentDeletion() {
        components.confirmationDialog()
            .shouldBeVisible()
            .clickButton("Delete")
    }

    fun reportRenderingWithPopovers(name: String) {
        page.locator("body").reportRendering(name)
    }

    companion object {
        fun Page.shouldBeDocumentsOverviewPage(spec: DocumentsOverviewPage.() -> Unit = {}) {
            DocumentsOverviewPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openDocumentsOverviewPage(spec: DocumentsOverviewPage.() -> Unit = {}) {
            navigate("/documents")
            shouldBeDocumentsOverviewPage(spec)
        }
    }
}
