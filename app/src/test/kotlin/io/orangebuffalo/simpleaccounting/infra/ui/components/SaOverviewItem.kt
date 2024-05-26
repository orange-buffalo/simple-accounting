package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.simpleaccounting.infra.ui.components.SaPageableItems.Companion.pageableItems
import io.orangebuffalo.simpleaccounting.infra.utils.innerTextOrNull
import io.orangebuffalo.simpleaccounting.infra.utils.innerTextTrimmed
import io.orangebuffalo.simpleaccounting.infra.utils.shouldNotBeVisible

class SaOverviewItem private constructor(
    private val panel: Locator,
) {

    val title: String?
        get() = panel.locator(".overview-item__title").innerTextOrNull()

    val primaryAttributes : List<PrimaryAttribute>
        get() = panel.locator(".overview-item-primary-attribute").all().map {
            PrimaryAttribute(
                icon = it.locator(".overview-item-primary-attribute__icon").getAttribute("data-icon"),
                text = it.innerTextTrimmed(),
            )
        }

    private val detailsTrigger = panel.locator(".overview-item__details-trigger")

    fun shouldNotHaveDetails() {
        detailsTrigger.shouldNotBeVisible()
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
