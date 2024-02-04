package io.orangebuffalo.simpleaccounting.web.ui.shared

import com.microsoft.playwright.Page
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.database.TestData
import io.orangebuffalo.simpleaccounting.services.persistence.repos.PlatformUserRepository
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
) {

    @Test
    fun `should change password for regular user`(page: Page, testData: PasswordChangeTestData) {
        whenever(passwordEncoder.encode("newPassword")) doReturn "newPasswordHash"

        page.loginAs(testData.fry)
        page.shouldHaveSideMenu().clickMyProfile()
        page.shouldBeMyProfilePage()
            .currentPassword {
                fill("currentPassword")
            }
            .newPassword {
                fill("newPassword")
            }
            .newPasswordConfirmation {
                fill("newPassword")
            }
            .changePasswordButton { click() }
            .shouldHaveNotifications {
                success("Password has been changed")
            }

        repository.findByUserName(testData.fry.userName)
            .shouldNotBeNull()
            .passwordHash.shouldBe("newPasswordHash")
    }

    @Test
    fun `should prevent submit if inputs not provided`(page: Page, testData: PasswordChangeTestData) {
        page.loginAs(testData.fry)
        page.shouldHaveSideMenu().clickMyProfile()
        page.shouldBeMyProfilePage()
            .changePasswordButton { shouldBeDisabled() }
            .currentPassword {
                fill("currentPassword")
            }
            .changePasswordButton { shouldBeDisabled() }
            .newPassword {
                fill("newPassword")
            }
            .changePasswordButton { shouldBeDisabled() }
            .newPasswordConfirmation {
                fill("newPassword")
            }
            .changePasswordButton { shouldBeEnabled() }
    }

    @Test
    fun `should validate that confirmation matches the new password`(page: Page, testData: PasswordChangeTestData) {
        page.loginAs(testData.fry)
        page.shouldHaveSideMenu().clickMyProfile()
        page.shouldBeMyProfilePage()
            .currentPassword {
                fill("currentPassword")
            }
            .newPassword {
                fill("newPassword")
            }
            .newPasswordConfirmation {
                fill("newPassword1")
            }
            .changePasswordButton { click() }
            .newPasswordConfirmation { formItem ->
                formItem.shouldHaveValidationError("New password confirmation does not match")
            }
    }

    @Test
    fun `should validate that current password matches`(page: Page, testData: PasswordChangeTestData) {
        whenever(passwordEncoder.matches("currentPassword", testData.fry.passwordHash)) doReturn false

        page.loginAs(testData.fry)
        page.shouldHaveSideMenu().clickMyProfile()
        page.shouldBeMyProfilePage()
            .currentPassword {
                fill("currentPassword")
            }
            .newPassword {
                fill("newPassword")
            }
            .newPasswordConfirmation {
                fill("newPassword")
            }
            .changePasswordButton { click() }
            .shouldHaveNotifications {
                validationFailed()
            }
            .currentPassword { formItem ->
                formItem.shouldHaveValidationError("Current password does not match")
            }

        withClue("Password should not be changed") {
            repository.findByUserName(testData.fry.userName)
                .shouldNotBeNull()
                .passwordHash.shouldBe(testData.fry.passwordHash)
        }
    }

    @Test
    fun `should change password for admin user`(page: Page, testData: PasswordChangeTestData) {
        whenever(passwordEncoder.encode("newPassword")) doReturn "newPasswordHash"

        page.loginAs(testData.farnsworth)
        page.shouldHaveSideMenu().clickMyProfile()
        page.shouldBeMyProfilePage()
            .currentPassword {
                fill("currentPassword")
            }
            .newPassword {
                fill("newPassword")
            }
            .newPasswordConfirmation {
                fill("newPassword")
            }
            .changePasswordButton { click() }
            .shouldHaveNotifications {
                success("Password has been changed")
            }

        repository.findByUserName(testData.farnsworth.userName)
            .shouldNotBeNull()
            .passwordHash.shouldBe("newPasswordHash")
    }

    class PasswordChangeTestData : TestData {
        val fry = Prototypes.fry()
        val workspace = Prototypes.workspace(owner = fry)
        val farnsworth = Prototypes.farnsworth()
    }
}
