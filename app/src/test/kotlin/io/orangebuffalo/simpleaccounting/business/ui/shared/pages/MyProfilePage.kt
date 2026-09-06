package io.orangebuffalo.simpleaccounting.business.ui.shared.pages

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.reportRendering
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByContainer
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Button.Companion.buttonByText
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ComponentsAccessors
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ConfirmationDialog
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.ConfirmationDialog.Companion.confirmationDialog
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemTextInputByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.PageHeader.Companion.pageHeader
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel.Companion.statusLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponent
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.UiComponentMarker
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.FormItem.Companion.formItemSelectByLabel
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SectionHeader.Companion.sectionHeader

class MyProfilePage private constructor(page: Page) : SaPageBase(page) {
    private val header = components.pageHeader("My Profile")
    private val passwordChangeForm = PasswordChangeForm(components)
    private val documentsStorageSection = DocumentStorageSection(components)
    private val languagePreferencesSection = LanguagePreferencesSection(components)
    private val oauthProvidersSection = OAuthProvidersSection(components)

    private fun shouldBeOpen() {
        header.shouldBeVisible()
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

    fun shouldHaveOAuthProvidersSectionVisible(spec: OAuthProvidersSection.() -> Unit = {}) {
        oauthProvidersSection.shouldBeVisible()
        oauthProvidersSection.spec()
    }

    fun shouldHaveOAuthProvidersSectionHidden() {
        oauthProvidersSection.shouldBeHidden()
    }

    fun shouldHavePasswordChangeSectionHidden() {
        passwordChangeForm.shouldBeHidden()
    }

    fun shouldHaveUnlinkConfirmation(message: String, spec: ConfirmationDialog.() -> Unit) {
        components.confirmationDialog().shouldBeVisible().apply {
            shouldHaveMessage(message)
            spec()
        }
    }

    @UiComponentMarker
    class PasswordChangeForm(components: ComponentsAccessors) {
        val currentPassword = components.formItemTextInputByLabel("Current Password")
        val newPassword = components.formItemTextInputByLabel("New Password")
        val newPasswordConfirmation = components.formItemTextInputByLabel("New Password Confirmation")
        val changePasswordButton = components.buttonByText("Apply new password")
        private val passwordChangeSectionHeader = components.sectionHeader("Change Password")

        fun shouldBeVisible() {
            passwordChangeSectionHeader.shouldBeVisible()
        }

        fun shouldBeHidden() {
            passwordChangeSectionHeader.shouldBeHidden()
        }
    }

    @UiComponentMarker
    class DocumentStorageSection(components: ComponentsAccessors) {
        private val documentStorageSectionHeader = components.sectionHeader("Documents Storage")
        val googleDriveConfig = StorageSubSection(components, "google-drive", "Google Drive") { container ->
            GoogleDriveSettings(components, container)
        }
        val localStorageConfig = StorageSubSection(components, "local-fs", "Local Storage") { container ->
            LocalStorageSettings(container)
        }

        fun shouldBeVisible() {
            documentStorageSectionHeader.shouldBeVisible()
        }

        fun shouldBeHidden() {
            documentStorageSectionHeader.shouldBeHidden()
        }

        fun shouldHaveGoogleDriveConfigVisible(spec: StorageSubSection<GoogleDriveSettings>.() -> Unit) {
            googleDriveConfig.shouldBeVisible()
            googleDriveConfig.spec()
        }

        fun shouldHaveLocalStorageConfigVisible(spec: StorageSubSection<LocalStorageSettings>.() -> Unit) {
            localStorageConfig.shouldBeVisible()
            localStorageConfig.spec()
        }

        class StorageSubSection<T>(
            components: ComponentsAccessors,
            storageId: String,
            private val title: String,
            settingsProvider: (Locator) -> T,
        ) : UiComponent<StorageSubSection<T>>() {
            private val container = components.page.locator("#storage-config_$storageId")
            private val nameEl = container.locator(".sa-documents-storage-item__name")
            private val headerEl = container.locator(".sa-documents-storage-item__header")
            val uploadStatusLabel = components.statusLabel(headerEl)
            val useForUploadsButton = container.locator(".sa-documents-storage-item__use-action")
            val settings = settingsProvider(container)
            val infoMessage = container.locator(".sa-documents-storage-section__storage-info")
            val migrationLink = infoMessage.getByText("Documents Migration")

            fun shouldBeVisible() {
                container.shouldBeVisible()
                nameEl.shouldHaveText(title)
            }

            fun shouldHaveUsedForUploadsStatus() {
                uploadStatusLabel.shouldBeSuccess("Used for uploads")
            }

            fun shouldHaveUseForUploadsAction() {
                useForUploadsButton.shouldBeVisible()
                useForUploadsButton.shouldHaveText("Use for uploads")
            }

            fun clickUseForUploads() {
                useForUploadsButton.click()
            }

            fun clickMigrationLink() {
                migrationLink.click()
            }

            fun shouldHaveNotAvailableStatus() {
                uploadStatusLabel.shouldBeRegular("Not available")
            }

            fun reportRendering(name: String) {
                container.reportRendering(name)
            }
        }

        class GoogleDriveSettings(
            components: ComponentsAccessors,
            parentEl: Locator,
        ) : UiComponent<GoogleDriveSettings>() {
            private val container = parentEl.locator(".sa-gdrive-integration")
            val status = components.statusLabel(container)
            val startAuthorizationButton = components.buttonByText("Start authorization now")
            val retryAuthorizationButton = components.buttonByText("Try again")
            val detailsMessage: Locator = container.locator(".sa-gdrive-integration__status__details")

            fun shouldBeHidden() {
                container.shouldBeHidden()
            }

            fun shouldBeVisible() {
                container.shouldBeVisible()
            }
        }

        @UiComponentMarker
        class LocalStorageSettings(
            parentEl: Locator,
        ) {
            private val container = parentEl
        }
    }

    @UiComponentMarker
    class OAuthProvidersSection(private val components: ComponentsAccessors) {
        private val sectionHeader = components.sectionHeader("Authentication Providers")
        private val container = components.page.locator(".sa-oauth-providers-section")

        fun shouldBeVisible() {
            sectionHeader.shouldBeVisible()
        }

        fun shouldBeHidden() {
            sectionHeader.shouldBeHidden()
        }

        fun provider(providerId: String, spec: OAuthProviderLink.() -> Unit) {
            OAuthProviderLink(components, providerId).spec()
        }

        fun reportRendering(name: String) {
            container.reportRendering(name)
        }
    }

    class OAuthProviderLink(
        components: ComponentsAccessors,
        providerId: String,
    ) : UiComponent<OAuthProviderLink>() {
        private val container = components.page.locator(
            ".sa-oauth-providers-section__provider[data-provider-id='$providerId']"
        )
        private val nameEl = container.locator(".sa-oauth-providers-section__provider-name")
        private val status = components.statusLabel(container)
        private val actionButton = components.buttonByContainer(container)

        fun shouldHaveName(name: String) {
            nameEl.shouldHaveText(name)
        }

        fun shouldBeLinkedAs(externalId: String) {
            status.shouldBeSuccess("Linked as $externalId")
            actionButton.shouldHaveLabelSatisfying { it.shouldBe("Unlink account") }
        }

        fun shouldNotBeLinked() {
            status.shouldBeRegular("Not linked")
            actionButton.shouldHaveLabelSatisfying { it.shouldBe("Link account") }
        }

        fun clickAction() {
            actionButton.click()
        }
    }

    @UiComponentMarker
    class LanguagePreferencesSection(components: ComponentsAccessors) {
        val sectionHeader = components.sectionHeader("Language Preferences")
        val ukrainianSectionHeader = components.sectionHeader("Мовні Уподобання")
        val language = components.formItemSelectByLabel("Interface Language")
        val locale = components.formItemSelectByLabel("Language to display dates, amounts, etc")

        fun shouldBeVisible() {
            sectionHeader.shouldBeVisible()
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

        fun Page.shouldBeMyProfilePage(spec: MyProfilePage.() -> Unit = {}) {
            MyProfilePage(this).apply {
                shouldBeOpen()
                spec()
            }
        }
    }
}
