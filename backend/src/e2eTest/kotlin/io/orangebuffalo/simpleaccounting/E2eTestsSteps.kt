package io.orangebuffalo.simpleaccounting

import com.codeborne.selenide.Selectors.*
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.Selenide.open

fun loginWithFry() {
    open("/")
    element(by("placeholder", "Login")).value = "Fry"
    element(by("placeholder", "Password")).value = "password"
    element(byText("Login")).click()
}

fun sideMenuItem(text: String) =
    element(byXpath("//*[contains(@class, 'the-side-menu__link') and contains(text(), '$text')]"))

fun overviewItemByTitle(title: String) =
    element(byXpath("//*[contains(@class, 'overview-item__title') and contains(text(), '$title')]"))
