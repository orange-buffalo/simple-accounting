package io.orangebuffalo.simpleaccounting.tests.ui.admin.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.kotest.matchers.string.shouldEndWith
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByContainer
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ComponentsAccessors
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemSelectByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel.Companion.statusLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponent

abstract class UserPageBase<T : UserPageBase<T>>(page: Page) : SaPageBase<T>(page) {
    val userName = components.formItemTextInputByLabel("Username")
    val role = components.formItemSelectByLabel("User role")
    val activationStatus = components.formItemByLabel("Activation status") { UserActivationStatus(it, components) }
    val saveButton = components.buttonByText("Save")
    val cancelButton = components.buttonByText("Cancel")
}

class CreateUserPage(page: Page) : UserPageBase<CreateUserPage>(page) {
    private val header = components.pageHeader("Create New User")

    fun shouldBeOpen() = header.shouldBeVisible()
}

class EditUserPage(page: Page) : UserPageBase<EditUserPage>(page) {
    private val header = components.pageHeader("Edit User")

    fun shouldBeOpen() = header.shouldBeVisible()
}

fun Page.shouldBeCreateUserPage(): CreateUserPage = CreateUserPage(this).shouldBeOpen()

fun Page.shouldBeEditUserPage(): EditUserPage = EditUserPage(this).shouldBeOpen()

class UserActivationStatus<T : UserPageBase<T>>(
    container: Locator,
    components: ComponentsAccessors<T>,
) : UiComponent<Unit, UserActivationStatus<T>>(Unit) {

    private val status = components.statusLabel(container.locator("xpath=.."))
    private val linkButton = components.buttonByContainer(container)

    fun shouldBeActivated() {
        status.shouldBeSimplifiedSuccess(null)
        linkButton.shouldBeHidden()
    }

    fun shouldBeNotActivated(tokenValue: String) {
        status.shouldBeSimplifiedPending(null)
        linkButton.shouldBeVisible()
        linkButton.shouldHaveLabelSatisfying { label ->
            label.shouldEndWith("/$tokenValue")
        }
    }
}
