package io.orangebuffalo.simpleaccounting.web.ui.admin.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.infra.ui.components.FormItem.Companion.formItemSelectByLabel
import io.orangebuffalo.simpleaccounting.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.infra.ui.components.SaPageBase

abstract class UserPageBase<T : UserPageBase<T>>(page: Page) : SaPageBase<T>(page) {
    val userName = components.formItemTextInputByLabel("Username")
    val role = components.formItemSelectByLabel("User role")
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
