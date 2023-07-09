package io.orangebuffalo.simpleaccounting.e2e

import com.microsoft.playwright.Page
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
        page.sideMenuItem("Dashboard").assertThat().isVisible()
    }

    @Test
    fun `should load invoices`(page: Page) {
        page.loginWithFry()
        page.sideMenuItem("Invoice").click()
        page.overviewItemByTitle("Stuffed Toys").assertThat().isVisible()
    }
}

