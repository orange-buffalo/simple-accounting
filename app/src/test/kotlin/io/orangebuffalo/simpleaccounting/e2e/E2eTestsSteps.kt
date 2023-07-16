package io.orangebuffalo.simpleaccounting.e2e

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page

fun Page.loginWithFry() {
    navigate("/")
    getByPlaceholder("Login").type("Fry")
    getByPlaceholder("Password").type("password")
    getByText("Login").click()
}

fun Page.sideMenuItem(text: String): Locator =
    locator("//*[contains(@class, 'side-menu__link') and contains(text(), '$text')]")

fun Page.overviewItemByTitle(title: String): Locator =
    locator("//*[contains(@class, 'overview-item__title') and contains(text(), '$title')]")
