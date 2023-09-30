package io.orangebuffalo.simpleaccounting.web.ui.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.infra.ui.components.TextInput
import io.orangebuffalo.simpleaccounting.infra.ui.components.buttonByText
import io.orangebuffalo.simpleaccounting.infra.ui.components.formItemByLabel
import io.orangebuffalo.simpleaccounting.infra.utils.assertThat

class MyProfilePage(page: Page) : SaPageBase<MyProfilePage>(page) {
    private val header = page.locator("//h1[normalize-space(.) = 'My Profile']")
    val currentPassword = components.formItemByLabel("Current Password", TextInput::class)
    val newPassword = components.formItemByLabel("New Password", TextInput::class)
    val newPasswordConfirmation = components.formItemByLabel("New Password Confirmation", TextInput::class)
    val changePasswordButton = components.buttonByText("Apply new password")

    fun shouldBeOpen(): MyProfilePage {
        header.assertThat().isVisible()
        return this
    }
}

fun Page.shouldBeMyProfilePage(): MyProfilePage = MyProfilePage(this).shouldBeOpen()
