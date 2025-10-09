package io.orangebuffalo.simpleaccounting.tests.ui.shared

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldEventually
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
        page.authenticateViaCookie(preconditions.fry)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible()
        }
        
        // Click on language select to open dropdown
        page.locator("//*[${XPath.hasClass("el-form-item__label")} and text()='Interface Language']/..//*[${XPath.hasClass("el-select__wrapper")}]").click()
        // Select Ukrainian option
        page.locator("//*[${XPath.hasClass("el-select-dropdown__item")}]/span[text()='Українська']").click()
        
        // After changing to Ukrainian, the notification appears in Ukrainian
        page.shouldHaveNotifications {
            success()
        }

        shouldEventually("Should update the language in the database") {
            aggregateTemplate.findSingle<PlatformUser>(preconditions.fry.id!!)
                .i18nSettings.language.shouldBe("uk")
        }
    }

    @Test
    fun `should change locale for regular user`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible {
                localeFormItem.shouldBeVisible()
            }
        }
        
        // Click on locale select to open dropdown using form item
        page.locator(".el-form-item").filter(com.microsoft.playwright.Locator.FilterOptions().setHasText("Language to display dates, amounts, etc"))
            .locator(".el-select__wrapper").click()
        // Select Albanian option
        page.locator("//*[${XPath.hasClass("el-select-dropdown__item")}]/span[text()='Albanian']").click()
        
        page.shouldHaveNotifications {
            success()
        }

        shouldEventually("Should update the locale in the database") {
            aggregateTemplate.findSingle<PlatformUser>(preconditions.fry.id!!)
                .i18nSettings.locale.shouldBe("sq")
        }
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
        page.authenticateViaCookie(preconditions.farnsworth)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible()
        }
        
        // Click on language select to open dropdown
        page.locator("//*[${XPath.hasClass("el-form-item__label")} and text()='Interface Language']/..//*[${XPath.hasClass("el-select__wrapper")}]").click()
        // Select Ukrainian option
        page.locator("//*[${XPath.hasClass("el-select-dropdown__item")}]/span[text()='Українська']").click()
        
        // After changing to Ukrainian, the notification appears in Ukrainian
        page.shouldHaveNotifications {
            success()
        }

        shouldEventually("Should update the language in the database") {
            aggregateTemplate.findSingle<PlatformUser>(preconditions.farnsworth.id!!)
                .i18nSettings.language.shouldBe("uk")
        }
    }

    @Test
    fun `should change locale for admin user`(page: Page) {
        page.authenticateViaCookie(preconditions.farnsworth)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible {
                localeFormItem.shouldBeVisible()
            }
        }
        
        // Click on locale select to open dropdown using form item
        page.locator(".el-form-item").filter(com.microsoft.playwright.Locator.FilterOptions().setHasText("Language to display dates, amounts, etc"))
            .locator(".el-select__wrapper").click()
        // Select British English option
        page.locator("//*[${XPath.hasClass("el-select-dropdown__item")}]/span[text()='British English']").click()
        
        page.shouldHaveNotifications {
            success()
        }

        shouldEventually("Should update the locale in the database") {
            aggregateTemplate.findSingle<PlatformUser>(preconditions.farnsworth.id!!)
                .i18nSettings.locale.shouldBe("en_GB")
        }
    }

    @Test
    fun `should update UI language immediately after language change`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible()
        }
        
        // Click on language select to open dropdown and select Ukrainian
        page.locator("//*[${XPath.hasClass("el-form-item__label")} and text()='Interface Language']/..//*[${XPath.hasClass("el-select__wrapper")}]").click()
        page.locator("//*[${XPath.hasClass("el-select-dropdown__item")}]/span[text()='Українська']").click()
        
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
