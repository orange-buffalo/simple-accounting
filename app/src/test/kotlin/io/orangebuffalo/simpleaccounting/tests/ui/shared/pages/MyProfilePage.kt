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
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemSelectByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SectionHeader.Companion.sectionHeader

class MyProfilePage private constructor(page: Page) : SaPageBase(page) {
    private val container = page.locator(".my-profile-page")
    private val header = components.pageHeader("My Profile")
    private val passwordChangeForm = PasswordChangeForm(page, components)
    private val documentsStorageSection = DocumentStorageSection(components)
    private val languagePreferencesSection = LanguagePreferencesSection(components)

    private fun shouldBeOpen() {
        header.shouldBeVisible()
    }

    fun reportRendering(name: String) {
        container.reportRendering(name)
    }

    fun shouldHavePasswordChangeSectionVisible(spec: PasswordChangeForm.() -> Unit = {}) {
        passwordChangeForm.shouldBeVisible()
        passwordChangeForm.spec()
    }

    fun shouldHaveLanguagePreferencesSectionVisible(spec: LanguagePreferencesSection.() -> Unit = {}) {
        languagePreferencesSection.shouldBeVisible()
        languagePreferencesSection.spec()
    }
    
    fun languagePreferencesSection(spec: LanguagePreferencesSection.() -> Unit) {
        languagePreferencesSection.spec()
    }

    fun shouldHaveDocumentsStorageSectionVisible(spec: DocumentStorageSection.() -> Unit = {}) {
        documentsStorageSection.shouldBeVisible()
        documentsStorageSection.spec()
    }

    fun shouldHaveDocumentsStorageSectionHidden() {
        documentsStorageSection.shouldBeHidden()
    }

    @UiComponentMarker
    class PasswordChangeForm(page: Page, components: ComponentsAccessors) {
        val currentPassword = components.formItemTextInputByLabel("Current Password")
        val newPassword = components.formItemTextInputByLabel("New Password")
        val newPasswordConfirmation = components.formItemTextInputByLabel("New Password Confirmation")
        val changePasswordButton = components.buttonByText("Apply new password")
        private val passwordChangeSectionHeader =
            page.locator("//*[contains(@class, 'el-form')]//h2[text()='Change Password']")
        private val container = page.locator(".my-profile-page")

        fun shouldBeVisible() {
            passwordChangeSectionHeader.shouldBeVisible()
        }

        fun reportRendering(name: String) {
            container.reportRendering(name)
        }
    }

    @UiComponentMarker
    class DocumentStorageSection(components: ComponentsAccessors) {
        private val documentStorageSectionHeader =
           components.page.locator("//*[contains(@class, 'el-form')]//h2[text()='Documents Storage']")
        private val googleDriveConfig =
            DocumentStorageConfig(components, "google-drive", "Google Drive") { container ->
                GoogleDriveSettings(components, container)
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
            components: ComponentsAccessors,
            storageId: String,
            private val title: String,
            settingsProvider: (Locator) -> T,
        ) : UiComponent<DocumentStorageConfig<T>>() {
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
            components: ComponentsAccessors,
            parentEl: Locator
        ) : UiComponent<GoogleDriveSettings>() {
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

    @UiComponentMarker
    class LanguagePreferencesSection(components: ComponentsAccessors) {
        val sectionHeader = components.sectionHeader("Language Preferences")
        val ukrainianSectionHeader = components.sectionHeader("Мовні Уподобання")
        val language = components.formItemSelectByLabel("Interface Language")
        val locale = components.formItemSelectByLabel("Language to display dates, amounts, etc")
        private val container = components.page.locator(".my-profile-page")

        fun shouldBeVisible() {
            sectionHeader.shouldBeVisible()
        }

        fun reportRendering(name: String) {
            container.reportRendering(name)
        }
    }

    companion object {
        fun Page.openMyProfilePage(spec: MyProfilePage.() -> Unit) {
            navigate("/my-profile")
            MyProfilePage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
