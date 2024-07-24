package io.orangebuffalo.simpleaccounting.web.ui.shared

import com.microsoft.playwright.Page
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.users.PlatformUserRepository
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.infra.utils.shouldHaveNotifications
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.shouldBeMyProfilePage
import io.orangebuffalo.simpleaccounting.web.ui.shared.pages.shouldHaveSideMenu
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder

@SimpleAccountingFullStackTest
class PasswordChangeFullStackTest(
    @Autowired private val repository: PlatformUserRepository,
    @Autowired private val passwordEncoder: PasswordEncoder,
    preconditionsFactory: PreconditionsFactory,
) {

    @Test
    fun `should change password for regular user`(page: Page) {
        whenever(passwordEncoder.encode("newPassword")) doReturn "newPasswordHash"

        page.loginAs(preconditions.fry)
        page.shouldHaveSideMenu().clickMyProfile()
        page.shouldBeMyProfilePage().shouldHavePasswordChangeSectionVisible {
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

        repository.findByUserName(preconditions.fry.userName)
            .shouldNotBeNull()
            .passwordHash.shouldBe("newPasswordHash")
    }

    @Test
    fun `should prevent submit if inputs not provided`(page: Page) {
        page.loginAs(preconditions.fry)
        page.shouldHaveSideMenu().clickMyProfile()
        page.shouldBeMyProfilePage().shouldHavePasswordChangeSectionVisible {
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

    @Test
    fun `should validate that confirmation matches the new password`(page: Page) {
        page.loginAs(preconditions.fry)
        page.shouldHaveSideMenu().clickMyProfile()
        page.shouldBeMyProfilePage().shouldHavePasswordChangeSectionVisible {
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
            newPasswordConfirmation {
                shouldHaveValidationError("New password confirmation does not match")
            }
        }
    }

    @Test
    fun `should validate that current password matches`(page: Page) {
        whenever(passwordEncoder.matches("currentPassword", preconditions.fry.passwordHash)) doReturn false

        page.loginAs(preconditions.fry)
        page.shouldHaveSideMenu().clickMyProfile()
        page.shouldBeMyProfilePage().shouldHavePasswordChangeSectionVisible {
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
            currentPassword {
                shouldHaveValidationError("Current password does not match")
            }
        }

        withClue("Password should not be changed") {
            repository.findByUserName(preconditions.fry.userName)
                .shouldNotBeNull()
                .passwordHash.shouldBe(preconditions.fry.passwordHash)
        }
    }

    @Test
    fun `should change password for admin user`(page: Page) {
        whenever(passwordEncoder.encode("newPassword")) doReturn "newPasswordHash"

        page.loginAs(preconditions.farnsworth)
        page.shouldHaveSideMenu().clickMyProfile()
        page.shouldBeMyProfilePage().shouldHavePasswordChangeSectionVisible {
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

        repository.findByUserName(preconditions.farnsworth.userName)
            .shouldNotBeNull()
            .passwordHash.shouldBe("newPasswordHash")
    }

    private val preconditions by preconditionsFactory {
        object {
            val fry = fry()

            // TODO #23: workspace should not be required?
            val workspace = workspace(owner = fry)
            val farnsworth = farnsworth()
        }
    }
}
