package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText

class Notifications(
    private val page: Page,
) {
    fun success(message: String? = null) {
        assertNotification(typeClassSuffix = "success", message = message)
    }

    fun validationFailed() {
        warning("Some of the fields have not been filled correctly. Please check the form and try again.")
    }

    fun warning(message: String? = null) {
        assertNotification(typeClassSuffix = "warning", message = message)
    }

    fun error(message: String? = null) {
        assertNotification(typeClassSuffix = "error", message = message)
    }

    fun shouldHaveNoNotifications() {
        val allNotifications = page.locator(".sa-notification")
        allNotifications.shouldBeHidden()
    }

    private fun assertNotification(
        typeClassSuffix: String,
        message: String?,
    ) {
        val container = page.locator(".sa-notification--$typeClassSuffix")
        container.shouldBeVisible()
        if (message != null) {
            container.locator(".el-message__content").shouldHaveText(message)
        }
        container.locator(".el-message__closeBtn").click()
        container.shouldBeHidden()
    }
}
