package io.orangebuffalo.simpleaccounting.tests.ui.shared

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
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
            shouldHaveLanguagePreferencesSectionVisible {
                languageFormItem.input.shouldHaveSelectedValue("English")
                languageFormItem.input.selectOption("Українська")
                
                withHint("Should update selected value immediately") {
                    languageFormItem.input.shouldHaveSelectedValue("Українська")
                }
            }
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
                localeFormItem.input.shouldHaveSelectedValue("Australian English")
                localeFormItem.input.selectOption("Albanian")
                
                withHint("Should update selected value immediately") {
                    localeFormItem.input.shouldHaveSelectedValue("Albanian")
                }
            }
        }

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
            shouldHaveLanguagePreferencesSectionVisible {
                languageFormItem.input.shouldHaveSelectedValue("English")
                languageFormItem.input.selectOption("Українська")
                
                withHint("Should update selected value immediately") {
                    languageFormItem.input.shouldHaveSelectedValue("Українська")
                }
            }
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
                localeFormItem.input.shouldHaveSelectedValue("Australian English")
                localeFormItem.input.selectOption("British English")
                
                withHint("Should update selected value immediately") {
                    localeFormItem.input.shouldHaveSelectedValue("British English")
                }
            }
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
            shouldHaveLanguagePreferencesSectionVisible {
                languageFormItem.input.selectOption("Українська")
                
                withHint("Locale select should be updated to Ukrainian language") {
                    // After language change, locale options should be displayed in Ukrainian
                    // The currently selected locale (Australian English) should now show as "Австралійська англійська"
                    localeFormItem.input.shouldHaveSelectedValue("Австралійська англійська")
                }
            }
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry().withWorkspace()
            val farnsworth = farnsworth()
        }
    }
}
