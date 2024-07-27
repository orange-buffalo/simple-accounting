package io.orangebuffalo.simpleaccounting.tests.e2e

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeVisible
import io.orangebuffalo.testcontainers.playwright.junit.PlaywrightConfig
import io.orangebuffalo.testcontainers.playwright.junit.PlaywrightExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(E2eTestsExtension::class)
@ExtendWith(PlaywrightExtension::class)
@PlaywrightConfig(configurer = E2eTestsPlaywrightConfigurer::class)
class E2eTests {

    @Test
    fun `should login successfully`(page: Page) {
        page.loginWithFry()
        page.sideMenuItem("Dashboard").shouldBeVisible()
    }

    @Test
    fun `should load invoices`(page: Page) {
        page.loginWithFry()
        page.sideMenuItem("Invoice").click()
        page.overviewItemByTitle("Stuffed Toys").shouldBeVisible()
    }
}

private fun Page.loginWithFry() {
    navigate("/")
    getByPlaceholder("Login").fill("Fry")
    getByPlaceholder("Password").fill("password")
    getByText("Login").click()
}

private fun Page.sideMenuItem(text: String): Locator =
    locator("//*[contains(@class, 'side-menu__link') and contains(text(), '$text')]")

private fun Page.overviewItemByTitle(title: String): Locator =
    locator("//*[contains(@class, 'overview-item__title') and contains(text(), '$title')]")
