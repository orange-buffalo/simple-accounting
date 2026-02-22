package io.orangebuffalo.simpleaccounting.business.ui.shared.profile

import com.microsoft.playwright.Page
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.Companion.openMyProfilePage
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersRepository
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldHaveNotifications
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired

/**
 * Tests password change functionality on My Profile page for both regular users and admins.
 * See also:
 * - [io.orangebuffalo.simpleaccounting.business.ui.user.profile.UserProfileFullStackTest] for basic My Profile page rendering
 * - [io.orangebuffalo.simpleaccounting.business.ui.user.profile.UserProfileGoogleDriveDocumentStorageFullStackTest] for Google Drive storage integration
 * - [LanguagePreferencesFullStackTest] for language and locale preferences
 */
class PasswordChangeFullStackTest(
    @param:Autowired private val repository: PlatformUsersRepository,
) : SaFullStackTestBase() {

    @Test
    fun `should change password for regular user`(page: Page) {
        whenever(passwordEncoder.encode("newPassword")) doReturn "newPasswordHash"

        page.authenticateViaCookie(preconditions.fry)
        page.openMyProfilePage {
            shouldHavePasswordChangeSectionVisible {
                currentPassword {
                    input { fill("currentPassword") }
                }
                newPassword {
                    input { fill("newPassword") }
                }
                newPasswordConfirmation {
                    input { fill("newPassword") }
                }
                changePasswordButton { click() }

                page.shouldHaveNotifications {
                    success("Password has been changed")
                }
            }
        }

        repository.findByUserName(preconditions.fry.userName)
            .shouldNotBeNull()
            .passwordHash.shouldBe("newPasswordHash")
    }

    @Test
    fun `should prevent submit if inputs not provided`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openMyProfilePage {
            shouldHavePasswordChangeSectionVisible {
                changePasswordButton { shouldBeDisabled() }
                currentPassword {
                    input { fill("currentPassword") }
                }
                changePasswordButton { shouldBeDisabled() }
                newPassword {
                    input { fill("newPassword") }
                }
                changePasswordButton { shouldBeDisabled() }
                newPasswordConfirmation {
                    input { fill("newPassword") }
                }
                changePasswordButton { shouldBeEnabled() }
            }
        }
    }

    @Test
    fun `should validate that confirmation matches the new password`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)
        page.openMyProfilePage {
            shouldHavePasswordChangeSectionVisible {
                currentPassword {
                    input { fill("currentPassword") }
                }
                newPassword {
                    input { fill("newPassword") }
                }
                newPasswordConfirmation {
                    input { fill("newPassword1") }
                }
                changePasswordButton { click() }
                page.shouldHaveNotifications {
                    validationFailed()
                }
                newPasswordConfirmation {
                    shouldHaveValidationError("New password confirmation does not match")
                }
            }
        }
    }

    @Test
    fun `should validate that current password matches`(page: Page) {
        whenever(passwordEncoder.matches("currentPassword", preconditions.fry.passwordHash)) doReturn false

        page.authenticateViaCookie(preconditions.fry)
        page.openMyProfilePage {
            shouldHavePasswordChangeSectionVisible {
                currentPassword {
                    input { fill("currentPassword") }
                }
                newPassword {
                    input { fill("newPassword") }
                }
                newPasswordConfirmation {
                    input { fill("newPassword") }
                }
                changePasswordButton { click() }
                page.shouldHaveNotifications {
                    validationFailed()
                }
                currentPassword {
                    shouldHaveValidationError("Current password does not match")
                }
            }
        }

        withHint("Password should not be changed") {
            repository.findByUserName(preconditions.fry.userName)
                .shouldNotBeNull()
                .passwordHash.shouldBe(preconditions.fry.passwordHash)
        }
    }

    @Test
    fun `should change password for admin user`(page: Page) {
        whenever(passwordEncoder.encode("newPassword")) doReturn "newPasswordHash"

        page.authenticateViaCookie(preconditions.farnsworth)
        page.openMyProfilePage {
            shouldHavePasswordChangeSectionVisible {
                currentPassword {
                    input { fill("currentPassword") }
                }
                newPassword {
                    input { fill("newPassword") }
                }
                newPasswordConfirmation {
                    input { fill("newPassword") }
                }
                changePasswordButton { click() }
                page.shouldHaveNotifications {
                    success("Password has been changed")
                }
            }
        }

        repository.findByUserName(preconditions.farnsworth.userName)
            .shouldNotBeNull()
            .passwordHash.shouldBe("newPasswordHash")
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry().withWorkspace()
            val farnsworth = farnsworth()
        }
    }
}
