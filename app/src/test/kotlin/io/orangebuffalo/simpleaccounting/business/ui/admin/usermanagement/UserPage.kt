package io.orangebuffalo.simpleaccounting.business.ui.admin.usermanagement

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

abstract class UserPageBase(page: Page) : SaPageBase(page) {
    val userName = components.formItemTextInputByLabel("Username")
    val role = components.formItemSelectByLabel("User role")
    val activationStatus = components.formItemByLabel("Activation status") { UserActivationStatus(it, components) }
    val saveButton = components.buttonByText("Save")
    val cancelButton = components.buttonByText("Cancel")
}

class CreateUserPage private constructor(page: Page) : UserPageBase(page) {
    private val header = components.pageHeader("Create New User")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeCreateUserPage(spec: CreateUserPage.() -> Unit) {
            CreateUserPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}

class EditUserPage private constructor(page: Page) : UserPageBase(page) {
    private val header = components.pageHeader("Edit User")

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    companion object {
        fun Page.shouldBeEditUserPage(spec: EditUserPage.() -> Unit) {
            EditUserPage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}

class UserActivationStatus(
    container: Locator,
    components: ComponentsAccessors,
) : UiComponent<UserActivationStatus>() {

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
