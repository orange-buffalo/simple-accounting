package io.orangebuffalo.simpleaccounting.tests.ui.shared

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldHaveNotifications
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.MyProfilePage.Companion.openMyProfilePage
import org.junit.jupiter.api.Test

/**
 * Tests language and locale preferences on My Profile page for both regular users and admins.
 * See also:
 * - [io.orangebuffalo.simpleaccounting.tests.ui.user.UserProfileFullStackTest] for basic My Profile page rendering
 * - [PasswordChangeFullStackTest] for password change functionality
 * - [io.orangebuffalo.simpleaccounting.tests.ui.user.UserProfileGoogleDriveDocumentStorageFullStackTest] for Google Drive storage integration
 */
class LanguagePreferencesFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should change language for regular user`(page: Page) {
        val initialUser = preconditions.fry
        
        page.authenticateViaCookie(initialUser)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible {
                selectLanguage("Українська")
            }
        }
        
        page.shouldHaveNotifications {
            success()
        }

        // Assert only language changed, everything else stayed the same
        val updatedUser = aggregateTemplate.findSingle<PlatformUser>(initialUser.id!!)
        updatedUser.userName.shouldBe(initialUser.userName)
        updatedUser.passwordHash.shouldBe(initialUser.passwordHash)
        updatedUser.isAdmin.shouldBe(initialUser.isAdmin)
        updatedUser.activated.shouldBe(initialUser.activated)
        updatedUser.documentsStorage.shouldBe(initialUser.documentsStorage)
        updatedUser.i18nSettings.shouldBe(I18nSettings(
            language = "uk",
            locale = initialUser.i18nSettings.locale
        ))
    }

    @Test
    fun `should change locale for regular user`(page: Page) {
        val initialUser = preconditions.fry
        
        page.authenticateViaCookie(initialUser)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible {
                selectLocale("Albanian")
            }
        }
        
        page.shouldHaveNotifications {
            success()
        }

        // Assert only locale changed, everything else stayed the same
        val updatedUser = aggregateTemplate.findSingle<PlatformUser>(initialUser.id!!)
        updatedUser.userName.shouldBe(initialUser.userName)
        updatedUser.passwordHash.shouldBe(initialUser.passwordHash)
        updatedUser.isAdmin.shouldBe(initialUser.isAdmin)
        updatedUser.activated.shouldBe(initialUser.activated)
        updatedUser.documentsStorage.shouldBe(initialUser.documentsStorage)
        updatedUser.i18nSettings.shouldBe(I18nSettings(
            language = initialUser.i18nSettings.language,
            locale = "sq"
        ))
    }

    @Test
    fun `should have available languages`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible {
                languageFormItem.input.shouldHaveOptions("English", "Українська")
            }
        }
    }

    @Test
    fun `should change language for admin user`(page: Page) {
        val initialUser = preconditions.farnsworth
        
        page.authenticateViaCookie(initialUser)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible {
                selectLanguage("Українська")
            }
        }
        
        page.shouldHaveNotifications {
            success()
        }

        // Assert only language changed, everything else stayed the same
        val updatedUser = aggregateTemplate.findSingle<PlatformUser>(initialUser.id!!)
        updatedUser.userName.shouldBe(initialUser.userName)
        updatedUser.passwordHash.shouldBe(initialUser.passwordHash)
        updatedUser.isAdmin.shouldBe(initialUser.isAdmin)
        updatedUser.activated.shouldBe(initialUser.activated)
        updatedUser.documentsStorage.shouldBe(initialUser.documentsStorage)
        updatedUser.i18nSettings.shouldBe(I18nSettings(
            language = "uk",
            locale = initialUser.i18nSettings.locale
        ))
    }

    @Test
    fun `should change locale for admin user`(page: Page) {
        val initialUser = preconditions.farnsworth
        
        page.authenticateViaCookie(initialUser)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible {
                selectLocale("British English")
            }
        }
        
        page.shouldHaveNotifications {
            success()
        }

        // Assert only locale changed, everything else stayed the same
        val updatedUser = aggregateTemplate.findSingle<PlatformUser>(initialUser.id!!)
        updatedUser.userName.shouldBe(initialUser.userName)
        updatedUser.passwordHash.shouldBe(initialUser.passwordHash)
        updatedUser.isAdmin.shouldBe(initialUser.isAdmin)
        updatedUser.activated.shouldBe(initialUser.activated)
        updatedUser.documentsStorage.shouldBe(initialUser.documentsStorage)
        updatedUser.i18nSettings.shouldBe(I18nSettings(
            language = initialUser.i18nSettings.language,
            locale = "en_GB"
        ))
    }

    @Test
    fun `should update UI language immediately after language change`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible {
                selectLanguage("Українська")
            }
        }
        
        page.shouldHaveNotifications {
            success()
        }
        
        // After language change, the UI should be in Ukrainian
        withHint("UI should be updated to Ukrainian language") {
            // The language preferences section header should be in Ukrainian  
            page.locator("//*[contains(@class, 'el-form')]//h2[text()='Мовні Уподобання']").shouldBeVisible()
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry().withWorkspace()
            val farnsworth = farnsworth()
        }
    }
}
