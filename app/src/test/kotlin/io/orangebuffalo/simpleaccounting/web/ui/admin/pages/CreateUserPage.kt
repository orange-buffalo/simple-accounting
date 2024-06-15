package io.orangebuffalo.simpleaccounting.web.ui.admin.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.infra.ui.components.FormItem.Companion.formItemSelectByLabel
import io.orangebuffalo.simpleaccounting.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.infra.ui.components.SaPageBase

class CreateUserPage (page: Page) : SaPageBase<CreateUserPage>(page) {
    private val header = components.pageHeader("Create New User")
    val userName = components.formItemTextInputByLabel("Username")
    val role = components.formItemSelectByLabel("User role")
    val saveButton = components.buttonByText("Save")
    val cancelButton = components.buttonByText("Cancel")

    fun shouldBeOpen() = header.shouldBeVisible()
}

fun Page.shouldBeCreateUserPage(): CreateUserPage = CreateUserPage(this).shouldBeOpen()
