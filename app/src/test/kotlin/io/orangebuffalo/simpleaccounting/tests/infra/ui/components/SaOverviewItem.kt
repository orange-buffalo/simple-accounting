package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageableItems.Companion.pageableItems
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath
import io.orangebuffalo.simpleaccounting.tests.infra.utils.innerTextOrNull
import io.orangebuffalo.simpleaccounting.tests.infra.utils.innerTextTrimmed

class SaOverviewItem private constructor(
    private val panel: Locator,
) {

    /**
     * Returns a locator relative to this overview item panel.
     */
    fun locator(selector: String): Locator = panel.locator(selector)

    val title: String?
        get() = panel.locator(".overview-item__title").innerTextOrNull()
    
    val lastColumnContent: String?
        get() = panel.locator(".overview-item__last-column").innerTextOrNull()

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

    fun shouldHaveDetails(vararg sections: DetailsSectionSpec) {
        expandDetails()
        val detailsContainer = panel.locator(".overview-item__details")
        
        // First verify actions if any section has them
        val allActions = sections.flatMap { it.actions }
        if (allActions.isNotEmpty()) {
            val actionsLocator = detailsContainer.locator(".sa-action-link")
            actionsLocator.shouldHaveCount(allActions.size)
            val actualActions = actionsLocator.all().map { it.innerTextTrimmed() }
            actualActions.shouldContainExactly(allActions)
        }
        
        val sectionsLocator = detailsContainer.locator(".sa-overview-item-details-section")
        
        // Use Playwright assertion to wait for the correct number of sections
        sectionsLocator.shouldHaveCount(sections.size)
        
        val actualSections = sectionsLocator.all()
        
        sections.forEachIndexed { index, expectedSection ->
            val actualSection = actualSections[index]
            val sectionTitle = actualSection.locator(".sa-overview-item-details-section__title").innerTextOrNull()
            sectionTitle.shouldBe(expectedSection.title, "Section $index has wrong title")
            
            val attributesLocator = actualSection.locator(".sa-overview-item-details-section-attribute")
            
            // Use Playwright assertion to wait for the correct number of attributes
            attributesLocator.shouldHaveCount(expectedSection.attributes.size)
            
            val actualAttributes = attributesLocator.all()
            
            expectedSection.attributes.forEachIndexed { attrIndex, (expectedLabel, expectedValue) ->
                val actualAttribute = actualAttributes[attrIndex]
                val actualLabel = actualAttribute.locator(".sa-overview-item-details-section-attribute__label").innerTextOrNull()
                val actualValue = actualAttribute.locator(".sa-overview-item-details-section-attribute__value").innerTextOrNull()
                
                actualLabel.shouldBe(expectedLabel, "Attribute $attrIndex in section '${expectedSection.title}' has wrong label")
                actualValue.shouldBe(expectedValue, "Attribute '$expectedLabel' in section '${expectedSection.title}' has wrong value")
            }
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

    val attributePreviewIcons: List<String>
        get() = panel.locator(".overview-item__attribute-preview .sa-icon")
            .all()
            .map { it.getAttribute("data-icon") }

    companion object {
        fun ComponentsAccessors.overviewItems() =
            pageableItems { container -> SaOverviewItem(container.locator(".overview-item__panel")) }
    }

    data class PrimaryAttribute(
        val icon: String,
        val text: String,
    )
}

data class DetailsSectionSpec(
    val title: String,
    val attributes: List<Pair<String, String>> = emptyList(),
    val actions: List<String> = emptyList()
)

