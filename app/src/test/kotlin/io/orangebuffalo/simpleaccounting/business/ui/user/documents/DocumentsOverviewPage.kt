package io.orangebuffalo.simpleaccounting.business.ui.user.documents

import com.microsoft.playwright.Page
import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.AriaRole
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Checkbox
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.TextInput
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponent
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponentMarker
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveTitles
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ConfirmationDialog.Companion.confirmationDialog
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.overviewItems
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.regex.Pattern

class DocumentsOverviewPage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("Documents")
    val pageItems = components.overviewItems()
    val filters = DocumentFilterChips(container = page.locator(".document-filters"), page = page)
    val uploadButton = components.buttonByText("Upload")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun shouldHaveNoMatchingDocuments() {
        pageItems.shouldHaveTitles()
        page.locator(".sa-pageable-items__empty-results").also {
            it.shouldBeVisible()
            it.shouldHaveText("No results here")
        }
    }

    fun confirmDocumentDeletion() {
        components.confirmationDialog()
            .shouldBeVisible()
            .clickButton("Delete")
    }

    fun reportRenderingWithPopovers(name: String) {
        page.locator("body").reportRendering(name)
    }

    companion object {
        fun Page.shouldBeDocumentsOverviewPage(spec: DocumentsOverviewPage.() -> Unit = {}) {
            DocumentsOverviewPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }

        fun Page.openDocumentsOverviewPage(spec: DocumentsOverviewPage.() -> Unit = {}) {
            navigate("/documents")
            shouldBeDocumentsOverviewPage(spec)
        }
    }
}

@UiComponentMarker
class DocumentFilterChips(private val container: Locator, private val page: Page) : UiComponent<DocumentFilterChips>() {
    private val editor = page.locator(".document-filters__editor")

    private fun chipButton(label: String): Locator {
        val chipLabel = if (label == "File name or usage title") "Search" else label
        container.shouldSatisfy {
            snapshot().filter { it.label == chipLabel }.shouldHaveSize(1)
        }
        val button = container.locator(".document-filters__chip > button:first-child > span")
            .filter(Locator.FilterOptions().setHasText(Pattern.compile("^$chipLabel:$")))
            .locator("..")
        button.shouldHaveCount(1)
        return button
    }

    private fun open(label: String) {
        val button = chipButton(label)
        if (button.getAttribute("aria-expanded") != "true") {
            val resultsTop = page.locator(".sa-pageable-items").boundingBox()!!.y
            button.click()
            editor.shouldHaveCount(1)
            container.shouldSatisfy {
                page.locator(".sa-pageable-items").boundingBox()!!.y.shouldBe(resultsTop)
            }
        }
    }

    fun openWithKeyboard(label: String): DocumentFilterChips {
        val resultsTop = page.locator(".sa-pageable-items").boundingBox()!!.y
        chipButton(label).press("Enter")
        editor.shouldHaveCount(1)
        editor.locator("input").shouldSatisfy {
            evaluate("element => element === document.activeElement").shouldBe(true)
        }
        container.shouldSatisfy {
            page.locator(".sa-pageable-items").boundingBox()!!.y.shouldBe(resultsTop)
        }
        return this
    }

    fun closeWithEscape(): DocumentFilterChips {
        editor.getByRole(AriaRole.TEXTBOX).press("Escape")
        editor.shouldHaveCount(0)
        chipButton("Search").shouldSatisfy {
            evaluate("element => element === document.activeElement").shouldBe(true)
        }
        return this
    }

    fun closeWithButton(label: String): DocumentFilterChips {
        open(label)
        editor.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Close").setExact(true)).click()
        editor.shouldHaveCount(0)
        chipButton(label).shouldSatisfy {
            evaluate("element => element === document.activeElement").shouldBe(true)
        }
        return this
    }

    fun closeByOutsideClick(): DocumentFilterChips {
        page.getByRole(AriaRole.HEADING, Page.GetByRoleOptions().setName("Documents").setExact(true)).click()
        editor.shouldHaveCount(0)
        return this
    }

    fun addTextFilter(label: String, value: String): DocumentFilterChips {
        open(label)
        TextInput.byContainer(editor).fill(value)
        return this
    }

    fun addSelectFilter(label: String, value: String): DocumentFilterChips {
        open(label)
        Checkbox.checkboxByOwnLabel(container = editor, label = value).click()
        return this
    }

    fun toggleOptionWithKeyboard(label: String, value: String): DocumentFilterChips {
        open(label)
        editor.getByRole(AriaRole.CHECKBOX, Locator.GetByRoleOptions().setName(value).setExact(true)).press("Space")
        return this
    }

    fun clearWithKeyboard(label: String): DocumentFilterChips {
        removeButton(label = label, value = singleValue(label)).press("Enter")
        return this
    }

    fun shouldHaveSelectOptions(label: String, vararg values: String): DocumentFilterChips {
        open(label)
        editor.locator(".el-checkbox__label").shouldSatisfy {
            allInnerTexts().shouldContainExactly(*values)
        }
        return this
    }

    fun shouldHaveChips(vararg values: DocumentFilterChipData): DocumentFilterChips {
        container.shouldSatisfy {
            snapshot().shouldContainExactly(*values)
        }
        return this
    }

    private fun snapshot(): List<DocumentFilterChipData> {
        val json = container.locator(".document-filters__chip").evaluateAll("""
            chips => JSON.stringify(chips.map(chip => ({
                label: chip.querySelector('button > span').textContent.trim().replace(/:$/, ''),
                summary: Array.from(chip.querySelectorAll('strong')).map(value => value.textContent.trim()).join(', '),
                active: chip.classList.contains('document-filters__chip--active')
            })))
        """) as String
        return Json.decodeFromString(json)
    }

    fun clear(label: String): DocumentFilterChips {
        removeValue(label = label, value = singleValue(label))
        return this
    }

    private fun singleValue(label: String): String = snapshot().single { it.label == label }.summary

    private fun removeButton(label: String, value: String): Locator {
        container.shouldSatisfy {
            snapshot().filter { it.label == label }.shouldHaveSize(1)
        }
        val values = chipButton(label).locator("..").locator(".document-filters__value strong")
        values.shouldSatisfy {
            allTextContents().filter { it.trim() == value }.shouldHaveSize(1)
        }
        return container.getByRole(
            AriaRole.BUTTON,
            Locator.GetByRoleOptions().setName("Remove $label: $value").setExact(true),
        ).also { it.shouldHaveCount(1) }
    }

    fun removeValue(label: String, value: String): DocumentFilterChips {
        removeButton(label = label, value = value).click()
        return this
    }

    fun clearAll(): DocumentFilterChips {
        container.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Clear All").setExact(true)).click()
        return this
    }
}

@Serializable
data class DocumentFilterChipData(val label: String, val summary: String, val active: Boolean)
