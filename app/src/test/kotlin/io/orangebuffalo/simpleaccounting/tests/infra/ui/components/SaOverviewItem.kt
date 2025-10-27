package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
    
    val amount: String?
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
        val actualSections = detailsContainer.locator(".sa-overview-item-details-section").all()
        
        actualSections.size.shouldBe(sections.size, "Expected ${sections.size} sections but found ${actualSections.size}")
        
        sections.forEachIndexed { index, expectedSection ->
            val actualSection = actualSections[index]
            val sectionTitle = actualSection.locator(".sa-overview-item-details-section__title").innerTextTrimmed()
            sectionTitle.shouldBe(expectedSection.title, "Section $index has wrong title")
            
            val actualAttributes = actualSection.locator(".sa-overview-item-details-section-attribute").all()
            actualAttributes.size.shouldBe(expectedSection.attributes.size, 
                "Section '${expectedSection.title}' should have ${expectedSection.attributes.size} attributes but has ${actualAttributes.size}")
            
            expectedSection.attributes.forEachIndexed { attrIndex, (expectedLabel, expectedValue) ->
                val actualAttribute = actualAttributes[attrIndex]
                val actualLabel = actualAttribute.locator(".sa-overview-item-details-section-attribute__label").innerTextTrimmed()
                val actualValue = actualAttribute.locator(".sa-overview-item-details-section-attribute__value").innerTextTrimmed()
                
                actualLabel.shouldBe(expectedLabel, "Attribute $attrIndex in section '${expectedSection.title}' has wrong label")
                actualValue.shouldBe(expectedValue, "Attribute '$expectedLabel' in section '${expectedSection.title}' has wrong value")
            }
        }
    }

    private fun expandDetails() {
        detailsTrigger.click()
    }

    fun detailsSection(title: String, spec: DetailsSection.() -> Unit) {
        val section = DetailsSection(panel.locator("xpath=.//*[${XPath.hasClass("overview-item__details")}]//*[${XPath.hasClass("sa-overview-item-details-section")} and .//*[${XPath.hasText(title)}]]"))
        section.spec()
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

    val statusLabelLocator: Locator
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

data class DetailsSectionSpec(
    val title: String,
    val attributes: List<Pair<String, String>>
) {
    constructor(title: String, vararg attributes: Pair<String, String>) : this(title, attributes.toList())
}

@UiComponentMarker
class DetailsSection internal constructor(
    private val section: Locator,
) {
    fun shouldHaveAttribute(label: String, value: String) {
        val attribute = section.locator("xpath=.//*[${XPath.hasClass("sa-overview-item-details-section-attribute")} and .//*[${XPath.hasText(label)}]]")
        attribute.locator(".sa-overview-item-details-section-attribute__value").shouldHaveText(value)
    }

    fun shouldHaveAttributeContaining(label: String, value: String) {
        val attribute = section.locator("xpath=.//*[${XPath.hasClass("sa-overview-item-details-section-attribute")} and .//*[${XPath.hasText(label)}]]")
        val actualText = attribute.locator(".sa-overview-item-details-section-attribute__value").innerTextTrimmed()
        actualText.shouldContain(value)
    }
}
