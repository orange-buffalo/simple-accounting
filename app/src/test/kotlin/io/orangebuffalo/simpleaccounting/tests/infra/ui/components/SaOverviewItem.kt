package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageableItems.Companion.pageableItems
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath
import io.orangebuffalo.simpleaccounting.tests.infra.utils.innerTextOrNull
import io.orangebuffalo.simpleaccounting.tests.infra.utils.innerTextTrimmed
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeHidden

class SaOverviewItem private constructor(
    private val panel: Locator,
) {

    val title: String?
        get() = panel.locator(".overview-item__title").innerTextOrNull()

    val primaryAttributes: List<PrimaryAttribute>
        get() = panel.locator(".overview-item-primary-attribute").all().map {
            PrimaryAttribute(
                icon = it.locator(".overview-item-primary-attribute__icon").getAttribute("data-icon"),
                text = it.innerTextTrimmed(),
            )
        }

    private val detailsTrigger = panel.locator(".overview-item__details-trigger")

    fun shouldNotHaveDetails() {
        detailsTrigger.shouldBeHidden()
    }

    fun executeAction(actionLinkText: String) {
        panel
            .locator("xpath=.//*[${XPath.hasClass("sa-action-link")} and .//*[${XPath.hasText(actionLinkText)}]]")
            .click()
    }

    companion object {
        fun <T : SaPageBase<T>> ComponentsAccessors<T>.overviewItems() =
            pageableItems { container -> SaOverviewItem(container.locator(".overview-item__panel")) }
    }

    data class PrimaryAttribute(
        val icon: String,
        val text: String,
    )
}
