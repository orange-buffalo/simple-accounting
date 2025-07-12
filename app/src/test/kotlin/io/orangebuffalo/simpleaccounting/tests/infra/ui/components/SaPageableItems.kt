package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Paginator.Companion.twoSyncedPaginators
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue

class SaPageableItems<I, P : Any> private constructor(
    private val container: Locator,
    parent: P,
    val paginator: Paginator,
    private val itemFactory: (container: Locator) -> I
) : UiComponent<P, SaPageableItems<I, P>>(parent) {

    private val itemsEls = container.locator(".sa-pageable-items__item")

    /**
     * Warning! Will provide the items as are during the call. User [shouldHaveExactItems] to ensure
     * the items are available in case of dynamic content.
     */
    val staticItems: List<I>
        get() = itemsEls.all().map { itemFactory(it) }

    /**
     * Waits for the items to be the provided values, which are mapped from the items.
     */
    fun <T> shouldHaveExactItems(vararg items: T, mapper: (item: I) -> T) {
        assertThat(itemsEls).hasCount(items.size)
        staticItems
            .map(mapper)
            .shouldContainExactly(*items)
    }

    /**
     * Ensures that the item satisfying the predicate is present, and returns it.
     */
    fun shouldHaveItemSatisfying(itemPredicate: (item: I) -> Boolean): I {
        var item: I? = null
        container.shouldSatisfy {
            staticItems
                .filter { itemPredicate(it) }
                .shouldWithClue("Exactly one item expected to satisfy the predicate") {
                    item = shouldBeSingle()
                }
        }
        return item!!
    }

    /**
     * Waits for the items to contain the provided values, which are mapped from the items.
     * Only checks that all the provided items are included, but allows extra items.
     */
    fun <T> shouldContainItems(vararg items: T, mapper: (item: I) -> T) {
        container.shouldSatisfy {
            itemsEls.all().shouldHaveAtLeastSize(items.size)
            staticItems
                .map(mapper)
                .shouldContainAll(*items)
        }
    }

    /**
     * Loader is debounced and requires a tick to finish loading.
     */
    fun finishLoadingWhenTimeMocked() {
        container.page().clock().runFor(1)
    }

    companion object {
        fun <T : SaPageBase<T>, I> ComponentsAccessors<T>.pageableItems(
            itemFactory: (container: Locator) -> I
        ): SaPageableItems<I, T> {
            val container = page.locator(".sa-pageable-items")
            return SaPageableItems(
                container = container,
                parent = this.owner,
                paginator = this.twoSyncedPaginators(container),
                itemFactory = itemFactory
            )
        }
    }
}
