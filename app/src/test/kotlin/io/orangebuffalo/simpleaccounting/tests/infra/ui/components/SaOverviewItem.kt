package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageableItems.Companion.pageableItems
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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

/* language=javascript */
private const val DETAILS_DATA_JS = """
    (panel) => {
        const detailsContainer = panel.parentElement.querySelector('.overview-item__details');
        if (!detailsContainer) return null;

        const actions = Array.from(detailsContainer.querySelectorAll('.overview-item-details-section-actions .sa-action-link')).map(el => {
            return utils.getDynamicContent(el);
        });

        const sections = Array.from(detailsContainer.querySelectorAll('.overview-item-details-section')).map(section => {
            const titleEl = section.querySelector('.overview-item-details-section__title');
            const title = titleEl ? utils.getDynamicContent(titleEl) : null;

            const attributes = Array.from(section.querySelectorAll('.overview-item-details-section-attribute')).map(attr => {
                const labelEl = attr.querySelector('.overview-item-details-section-attribute__label');
                const label = labelEl ? utils.getDynamicContent(labelEl) : null;
                const valueEl = attr.querySelector('.overview-item-details-section-attribute__label + div');
                const value = valueEl ? utils.getDynamicContent(valueEl) : null;
                return [label, value];
            });

            // Extract free content from .col elements that are not inside .overview-item-details-section-attribute
            let content = null;
            const nonAttributeColElements = Array.from(section.querySelectorAll('.row > .col'))
                .filter(colEl => !colEl.closest('.overview-item-details-section-attribute'));
            
            if (nonAttributeColElements.length > 1) {
                content = '<multiple non-attribute content elements found, not supported>';
            } else if (nonAttributeColElements.length === 1) {
                content = utils.getDynamicContent(nonAttributeColElements[0]);
            }

            return { title: title.toUpperCase(), attributes, content };
        });

        return { actions, sections };
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

    private val detailsTrigger = panel.locator(".overview-item__details-trigger")

    fun shouldHaveDetails(actions: List<String> = emptyList(), vararg sections: DetailsSectionSpec) {
        expandDetails()
        shouldSatisfy("Overview item details should match the expected specification") {
            val detailsDataJson = panel.evaluate(
                """
                (panel) => {
                    ${injectJsUtils()}
                    const getDetailsData = $DETAILS_DATA_JS;
                    return JSON.stringify(getDetailsData(panel));
                }
                """,
            ) as String
            val detailsData = Json.decodeFromString<DetailsData>(detailsDataJson)
            detailsData.actions.shouldContainExactly(actions)
            val expectedSections = sections.map {
                DetailsSection(
                    it.title.uppercase(),
                    it.attributes.map { pair -> listOf(pair.first, pair.second) },
                    it.content
                )
            }
            detailsData.sections.shouldContainExactly(expectedSections)
        }
    }

    private fun expandDetails() {
        detailsTrigger.click()
    }

    fun executeAction(actionLinkText: String) {
        expandDetails()
        // The details div is a sibling of the panel, both within a parent container
        val container = panel.locator("xpath=..")
        container
            .locator("xpath=.//*[${XPath.hasClass("sa-action-link")} and .//*[${XPath.hasText(actionLinkText)}]]")
            .click()
    }

    fun executeEditAction() = executeAction("Edit")

    fun executeCopyAction() = executeAction("Copy")

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

@Serializable
private data class DetailsData(
    val actions: List<String>,
    val sections: List<DetailsSection>
)

@Serializable
private data class DetailsSection(
    val title: String?,
    val attributes: List<List<String>>,
    val content: String? = null
)

fun SaPageableItems<SaOverviewItem, SaOverviewItemData>.shouldHaveTitles(titles: List<String>) {
    shouldHaveDataSatisfying { items -> items.map { it.title }.shouldContainExactly(titles) }
}

fun SaPageableItems<SaOverviewItem, SaOverviewItemData>.shouldHaveTitles(vararg titles: String) {
    shouldHaveDataSatisfying { items -> items.map { it.title }.shouldContainExactly(*titles) }
}

data class DetailsSectionSpec(
    val title: String,
    val attributes: List<Pair<String, String>> = emptyList(),
    val content: String? = null
) {
    constructor(title: String, vararg attributes: Pair<String, String>) : this(title, attributes.toList(), null)
    constructor(title: String, content: String) : this(title, emptyList(), content)
}
