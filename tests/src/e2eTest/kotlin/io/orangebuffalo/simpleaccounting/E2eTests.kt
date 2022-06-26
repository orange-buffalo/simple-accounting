package io.orangebuffalo.simpleaccounting

import com.codeborne.selenide.Condition.appear
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(E2eTestsEnvironment::class)
// TODO enable back
@Disabled("Disabling temporary until frontend code is migrated")
class E2eTests {

    @Test
    fun `should login successfully`() {
        loginWithFry()
        sideMenuItem("Dashboard").should(appear)
    }

    @Test
    fun `should load invoices`() {
        loginWithFry()
        sideMenuItem("Invoice").click()
        overviewItemByTitle("Stuffed Toys").should(appear)
    }
}

