package io.orangebuffalo.simpleaccounting.business.ui.user.documents

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class DocumentsOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Documents")
    val pageItems = components.overviewItems()

    private fun shouldBeOpen() {
        header.shouldBeVisible()
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
