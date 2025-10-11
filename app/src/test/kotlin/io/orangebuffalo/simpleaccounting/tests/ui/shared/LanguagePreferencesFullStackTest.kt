package io.orangebuffalo.simpleaccounting.tests.ui.shared

import com.microsoft.playwright.Page
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldHaveNotifications
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
                // Verify initial state
                language {
                    input.shouldHaveSelectedValue("English")
                }
                locale {
                    input.shouldHaveSelectedValue("Australian English")
                }
                
                // Change language
                language {
                    input.selectOptionWithoutValidation("Українська")
                }
            }
            
            shouldHaveNotifications {
                success()
            }
        }

        // Assert only language changed, everything else stayed the same
        val updatedUser = aggregateTemplate.findSingle<PlatformUser>(initialUser.id!!)
        updatedUser.shouldBeEqualToIgnoringFields(initialUser, PlatformUser::i18nSettings, PlatformUser::version)
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

        // Assert only locale changed, everything else stayed the same
        val updatedUser = aggregateTemplate.findSingle<PlatformUser>(initialUser.id!!)
        updatedUser.shouldBeEqualToIgnoringFields(initialUser, PlatformUser::i18nSettings, PlatformUser::version)
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
                language {
                    input.shouldHaveOptions("English", "Українська")
                }
            }
        }
    }

    @Test
    fun `should change language for admin user`(page: Page) {
        val initialUser = preconditions.farnsworth
        
        page.authenticateViaCookie(initialUser)
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
                    input.selectOptionWithoutValidation("Українська")
                }
            }
            
            shouldHaveNotifications {
                success()
            }
        }

        // Assert only language changed, everything else stayed the same
        val updatedUser = aggregateTemplate.findSingle<PlatformUser>(initialUser.id!!)
        updatedUser.shouldBeEqualToIgnoringFields(initialUser, PlatformUser::i18nSettings, PlatformUser::version)
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
                // Verify initial state
                language {
                    input.shouldHaveSelectedValue("English")
                }
                locale {
                    input.shouldHaveSelectedValue("Australian English")
                }
                
                // Change locale
                locale {
                    input.selectOption("British English")
                }
            }
            
            shouldHaveNotifications {
                success("Language preferences have been saved")
            }
        }

        // Assert only locale changed, everything else stayed the same
        val updatedUser = aggregateTemplate.findSingle<PlatformUser>(initialUser.id!!)
        updatedUser.shouldBeEqualToIgnoringFields(initialUser, PlatformUser::i18nSettings, PlatformUser::version)
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
                // Verify initial state
                sectionHeader.shouldBeVisible()
                ukrainianSectionHeader.shouldBeHidden()
                
                // Change language
                language {
                    input.selectOptionWithoutValidation("Українська")
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

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry().withWorkspace()
            val farnsworth = farnsworth()
        }
    }
}
