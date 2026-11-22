package io.orangebuffalo.simpleaccounting.business.ui.shared.components

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.generaltaxes.GeneralTax
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes.CreateGeneralTaxPage.Companion.openCreateGeneralTaxPage
import io.orangebuffalo.simpleaccounting.business.ui.user.generaltaxes.GeneralTaxesOverviewPage.Companion.shouldBeGeneralTaxesOverviewPage
import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class SaFormNumberInputFullStackTest : SaFullStackTestBase() {

    @ParameterizedTest
    @ValueSource(strings = ["20.251", "2e1", "0,001", "0,100"])
    fun `should reject invalid percentage syntax without changing the entered text`(value: String, page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", i18nSettings = I18nSettings(locale = "en_GB", language = "en"))
                    .withWorkspace()
            }
        }
        page.authenticateViaCookie(testData.fry)
        page.openCreateGeneralTaxPage {
            title { input.fill("Space VAT") }
            rate { input.fill(value) }
            saveButton.click()

            shouldHaveNotifications { validationFailed() }
            rate {
                shouldHaveValidationError("Enter a number with at most 2 decimal places")
                input.shouldHaveValue(value)
            }
            if (value == "20.251") {
                reportRendering("number-input.validation-error-en_GB")
            }
        }
    }

    @ParameterizedTest
    @CsvSource("0, 0", "100, 10000")
    fun `should save inclusive percentage endpoints`(value: String, rateInBps: Int, page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().withWorkspace()
            }
        }
        page.authenticateViaCookie(testData.fry)
        page.openCreateGeneralTaxPage {
            title { input.fill("Space VAT") }
            rate { input.fill(value) }
            saveButton.click()
        }
        page.shouldBeGeneralTaxesOverviewPage()
        aggregateTemplate.findSingle<GeneralTax>().rateInBps.shouldBe(rateInBps)
    }

    @Test
    fun `should show decimal validation in Ukrainian`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", i18nSettings = I18nSettings(locale = "uk_UA", language = "uk"))
                    .withWorkspace()
            }
        }
        page.authenticateViaCookie(testData.fry)
        page.openCreateGeneralTaxPage(ukrainian = true) {
            title { input.fill("Space VAT") }
            rate { input.fill("20,251") }
            saveButton.click()

            shouldHaveNotifications { warning() }
            rate {
                shouldHaveValidationError("Вкажіть число: не більше 2 знаків після коми")
                input.shouldHaveValue("20,251")
            }
            reportRendering("number-input.validation-error-uk_UA")
        }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "en_GB|1234.56|1,234.56|20.29",
            "uk_UA|1234,56|1\u00A0234,56|20,29",
        ],
        delimiter = '|',
    )
    fun `should display profile locale separators and save exact basis points`(
        locale: String,
        ungrouped: String,
        grouped: String,
        allowedRate: String,
        page: Page,
    ) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", i18nSettings = I18nSettings(locale = locale, language = "en"))
                    .withWorkspace()
            }
        }
        page.authenticateViaCookie(testData.fry)
        page.openCreateGeneralTaxPage {
            rate { input.fill(ungrouped) }
            title { input.fill("Space VAT") }
            rate { input.shouldHaveValue(grouped) }
            reportRendering("number-input.grouping-$locale")

            saveButton.click()
            shouldHaveNotifications { validationFailed() }
            rate { shouldHaveValidationError("The value must be no greater than 100") }

            rate { input.fill(allowedRate) }
            saveButton.click()
        }
        page.shouldBeGeneralTaxesOverviewPage()
        aggregateTemplate.findSingle<GeneralTax>().rateInBps.shouldBe(2029)
    }
}
