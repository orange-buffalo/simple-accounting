package io.orangebuffalo.simpleaccounting.business.ui.user.accountstatements

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldHaveSize
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount
import io.orangebuffalo.kotestplaywrightassertions.shouldContainClass
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewFilters.Companion.overviewFilters
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase

class AccountStatementsOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Account statements")
    val pageItems = components.overviewItems()
    val filters = components.overviewFilters()

    fun statement(title: String): SaOverviewItem {
        pageItems.shouldHaveDataSatisfying { items -> items.filter { it.title == title }.shouldHaveSize(1) }
        return pageItems.shouldHaveItemSatisfying { it.title == title }.also {
            it.locator(":scope").shouldHaveCount(1)
        }
    }

    fun shouldHaveTypeBadge(title: String, status: String) {
        val badge = statement(title).locator(".overview-item__middle-column .sa-status-label")
        badge.shouldHaveCount(1)
        badge.shouldContainClass("sa-status-label_${status}")
        badge.locator(":scope[class*='-simplified']").shouldHaveCount(0)
        badge.locator(".sa-icon").shouldHaveCount(0)
    }

    companion object {
        fun Page.shouldBeAccountStatementsOverviewPage(spec: AccountStatementsOverviewPage.() -> Unit = {}) {
            AccountStatementsOverviewPage(this).apply {
                header.shouldBeVisible()
                spec()
            }
        }

        fun Page.openAccountStatementsOverviewPage(spec: AccountStatementsOverviewPage.() -> Unit = {}) {
            navigate("/account-statements")
            shouldBeAccountStatementsOverviewPage(spec)
        }
    }
}
