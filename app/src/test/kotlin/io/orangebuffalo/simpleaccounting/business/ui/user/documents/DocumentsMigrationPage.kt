package io.orangebuffalo.simpleaccounting.business.ui.user.documents

import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class DocumentsMigrationPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Documents Migration")
    private val loadingIndicator = page.locator(".sa-documents-migration-page__loading")
    private val description = page.locator(".sa-documents-migration-page__description")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun shouldHaveLoadingIndicatorVisible() {
        loadingIndicator.shouldBeVisible()
    }

    fun shouldHaveDescription(text: String) {
        description.shouldBeVisible()
        description.shouldHaveText(text)
    }

    companion object {
        fun Page.openDocumentsMigrationPage(spec: DocumentsMigrationPage.() -> Unit) {
            navigate("/documents/migration")
            DocumentsMigrationPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.shouldBeDocumentsMigrationPage(spec: DocumentsMigrationPage.() -> Unit = {}) {
            DocumentsMigrationPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
