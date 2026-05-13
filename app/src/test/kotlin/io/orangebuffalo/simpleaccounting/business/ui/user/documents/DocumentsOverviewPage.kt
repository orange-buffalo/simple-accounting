package io.orangebuffalo.simpleaccounting.business.ui.user.documents

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ConfirmationDialog.Companion.confirmationDialog
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.DocumentsUpload
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SectionHeader.Companion.sectionHeader

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

class CreateStandaloneDocumentPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Upload Document")
    val generalInformationHeader = components.sectionHeader("General Information")
    val documentHeader = components.sectionHeader("Document")
    val title = components.formItemTextInputByLabel("Title")
    val documentsUpload = DocumentsUpload.singleton(components.page)
    val saveButton = components.buttonByText("Save")
    val cancelButton = components.buttonByText("Cancel")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeCreateStandaloneDocumentPage(spec: CreateStandaloneDocumentPage.() -> Unit = {}) {
            CreateStandaloneDocumentPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openCreateStandaloneDocumentPage(spec: CreateStandaloneDocumentPage.() -> Unit = {}) {
            navigate("/documents/create")
            shouldBeCreateStandaloneDocumentPage(spec)
        }
    }
}
