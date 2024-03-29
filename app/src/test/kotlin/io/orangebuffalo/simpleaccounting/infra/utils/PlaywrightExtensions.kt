package io.orangebuffalo.simpleaccounting.infra.utils

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.LocatorAssertions
import com.microsoft.playwright.assertions.PlaywrightAssertions

fun Locator.assertThat(): LocatorAssertions = PlaywrightAssertions.assertThat(this)

object XPath {
    fun hasClass(className: String): String = "contains(concat(' ', normalize-space(@class), ' '), ' $className ')"

    fun h1WithText(text: String): String = "//h1[normalize-space(.) = '$text']"
}

fun Page.openSimpleAccounting(): Page {
    navigate("/")
    addStyleTag(
        // disable animations to speedup the tests
        Page.AddStyleTagOptions()
            .setContent(
                """*, *::before, *::after {
                  transition-duration: 0s !important;
                  transition-delay: 0s !important;
                  animation-duration: 0s !important;
                  animation-delay: 0s !important;
                }"""
            )
    )
    return this
}
