package io.orangebuffalo.simpleaccounting.web.ui.shared.pages

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.infra.ui.components.ComponentsAccessors
import io.orangebuffalo.simpleaccounting.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.infra.utils.assertThat

class MyProfilePage(page: Page) : SaPageBase<MyProfilePage>(page) {
    private val header = components.pageHeader("My Profile")
    private val passwordChangeForm = PasswordChangeForm(page, components)
    private val documentsStorageSection = DocumentStorageSection(page)
    private val languagePreferencesSection = LanguagePreferencesSection(page)

    fun shouldBeOpen(): MyProfilePage = header.shouldBeVisible()

    fun shouldHavePasswordChangeSectionVisible(): MyProfilePage {
        passwordChangeForm.shouldBeVisible()
        return this
    }

    fun shouldHaveLanguagePreferencesSectionVisible(): MyProfilePage {
        languagePreferencesSection.shouldBeVisible()
        return this
    }

    fun shouldHavePasswordChangeSectionVisible(spec: PasswordChangeForm.() -> Unit): MyProfilePage {
        passwordChangeForm.shouldBeVisible()
        passwordChangeForm.spec()
        return this
    }

    fun shouldHaveDocumentsStorageSectionVisible(): MyProfilePage {
        documentsStorageSection.shouldBeVisible()
        return this
    }

    fun shouldHaveDocumentsStorageSectionHidden(): MyProfilePage {
        documentsStorageSection.shouldBeHidden()
        return this
    }

    class PasswordChangeForm(page: Page, components: ComponentsAccessors<MyProfilePage>) {
        val currentPassword = components.formItemTextInputByLabel("Current Password")
        val newPassword = components.formItemTextInputByLabel("New Password")
        val newPasswordConfirmation = components.formItemTextInputByLabel("New Password Confirmation")
        val changePasswordButton = components.buttonByText("Apply new password")
        private val passwordChangeSectionHeader =
            page.locator("//*[contains(@class, 'el-form')]//h2[text()='Change Password']")

        fun shouldBeVisible() {
            passwordChangeSectionHeader.assertThat().isVisible()
        }
    }

    class DocumentStorageSection(page: Page) {
        private val documentStorageSectionHeader =
            page.locator("//*[contains(@class, 'el-form')]//h2[text()='Documents Storage']")

        fun shouldBeVisible() {
            documentStorageSectionHeader.assertThat().isVisible()
        }

        fun shouldBeHidden() {
            documentStorageSectionHeader.assertThat().isHidden()
        }
    }

    class LanguagePreferencesSection(page: Page) {
        private val languagePreferencesSectionHeader =
            page.locator("//*[contains(@class, 'el-form')]//h2[text()='Language Preferences']")

        fun shouldBeVisible() {
            languagePreferencesSectionHeader.assertThat().isVisible()
        }
    }
}

fun Page.shouldBeMyProfilePage(): MyProfilePage = MyProfilePage(this).shouldBeOpen()
