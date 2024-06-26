package io.orangebuffalo.simpleaccounting.web.ui.shared

import com.microsoft.playwright.Page
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.domain.users.PlatformUserRepository
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.Preconditions
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsInfra
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
    @Autowired private val preconditionsInfra: PreconditionsInfra,
) {

    @Test
    fun `should change password for regular user`(page: Page) {
        val testData = setupPreconditions()

        whenever(passwordEncoder.encode("newPassword")) doReturn "newPasswordHash"

        page.loginAs(testData.fry)
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

        repository.findByUserName(testData.fry.userName)
            .shouldNotBeNull()
            .passwordHash.shouldBe("newPasswordHash")
    }

    @Test
    fun `should prevent submit if inputs not provided`(page: Page) {
        val testData = setupPreconditions()

        page.loginAs(testData.fry)
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
        val testData = setupPreconditions()

        page.loginAs(testData.fry)
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
        val testData = setupPreconditions()

        whenever(passwordEncoder.matches("currentPassword", testData.fry.passwordHash)) doReturn false

        page.loginAs(testData.fry)
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
            repository.findByUserName(testData.fry.userName)
                .shouldNotBeNull()
                .passwordHash.shouldBe(testData.fry.passwordHash)
        }
    }

    @Test
    fun `should change password for admin user`(page: Page) {
        val testData = setupPreconditions()

        whenever(passwordEncoder.encode("newPassword")) doReturn "newPasswordHash"

        page.loginAs(testData.farnsworth)
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

        repository.findByUserName(testData.farnsworth.userName)
            .shouldNotBeNull()
            .passwordHash.shouldBe("newPasswordHash")
    }

    private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
        val fry = fry()

        // TODO #23: workspace should not be required?
        val workspace = workspace(owner = fry)
        val farnsworth = farnsworth()
    }
}
