package io.orangebuffalo.simpleaccounting.infra.utils

import com.microsoft.playwright.Locator
import com.microsoft.playwright.assertions.LocatorAssertions
import com.microsoft.playwright.assertions.PlaywrightAssertions

fun Locator.assertThat(): LocatorAssertions = PlaywrightAssertions.assertThat(this)