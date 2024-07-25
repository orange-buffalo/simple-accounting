package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.tests.infra.utils.innerTextTrimmed
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy

/**
 * Supports multiple synced instances of paginator within a container
 * (e.g. as in SaPageableItems).
 */
class Paginator private constructor(
    private val locator: Locator,
    private val instancesCount: Int
) {
    private val instances: List<Locator>
        get() = locator.all().also { it.shouldHaveSize(instancesCount) }

    operator fun invoke(action: Paginator.() -> Unit) {
        this.action()
    }

    fun shouldHaveTotalPages(expected: Int) = shouldSatisfy {
        instances.forEach {
            it.locator(".el-pager > li")
                .all()
                .shouldHaveSize(expected)
        }
    }

    fun shouldHaveActivePage(expected: Int) = shouldSatisfy {
        instances.forEach {
            it.locator(".el-pager > li.is-active")
                .innerTextTrimmed()
                .shouldBe(expected.toString())
        }
    }

    fun next() {
        instances.first().locator(".btn-next").click()
    }

    fun previous() {
        instances.first().locator(".btn-prev").click()
    }

    companion object {
        fun <T : SaPageBase<T>> ComponentsAccessors<T>.twoSyncedPaginators(container: Locator) =
            Paginator(locator = container.locator(".el-pagination"), instancesCount = 2)
    }
}
