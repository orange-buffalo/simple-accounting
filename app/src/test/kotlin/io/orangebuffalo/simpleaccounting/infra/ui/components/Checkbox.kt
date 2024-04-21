package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.simpleaccounting.infra.utils.assertThat

class Checkbox<P : Any> private constructor(
    private val locator: Locator,
    parent: P,
) : UiComponent<P, Checkbox<P>>(parent) {
    fun shouldBeChecked() = locator.assertThat().isChecked()

    companion object {
        fun <T : SaPageBase<T>> ComponentsAccessors<T>.checkboxByOwnLabel(label: String) =
            Checkbox(page.getByLabel(label), this.owner)
    }
}
