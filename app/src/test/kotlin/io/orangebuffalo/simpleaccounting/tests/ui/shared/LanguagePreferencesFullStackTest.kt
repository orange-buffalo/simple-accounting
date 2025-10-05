package io.orangebuffalo.simpleaccounting.tests.ui.shared

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.XPath
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldEventually
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
        
        // Open the language select dropdown
        val languageSelectWrapper = page.locator("//*[@class='el-form-item__label' and text()='Interface Language']/..//*[@class='el-select__wrapper']")
        languageSelectWrapper.shouldBeVisible()
        languageSelectWrapper.click()
        
        // Select Ukrainian option
        page.locator("//*[${XPath.hasClass("el-select-dropdown__item")}]/span[text()='Українська']").click()

        shouldEventually("Should update the language in the database") {
            aggregateTemplate.findSingle<PlatformUser>(preconditions.fry.id!!)
                .i18nSettings.language.shouldBe("uk")
        }
    }

    @Test
    fun `should change locale for regular user`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible()
        }
        
        // Open the locale select dropdown  
        val localeSelectWrapper = page.locator("//*[${XPath.hasClass("el-form-item__label")} and starts-with(text(), 'Language to display')]/..//*[${XPath.hasClass("el-select__wrapper")}]")
        localeSelectWrapper.shouldBeVisible()
        localeSelectWrapper.click()
        
        // Select Albanian option
        page.locator("//*[${XPath.hasClass("el-select-dropdown__item")}]/span[text()='Albanian']").click()

        shouldEventually("Should update the locale in the database") {
            aggregateTemplate.findSingle<PlatformUser>(preconditions.fry.id!!)
                .i18nSettings.locale.shouldBe("sq_AL")
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
        
        // Open the language select dropdown
        val languageSelectWrapper = page.locator("//*[@class='el-form-item__label' and text()='Interface Language']/..//*[@class='el-select__wrapper']")
        languageSelectWrapper.shouldBeVisible()
        languageSelectWrapper.click()
        
        // Select Ukrainian option
        page.locator("//*[${XPath.hasClass("el-select-dropdown__item")}]/span[text()='Українська']").click()

        shouldEventually("Should update the language in the database") {
            aggregateTemplate.findSingle<PlatformUser>(preconditions.farnsworth.id!!)
                .i18nSettings.language.shouldBe("uk")
        }
    }

    @Test
    fun `should change locale for admin user`(page: Page) {
        page.authenticateViaCookie(preconditions.farnsworth)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible()
        }
        
        // Open the locale select dropdown
        val localeSelectWrapper = page.locator("//*[@class='el-form-item__label' and starts-with(text(), 'Language to display')]/..//*[@class='el-select__wrapper']")
        localeSelectWrapper.shouldBeVisible()
        localeSelectWrapper.click()
        
        // Select British English option
        page.locator("//*[${XPath.hasClass("el-select-dropdown__item")}]/span[text()='British English']").click()

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
        
        // Open and select Ukrainian language
        val languageSelectWrapper = page.locator("//*[@class='el-form-item__label' and text()='Interface Language']/..//*[@class='el-select__wrapper']")
        languageSelectWrapper.click()
        page.locator("//*[${XPath.hasClass("el-select-dropdown__item")}]/span[text()='Українська']").click()
        
        withHint("Locale select should be updated to Ukrainian language") {
            // After language change, locale options should be displayed in Ukrainian
            // Open the locale dropdown to verify the options
            val localeSelectWrapper = page.locator("//*[@class='el-form-item__label' and starts-with(text(), 'Мова для відображення')]/..//*[@class='el-select__wrapper']")
            localeSelectWrapper.shouldBeVisible()
            localeSelectWrapper.click()
            
            // Verify at least one option is in Ukrainian (contains Cyrillic)
            val localeOptions = page.locator("//*[${XPath.hasClass("el-select-dropdown__item")}]").allInnerTexts()
            localeOptions.any { it.matches(Regex(".*[А-Яа-яІіЇїЄєҐґ].*")) }.shouldBe(true)
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry().withWorkspace()
            val farnsworth = farnsworth()
        }
    }
}
