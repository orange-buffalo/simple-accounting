package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.utils.assert
import io.orangebuffalo.simpleaccounting.infra.utils.assertThat

class Notifications(
    private val page: Page,
) {
    fun success(message: String) {
        val successContainer = page.locator(".sa-notification--success")
        successContainer.assert { isVisible() }
        successContainer.locator(".el-message__content").assert { hasText(message) }
        successContainer.locator(".el-message__closeBtn").click()
        successContainer.assert { isHidden() }
    }

    fun validationFailed() {
        val validationErrorContainer = page.locator(".sa-notification--warning")
        validationErrorContainer.assertThat().isVisible()
        validationErrorContainer.locator(".el-message__content").assertThat()
            .hasText("Validation failed. Please correct your input and try again")
        validationErrorContainer.locator(".el-message__closeBtn").click()
        validationErrorContainer.assertThat().isHidden()
    }
}
