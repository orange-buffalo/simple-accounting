package io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class GeneralTaxesOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("General Taxes")
    val pageItems = components.overviewItems()
    val createButton = components.buttonByText("Add new")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeGeneralTaxesOverviewPage(spec: GeneralTaxesOverviewPage.() -> Unit = {}) {
            GeneralTaxesOverviewPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openGeneralTaxesOverviewPage(spec: GeneralTaxesOverviewPage.() -> Unit = {}) {
            navigate("/settings/general-taxes")
            shouldBeGeneralTaxesOverviewPage(spec)
        }
    }
}
