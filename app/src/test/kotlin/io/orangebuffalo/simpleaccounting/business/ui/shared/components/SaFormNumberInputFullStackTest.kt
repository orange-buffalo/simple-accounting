package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes.CreateGeneralTaxPage.Companion.openCreateGeneralTaxPage
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SaFormNumberInputFullStackTest : SaFullStackTestBase() {

    @ParameterizedTest
    @ValueSource(strings = ["20.251", "2e1"])
    fun `should reject invalid percentage syntax without changing the entered text`(value: String, page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().withWorkspace()
            }
        }
        page.authenticateViaCookie(testData.fry)
        page.openCreateGeneralTaxPage {
            title { input.fill("Space VAT") }
            rate {
                input.fill("20")
                input.fill(value)
            }
            saveButton.click()

            shouldHaveNotifications { validationFailed() }
            rate {
                shouldHaveValidationError("Enter a percentage from 0 to 100 with up to two decimal places")
                input.shouldHaveValue(value)
            }
        }
    }
}
