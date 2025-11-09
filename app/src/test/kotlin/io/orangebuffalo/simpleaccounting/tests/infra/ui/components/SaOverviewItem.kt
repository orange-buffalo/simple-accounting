package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageableItems.Companion.pageableItems
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath
import io.orangebuffalo.simpleaccounting.tests.infra.utils.dataValues
import io.orangebuffalo.simpleaccounting.tests.infra.utils.innerTextOrNull
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

/* language=javascript */
private const val DATA_JS = """
    (panel) => {
        return {
            title: utils.getDynamicContent(panel.querySelector('.overview-item__title')),
            primaryAttributes: Array.from(panel.querySelectorAll('.overview-item-primary-attribute')).map(attr => {
              return utils.getDynamicContent(attr);
            }),
            middleColumnContent: utils.getDynamicContent(panel.querySelector('.overview-item__middle-column .sa-status-label')),
            lastColumnContent: utils.getDynamicContent(panel.querySelector('.overview-item__last-column')),
            attributePreviewIcons: Array.from(panel.querySelectorAll('.overview-item-attribute-preview-icon')).map(icon => {
              return utils.getDynamicContent(icon);
            }),
            hasDetails: panel.querySelector('.overview-item__details-trigger') !== null  
        };
    };
"""

class SaOverviewItem private constructor(
    private val panel: Locator,
) {

    /**
     * Returns a locator relative to this overview item panel.
     */
    fun locator(selector: String): Locator = panel.locator(selector)

    val title: String?
        get() = panel.locator(".overview-item__title").innerTextOrNull()

//    val primaryAttributes: List<PrimaryAttribute>
//        get() = panel.locator(".overview-item-primary-attribute").all().map {
//            PrimaryAttribute(
//                icon = it.locator(".overview-item-primary-attribute__icon").getAttribute("data-icon"),
//                text = it.innerTextOrNull() ?: "<primary attribute text is missing>",
//            )
//        }

    private val detailsTrigger = panel.locator(".overview-item__details-trigger")

    fun shouldHaveDetails(actions: List<String> = emptyList(), vararg sections: DetailsSectionSpec) {
        expandDetails()
        shouldSatisfy("Overview item details should match the expected specification") {
            val detailsContainer = panel.locator("..").locator(".overview-item__details")

            val actionsLocator = detailsContainer.locator(".overview-item-details-section-actions .sa-action-link")
            val actualActions = actionsLocator.all().map { it.innerTextOrNull() }
            actualActions.shouldContainExactly(actions)

            val actualSections = detailsContainer.locator(".overview-item-details-section")
                .all().map { actualSection ->
                    val sectionTitle = actualSection
                        .locator(".overview-item-details-section__title")
                        .innerTextOrNull()
                    DetailsSectionSpec(
                        title = sectionTitle ?: "<section title is missing>",
                        attributes = actualSection.locator(".overview-item-details-section-attribute")
                            .all().map { attributeEl ->
                                val label = attributeEl
                                    .locator(".overview-item-details-section-attribute__label")
                                    .innerTextOrNull().shouldNotBeNull()
                                val value = attributeEl
                                    // next element sibling
                                    .locator(".overview-item-details-section-attribute__label + div")
                                    .innerTextOrNull()
                                label to (value ?: "<attribute value is missing>")
                            }
                    )
                }
            actualSections.shouldContainExactly(sections.map { it.copy(title = it.title.uppercase()) })
        }
    }

    private fun expandDetails() {
        detailsTrigger.click()
    }

    fun executeAction(actionLinkText: String) {
        panel
            .locator("xpath=.//*[${XPath.hasClass("sa-action-link")} and .//*[${XPath.hasText(actionLinkText)}]]")
            .click()
    }

    companion object {
        fun ComponentsAccessors.overviewItems() =
            pageableItems(
                itemDataJs = DATA_JS,
                itemDataSerializer = serializer<SaOverviewItemData>(),
            ) { container -> SaOverviewItem(container.locator(".overview-item__panel")) }

        fun primaryAttribute(icon: SaIconType, text: String) = dataValues(SaIcon.iconValue(icon), text)

        fun previewIcons(vararg icons: SaIconType) = icons.map { SaIcon.iconValue(it) }
    }
}

@Serializable
data class SaOverviewItemData(
    val title: String? = null,
    val primaryAttributes: List<String> = emptyList(),
    val middleColumnContent: String? = null,
    val lastColumnContent: String? = null,
    val attributePreviewIcons: List<String> = emptyList(),
    val hasDetails: Boolean = true,
)

fun SaPageableItems<SaOverviewItem, SaOverviewItemData>.shouldHaveTitles(titles: List<String>) {
    shouldHaveDataSatisfying { items -> items.map { it.title }.shouldContainExactly(titles) }
}

fun SaPageableItems<SaOverviewItem, SaOverviewItemData>.shouldHaveTitles(vararg titles: String) {
    shouldHaveDataSatisfying { items -> items.map { it.title }.shouldContainExactly(*titles) }
}

data class DetailsSectionSpec(
    val title: String,
    val attributes: List<Pair<String, String>> = emptyList()
) {
    constructor(title: String, vararg attributes: Pair<String, String>) : this(title, attributes.toList())
}
