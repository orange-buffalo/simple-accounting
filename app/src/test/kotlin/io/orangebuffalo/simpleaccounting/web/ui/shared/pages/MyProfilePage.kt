package io.orangebuffalo.simpleaccounting.web.ui.shared.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.ui.components.*

class MyProfilePage(page: Page) : SaPageBase<MyProfilePage>(page) {
    private val header = components.pageHeader("My Profile")
    val currentPassword = components.formItemByLabel("Current Password", TextInput::class)
    val newPassword = components.formItemByLabel("New Password", TextInput::class)
    val newPasswordConfirmation = components.formItemByLabel("New Password Confirmation", TextInput::class)
    val changePasswordButton = components.buttonByText("Apply new password")

    fun shouldBeOpen(): MyProfilePage = header.shouldBeVisible()
}

fun Page.shouldBeMyProfilePage(): MyProfilePage = MyProfilePage(this).shouldBeOpen()
