package io.orangebuffalo.simpleaccounting.business.ui.user.documents

import com.microsoft.playwright.Page
import com.microsoft.playwright.Locator
import com.microsoft.playwright.options.AriaRole
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveCount
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Checkbox
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.TextInput
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponent
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponentMarker
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
    val filters = DocumentFilterChips(page.locator(".document-filters"))
    val uploadButton = components.buttonByText("Upload")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
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
class DocumentFilterChips(private val container: Locator) : UiComponent<DocumentFilterChips>() {
    private val editor = container.locator(".document-filters__editor")

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
        if (button.getAttribute("aria-expanded") != "true") button.click()
    }

    fun openWithKeyboard(label: String): DocumentFilterChips {
        chipButton(label).press("Enter")
        return this
    }

    fun closeWithEscape(): DocumentFilterChips {
        editor.getByRole(AriaRole.TEXTBOX).press("Escape")
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
        container.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Clear $label").setExact(true))
            .press("Enter")
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
                summary: chip.querySelector('strong').textContent.trim(),
                active: chip.classList.contains('document-filters__chip--active')
            })))
        """) as String
        return Json.decodeFromString(json)
    }

    fun clear(label: String): DocumentFilterChips {
        container.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Clear $label").setExact(true)).click()
        return this
    }

    fun clearAll(): DocumentFilterChips {
        container.getByRole(AriaRole.BUTTON, Locator.GetByRoleOptions().setName("Clear All").setExact(true)).click()
        return this
    }
}

@Serializable
data class DocumentFilterChipData(val label: String, val summary: String, val active: Boolean)
