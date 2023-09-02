package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.simpleaccounting.infra.utils.assertThat

class Checkbox<T : Any>(
    private val locator: Locator,
    private val parent: T,
) {
    fun shouldBeChecked() = locator.assertThat().isChecked()

    operator fun invoke(action: Checkbox<*>.() -> Unit): T {
        this.action()
        return parent
    }
}

fun <T : SaPageBase<T>> ComponentsAccessors<T>.checkboxByOwnLabel(label: String) =
    Checkbox(page.getByLabel(label), this.owner)
