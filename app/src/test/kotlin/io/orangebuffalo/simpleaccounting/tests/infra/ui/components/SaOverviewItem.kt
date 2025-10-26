package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldContainClass
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageableItems.Companion.pageableItems
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath
import io.orangebuffalo.simpleaccounting.tests.infra.utils.innerTextOrNull
import io.orangebuffalo.simpleaccounting.tests.infra.utils.innerTextTrimmed

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

    val attributePreviewIcons: List<String>
        get() = panel.locator(".overview-item__attribute-preview .sa-icon")
            .all()
            .map { it.getAttribute("data-icon") }

    private val statusLabelLocator: Locator
        get() = panel.locator(".overview-item__middle-column .sa-status-label")

    fun hasAttributePreviewIcons(vararg icons: String) {
        val actualIcons = attributePreviewIcons.sorted()
        val expectedIcons = icons.toList().sorted()
        actualIcons.shouldContainExactly(expectedIcons)
    }

    fun shouldHaveSuccessStatus(text: String? = null) {
        statusLabelLocator.shouldContainClass("sa-status-label_success")
        if (text != null) {
            statusLabelLocator.shouldHaveText(text)
        }
    }

    fun shouldHavePendingStatus(text: String? = null) {
        statusLabelLocator.shouldContainClass("sa-status-label_pending")
        if (text != null) {
            statusLabelLocator.shouldHaveText(text)
        }
    }

    companion object {
        fun ComponentsAccessors.overviewItems() =
            pageableItems { container -> SaOverviewItem(container.locator(".overview-item__panel")) }
    }

    data class PrimaryAttribute(
        val icon: String,
        val text: String,
    )
}
