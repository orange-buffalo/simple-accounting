package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.infra.utils.assertThat

class Notifications(
    private val page: Page,
) {
    fun success(message: String) {
        val successContainer = page.locator(".sa-notification--success")
        successContainer.assertThat().isVisible()
        successContainer.locator(".el-message__content").assertThat().hasText(message)
        successContainer.locator(".el-message__closeBtn").click()
        successContainer.assertThat().isHidden()
    }

    fun validationFailed() {
        val successContainer = page.locator(".sa-notification--warning")
        successContainer.assertThat().isVisible()
        successContainer.locator(".el-message__content").assertThat()
            .hasText("Validation failed. Please correct your input and try again")
        successContainer.locator(".el-message__closeBtn").click()
        successContainer.assertThat().isHidden()
    }
}
