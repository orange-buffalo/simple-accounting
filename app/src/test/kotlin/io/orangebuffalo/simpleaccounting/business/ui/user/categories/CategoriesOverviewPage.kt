package io.orangebuffalo.simpleaccounting.business.ui.user.categories

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class CategoriesOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Categories")
    val pageItems = components.overviewItems()
    val createButton = components.buttonByText("Add new")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeCategoriesOverviewPage(spec: CategoriesOverviewPage.() -> Unit = {}) {
            CategoriesOverviewPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openCategoriesOverviewPage(spec: CategoriesOverviewPage.() -> Unit = {}) {
            navigate("/settings/categories")
            shouldBeCategoriesOverviewPage(spec)
        }
    }
}
