package io.orangebuffalo.simpleaccounting.tests.ui.shared.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ComponentsAccessors
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel.Companion.statusLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Switch.Companion.switchByContainer
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponent
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponentMarker

class MyProfilePage(page: Page) : SaPageBase<MyProfilePage>(page) {
    private val header = components.pageHeader("My Profile")
    private val passwordChangeForm = PasswordChangeForm(page, components)
    private val documentsStorageSection = DocumentStorageSection(components)
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

    fun shouldHaveDocumentsStorageSectionVisible(spec: DocumentStorageSection.() -> Unit): MyProfilePage {
        documentsStorageSection.shouldBeVisible()
        documentsStorageSection.spec()
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
            passwordChangeSectionHeader.shouldBeVisible()
        }
    }

    @UiComponentMarker
    class DocumentStorageSection(components: ComponentsAccessors<MyProfilePage>) {
        private val documentStorageSectionHeader =
           components.page.locator("//*[contains(@class, 'el-form')]//h2[text()='Documents Storage']")
        private val googleDriveConfig =
            DocumentStorageConfig(components, "google-drive", "Google Drive") { container ->
                GoogleDriveSettings(components, container, this)
            }

        fun shouldBeVisible() {
            documentStorageSectionHeader.shouldBeVisible()
        }

        fun shouldHaveGoogleDriveConfigVisible(spec: DocumentStorageConfig<GoogleDriveSettings>.() -> Unit) {
            googleDriveConfig.shouldBeVisible()
            googleDriveConfig.spec()
        }

        fun shouldBeHidden() {
            documentStorageSectionHeader.shouldBeHidden()
        }

        class DocumentStorageConfig<T>(
            components: ComponentsAccessors<MyProfilePage>,
            storageId: String,
            private val title: String,
            settingsProvider: (Locator) -> T,
        ) : UiComponent<MyProfilePage, DocumentStorageConfig<T>>(components.owner) {
            private val container = components.page.locator("#storage-config_$storageId")
            private val header = container.locator("h4")
            val switch = components.switchByContainer(container)
            val settings = settingsProvider(container)

            fun shouldBeVisible() {
                container.shouldBeVisible()
                header.shouldHaveText(title)
                switch.shouldBeVisible()
            }

            fun reportRendering(name: String) {
                container.reportRendering(name)
            }
        }

        class GoogleDriveSettings(
            components: ComponentsAccessors<MyProfilePage>,
            parentEl: Locator,
            parent: DocumentStorageSection
        ) : UiComponent<DocumentStorageSection, GoogleDriveSettings>(parent) {
            private val container = parentEl.locator(".sa-gdrive-integration")
            val status = components.statusLabel(container)
            val startAuthorizationButton = components.buttonByText("Start authorization now")
            val detailsMessage: Locator = container.locator(".sa-gdrive-integration__status__details")

            fun shouldBeHidden() {
                container.shouldBeHidden()
            }

            fun shouldBeVisible() {
                container.shouldBeVisible()
            }
        }
    }

    class LanguagePreferencesSection(page: Page) {
        private val languagePreferencesSectionHeader =
            page.locator("//*[contains(@class, 'el-form')]//h2[text()='Language Preferences']")

        fun shouldBeVisible() {
            languagePreferencesSectionHeader.shouldBeVisible()
        }
    }
}

fun Page.openMyProfilePage(): MyProfilePage {
    this.navigate("/my-profile")
    return MyProfilePage(this).shouldBeOpen()
}
