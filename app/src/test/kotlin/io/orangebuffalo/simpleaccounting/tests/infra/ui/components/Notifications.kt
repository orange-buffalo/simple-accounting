package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText

class Notifications(
    private val page: Page,
) {
    fun success(message: String? = null) {
        val successContainer = page.locator(".sa-notification--success")
        successContainer.shouldBeVisible()
        if (message != null) {
            successContainer.locator(".el-message__content").shouldHaveText(message)
        }
        successContainer.locator(".el-message__closeBtn").click()
        successContainer.shouldBeHidden()
    }

    fun validationFailed() {
        val validationErrorContainer = page.locator(".sa-notification--warning")
        validationErrorContainer.shouldBeVisible()
        validationErrorContainer.locator(".el-message__content").shouldHaveText(
            "Some of the fields have not been filled correctly. Please check the form and try again."
        )
        validationErrorContainer.locator(".el-message__closeBtn").click()
        validationErrorContainer.shouldBeHidden()
    }
}
