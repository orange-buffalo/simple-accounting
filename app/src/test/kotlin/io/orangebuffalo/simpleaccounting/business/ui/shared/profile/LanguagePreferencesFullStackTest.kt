package io.orangebuffalo.simpleaccounting.business.ui.shared.profile

import com.microsoft.playwright.Page
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.Companion.openMyProfilePage
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import org.junit.jupiter.api.Test

/**
 * Tests language and locale preferences on My Profile page for both regular users and admins.
 * See also:
 * - [io.orangebuffalo.simpleaccounting.business.ui.user.profile.UserProfileFullStackTest] for basic My Profile page rendering
 * - [PasswordChangeFullStackTest] for password change functionality
 * - [io.orangebuffalo.simpleaccounting.business.ui.user.profile.UserProfileGoogleDriveDocumentStorageFullStackTest] for Google Drive storage integration
 */
class LanguagePreferencesFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should change language for regular user`(page: Page) {
        testLanguageChange(page, preconditions.fry)
    }

    @Test
    fun `should change language for admin user`(page: Page) {
        testLanguageChange(page, preconditions.farnsworth)
    }

    @Test
    fun `should change locale for regular user`(page: Page) {
        testLocaleChange(page, preconditions.fry)
    }

    @Test
    fun `should change locale for admin user`(page: Page) {
        testLocaleChange(page, preconditions.farnsworth)
    }

    @Test
    fun `should have available languages`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible {
                language {
                    input.shouldHaveOptions("English", "Українська")
                }
            }
        }
    }

    @Test
    fun `should update UI language immediately after language change`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible {
                // Verify initial state
                sectionHeader.shouldBeVisible()
                ukrainianSectionHeader.shouldBeHidden()

                // Change language
                language {
                    input.selectOption("Українська", validate = false)
                }
            }

            shouldHaveNotifications {
                success()
            }

            // After language change, verify the UI is in Ukrainian by checking section headers
            // Note: Can't use shouldHaveLanguagePreferencesSectionVisible because it checks for English header
            languagePreferencesSection {
                sectionHeader.shouldBeHidden()
                ukrainianSectionHeader.shouldBeVisible()
            }
        }
    }

    private fun testLanguageChange(page: Page, user: PlatformUser) {
        page.authenticateViaCookie(user)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible {
                // Verify initial state
                language {
                    input.shouldHaveSelectedValue("English")
                }
                locale {
                    input.shouldHaveSelectedValue("Australian English")
                }

                // Change language
                language {
                    input.selectOption("Українська", validate = false)
                }
            }

            shouldHaveNotifications {
                // Not verifying notification text because UI language changes immediately,
                // creating a race condition where notification could show in either language
                success()
            }
        }

        assertOnlyI18nFieldChanged(
            user, I18nSettings(
                language = "uk",
                locale = user.i18nSettings.locale
            )
        )
    }

    private fun testLocaleChange(page: Page, user: PlatformUser) {
        page.authenticateViaCookie(user)
        page.openMyProfilePage {
            shouldHaveLanguagePreferencesSectionVisible {
                // Verify initial state
                language {
                    input.shouldHaveSelectedValue("English")
                }
                locale {
                    input.shouldHaveSelectedValue("Australian English")
                }

                // Change locale
                locale {
                    input.selectOption("Albanian")
                }
            }

            shouldHaveNotifications {
                success("Language preferences have been saved")
            }
        }

        assertOnlyI18nFieldChanged(
            user, I18nSettings(
                language = user.i18nSettings.language,
                locale = "sq"
            )
        )
    }

    private fun assertOnlyI18nFieldChanged(initialUser: PlatformUser, expectedI18nSettings: I18nSettings) {
        val updatedUser = aggregateTemplate.findSingle<PlatformUser>(initialUser.id!!)
        updatedUser.shouldBeEqualToIgnoringFields(initialUser, PlatformUser::i18nSettings, PlatformUser::version)
        updatedUser.i18nSettings.shouldBe(expectedI18nSettings)
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry().withWorkspace()
            val farnsworth = farnsworth()
        }
    }
}
